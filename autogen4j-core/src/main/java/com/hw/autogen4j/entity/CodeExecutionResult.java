package com.hw.autogen4j.entity;

/**
 * @author HamaWhite
 */
public record CodeExecutionResult(int exitCode, String logs, String image) {

    public CodeExecutionResult(int exitCode, String logs) {
        this(exitCode, logs, null);
    }
}
