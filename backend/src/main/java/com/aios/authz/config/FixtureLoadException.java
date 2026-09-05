package com.aios.authz.config;

/**
 * Thrown when a fixture file is missing, malformed, or fails validation. Always
 * unchecked and always fatal to startup — a fixture is either fully usable or the
 * application must not start, per ENG-03's "fail loudly" requirement.
 */
public class FixtureLoadException extends RuntimeException {

    public FixtureLoadException(String message) {
        super(message);
    }

    public FixtureLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
