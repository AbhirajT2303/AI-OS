package com.aios.authz.provenance;

import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.DataNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The lineage graph one workflow accumulates: every {@link DataNode} it has
 * read or derived, and how each was derived from the others.
 * {@link #effectiveClassification} — the least upper bound over a node's
 * transitive {@code derivedFrom} closure — is the primitive the whole thesis
 * rests on: a report carries the sensitivity of everything that produced it,
 * regardless of what its own text says.
 *
 * <p>Immutable and functional: {@link #withNode} returns a new graph rather
 * than mutating this one, matching {@code AuthorizationState}'s own
 * immutability.
 */
public final class ProvenanceGraph {

    private final Map<String, DataNode> nodesById;

    public ProvenanceGraph() {
        this(Map.of());
    }

    private ProvenanceGraph(Map<String, DataNode> nodesById) {
        this.nodesById = nodesById;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProvenanceGraph that)) {
            return false;
        }
        return nodesById.equals(that.nodesById);
    }

    @Override
    public int hashCode() {
        return nodesById.hashCode();
    }

    @Override
    public String toString() {
        return "ProvenanceGraph" + nodesById.keySet();
    }

    /** Adds or replaces a node. Cheap: callers only ever add nodes for actions that were ALLOWed. */
    public ProvenanceGraph withNode(DataNode node) {
        Objects.requireNonNull(node, "node must not be null");
        Map<String, DataNode> next = new HashMap<>(nodesById);
        next.put(node.id(), node);
        return new ProvenanceGraph(Map.copyOf(next));
    }

    public Optional<DataNode> find(String nodeId) {
        return Optional.ofNullable(nodesById.get(nodeId));
    }

    /**
     * Every node the graph currently knows about — both {@link DataAsset} roots
     * and {@link com.aios.authz.domain.DerivedData}. Reconstructing a graph from
     * this set (fold {@link #withNode} over it) reproduces this one exactly;
     * this is what persistence (ENG-30) round-trips through, rather than
     * exposing the internal map directly.
     */
    public Set<DataNode> allNodes() {
        return Set.copyOf(nodesById.values());
    }

    /** Every {@link DataAsset} root the graph currently knows about, regardless of what derives from it. */
    public Set<DataAsset> allRoots() {
        Set<DataAsset> roots = new HashSet<>();
        for (DataNode node : nodesById.values()) {
            if (node instanceof DataAsset asset) {
                roots.add(asset);
            }
        }
        return Set.copyOf(roots);
    }

    /**
     * The least upper bound of {@code nodeId}'s own classification and every
     * source it was (transitively) derived from. Cycle-safe: a node already on
     * the current path contributes its own declared classification once and is
     * not descended into again.
     *
     * @throws NoSuchElementException if {@code nodeId} is not a known node
     */
    public Classification effectiveClassification(String nodeId) {
        DataNode node = require(nodeId);
        return effectiveClassification(node, new HashSet<>());
    }

    private Classification effectiveClassification(DataNode node, Set<String> visiting) {
        if (!visiting.add(node.id())) {
            return node.declaredClassification();
        }
        Classification max = node.declaredClassification();
        for (String sourceId : node.derivedFrom()) {
            DataNode source = nodesById.get(sourceId);
            if (source != null) {
                max = Classification.max(max, effectiveClassification(source, visiting));
            }
        }
        return max;
    }

    /** Every source {@link DataAsset} reachable from {@code nodeId} through the derivation chain. */
    public Set<DataAsset> rootsOf(String nodeId) {
        require(nodeId);
        Set<DataAsset> roots = new HashSet<>();
        collectRoots(nodeId, roots, new HashSet<>());
        return Set.copyOf(roots);
    }

    private void collectRoots(String nodeId, Set<DataAsset> roots, Set<String> visited) {
        if (!visited.add(nodeId)) {
            return;
        }
        DataNode node = nodesById.get(nodeId);
        if (node == null) {
            return;
        }
        if (node instanceof DataAsset asset) {
            roots.add(asset);
            return;
        }
        for (String sourceId : node.derivedFrom()) {
            collectRoots(sourceId, roots, visited);
        }
    }

    /**
     * The roots among {@link #rootsOf(String)} whose own declared
     * classification equals {@code nodeId}'s effective classification — the
     * "dominant contributor(s)" an explanation should name, rather than every
     * root indiscriminately.
     */
    public Set<DataAsset> dominantRootsOf(String nodeId) {
        Classification effective = effectiveClassification(nodeId);
        Set<DataAsset> roots = rootsOf(nodeId);
        Set<DataAsset> dominant = new HashSet<>();
        for (DataAsset root : roots) {
            if (root.declaredClassification() == effective) {
                dominant.add(root);
            }
        }
        return Set.copyOf(dominant);
    }

    /**
     * One derivation path from {@code fromRootId} to {@code toNodeId}, root
     * first. Empty if no such path exists (including when either id is
     * unknown).
     */
    public List<String> lineagePath(String fromRootId, String toNodeId) {
        List<String> path = new ArrayList<>();
        if (findPath(toNodeId, fromRootId, path, new HashSet<>())) {
            Collections.reverse(path);
            return List.copyOf(path);
        }
        return List.of();
    }

    private boolean findPath(String currentId, String targetId, List<String> path, Set<String> visited) {
        if (!visited.add(currentId)) {
            return false;
        }
        DataNode node = nodesById.get(currentId);
        if (node == null) {
            return false;
        }
        path.add(currentId);
        if (currentId.equals(targetId)) {
            return true;
        }
        for (String sourceId : node.derivedFrom()) {
            if (findPath(sourceId, targetId, path, visited)) {
                return true;
            }
        }
        path.remove(path.size() - 1);
        return false;
    }

    private DataNode require(String nodeId) {
        DataNode node = nodesById.get(nodeId);
        if (node == null) {
            throw new NoSuchElementException("No data node with id: " + nodeId);
        }
        return node;
    }
}
