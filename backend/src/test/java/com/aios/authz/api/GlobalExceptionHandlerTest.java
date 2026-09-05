package com.aios.authz.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalArgumentBecomes400WithItsOwnSafeMessage() {
        ProblemDetail problem = handler.handleIllegalArgument(
            new IllegalArgumentException("Action outputDataId must not be blank if present"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Action outputDataId must not be blank if present");
    }

    @Test
    void unexpectedExceptionBecomes500WithoutLeakingItsRealMessage() {
        RuntimeException sensitive = new RuntimeException("db_password=hunter2 at /internal/path");

        ProblemDetail problem = handler.handleUnexpected(sensitive);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).doesNotContain("hunter2").doesNotContain("/internal/path");
    }
}
