package com.aios.authz.harness;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads one {@link Scenario} from a classpath JSON file, failing loudly on a
 * missing file, malformed JSON, or a value that fails a domain type's own
 * validation — the same fail-loud contract as {@code FixtureLoader} (ENG-03).
 * Adding a scenario to the suite means adding one JSON file here; no code
 * change is needed.
 */
public final class ScenarioLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Scenario load(String classpathLocation) {
        try (InputStream in = ScenarioLoader.class.getResourceAsStream(classpathLocation)) {
            if (in == null) {
                throw new ScenarioLoadException("Scenario not found on classpath: " + classpathLocation);
            }
            return objectMapper.readValue(in, Scenario.class);
        } catch (JacksonException e) {
            throw new ScenarioLoadException(
                "Malformed scenario at " + classpathLocation + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ScenarioLoadException(
                "Failed to read scenario at " + classpathLocation + ": " + e.getMessage(), e);
        }
    }

    public List<Scenario> loadAll(List<String> classpathLocations) {
        return classpathLocations.stream().map(this::load).toList();
    }
}
