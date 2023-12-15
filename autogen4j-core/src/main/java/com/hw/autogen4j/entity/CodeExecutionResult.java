package com.hw.autogen4j.entity;


/**
 * Represents the result of code execution.
 *
 * @param exitCode  0 if the code executes successfully.
 * @param logs the error message if the code fails to execute, the stdout otherwise.
 * @param image the docker image name after container run when docker is used.
 *
 * @author HamaWhite
 */
public record CodeExecutionResult(int exitCode, String logs, String image) {

    public CodeExecutionResult(int exitCode, String logs) {
        this(exitCode, logs, null);
    }
}
