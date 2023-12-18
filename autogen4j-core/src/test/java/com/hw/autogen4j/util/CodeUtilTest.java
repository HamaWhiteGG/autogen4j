package com.hw.autogen4j.util;

import com.hw.autogen4j.entity.CodeExecutionConfig;
import com.hw.autogen4j.entity.CodeExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.hw.autogen4j.util.CodeUtil.executeCode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author HamaWhite
 */
class CodeUtilTest {

    private CodeExecutionConfig config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // set up the configuration for each test
        config = CodeExecutionConfig.builder().workDir(tempDir.toString()).build();
    }

    @Test
    void testExecuteShellSuccessfully() {
        String code = """
                #!/bin/sh
                echo 'Hello, LLM!'
                """;

        CodeExecutionResult result = executeCode("shell", code, config);
        assertThat(result.exitCode()).isZero();
        assertThat(result.logs()).contains("Hello, LLM!");
    }

    @Test
    void testExecutePythonSuccessfully() {
        String code = """
                print('Hello, Python!')
                """;

        CodeExecutionResult result = executeCode("python", code, config);
        assertThat(result.exitCode()).isZero();
        assertThat(result.logs()).contains("Hello, Python!");
    }

    @Test
    void testHandleShellWithError() {
        String code = """
                #!/bin/sh
                exit 1
                """;

        CodeExecutionResult result = executeCode("shell", code, config);
        assertThat(result.exitCode()).isNotZero();
        assertThat(result.logs()).isEmpty();
    }

    @Test
    void testHandlePythonWithError() {
        String code = """
                raise Exception('Test Exception')
                """;

        CodeExecutionResult result = executeCode("python", code, config);
        assertThat(result.exitCode()).isNotZero();
        assertThat(result.logs()).contains("Test Exception");
    }

    @Test
    void testHandleExecutionTimeout() {
        String code = """
                #!/bin/sh
                sleep 10
                """;

        config.setTimeout(1);
        CodeExecutionResult result = executeCode("shell", code, config);
        assertThat(result.exitCode()).isNotZero();
    }
}