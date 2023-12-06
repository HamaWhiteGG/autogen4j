package com.hw.autogen4j.exception;

import java.io.Serial;

/**
 * Autogen4jException
 *
 * @author HamaWhite
 */
public class Autogen4jException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 193141189399279147L;

    /**
     * Creates a new Exception with the given message and null as the cause.
     *
     * @param message The exception message
     */
    public Autogen4jException(String message) {
        super(message);
    }

    /**
     * Creates a new Autogen4jException with the given formatted message and arguments.
     *
     * @param message The exception message format string
     * @param args    Arguments to format the message
     */
    public Autogen4jException(String message, Object... args) {
        super(String.format(message, args));
    }

    /**
     * Creates a new exception with a null message and the given cause.
     *
     * @param cause The exception that caused this exception
     */
    public Autogen4jException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message The exception message
     * @param cause   The exception that caused this exception
     */
    public Autogen4jException(String message, Throwable cause) {
        super(message, cause);
    }
}
