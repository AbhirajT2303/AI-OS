/**
 * Pure domain vocabulary: principals, intent, data assets, actions, trajectory.
 *
 * <p>No Spring, no I/O, no dependency on any other package in this project. Every
 * type here must be constructible and testable with a bare {@code new}. This is
 * what keeps the authorization semantics readable independent of any framework —
 * see ADR-0002.
 */
package com.aios.authz.domain;
