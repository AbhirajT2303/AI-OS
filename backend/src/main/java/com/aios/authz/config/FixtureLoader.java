package com.aios.authz.config;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.CollectionType;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads a JSON array from the classpath into a list of records, failing loudly on
 * a missing file or malformed content rather than starting with partial data.
 *
 * <p>Deliberately a plain class, not a Spring bean: a test overrides the fixture
 * set by calling {@link #load} with a different classpath location, with no need
 * to touch {@link FixtureConfig} or {@code application.properties} — see
 * {@code FixtureLoaderTest}.
 */
public final class FixtureLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> List<T> load(String classpathLocation, Class<T> elementType) {
        try (InputStream in = FixtureLoader.class.getResourceAsStream(classpathLocation)) {
            if (in == null) {
                throw new FixtureLoadException("Fixture not found on classpath: " + classpathLocation);
            }
            CollectionType listType =
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(in, listType);
        } catch (JacksonException e) {
            throw new FixtureLoadException(
                "Malformed fixture at " + classpathLocation + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new FixtureLoadException(
                "Failed to read fixture at " + classpathLocation + ": " + e.getMessage(), e);
        }
    }
}
