/**
 * The composition root: Spring wiring for policies, fixtures and the permission
 * catalog. Unlike the other packages, {@code config} may depend on any of them —
 * it is where they are assembled, not where authorization logic lives.
 */
package com.aios.authz.config;
