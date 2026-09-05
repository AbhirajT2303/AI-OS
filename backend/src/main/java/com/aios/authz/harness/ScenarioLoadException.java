package com.aios.authz.harness;

/** Thrown when a scenario file is missing, malformed, or fails validation. */
public class ScenarioLoadException extends RuntimeException {

    public ScenarioLoadException(String message) {
        super(message);
    }

    public ScenarioLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
