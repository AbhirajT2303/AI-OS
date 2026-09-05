/**
 * The agent harness: replays scenarios through both engines on identical input
 * and produces the RBAC-vs-trajectory comparison. This is the deliverable that
 * turns the thesis into a repeatable experiment — see docs/SCENARIOS.md.
 *
 * <p>Depends on {@link com.aios.authz.engine} (and transitively on
 * {@code domain}, {@code state}, {@code provenance}, {@code policy} for result
 * types). Deterministic — no randomness, no LLM.
 */
package com.aios.authz.harness;
