package com.aios.authz.engine;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A trajectory-aware DENY is only meaningful evidence if the baseline could not
 * have produced it. This test is the structural guarantee behind that claim: an
 * accidental dependency from the baseline onto {@code state} or
 * {@code provenance} would silently invalidate every RBAC-vs-trajectory
 * comparison in docs/SCENARIOS.md, so it is enforced here rather than left to
 * code review.
 */
class RbacBaselineEngineIsolationTest {

    private static final String[] FORBIDDEN_PACKAGE_PREFIXES = {
        "com.aios.authz.state", "com.aios.authz.provenance"
    };

    @Test
    void constructorTakesOnlyAPermissionCatalog() {
        Constructor<?>[] constructors = RbacBaselineEngine.class.getDeclaredConstructors();

        assertThat(constructors).hasSize(1);
        assertThat(constructors[0].getParameterTypes()).containsExactly(PermissionCatalog.class);
    }

    @Test
    void noFieldReachesStateOrProvenance() {
        for (Field field : RbacBaselineEngine.class.getDeclaredFields()) {
            String packageName = field.getType().getPackageName();
            assertThat(isForbidden(packageName))
                .as("field '%s' has type '%s' in a forbidden package", field.getName(), field.getType())
                .isFalse();
        }
    }

    private static boolean isForbidden(String packageName) {
        for (String prefix : FORBIDDEN_PACKAGE_PREFIXES) {
            if (packageName.equals(prefix) || packageName.startsWith(prefix + ".")) {
                return true;
            }
        }
        return false;
    }
}
