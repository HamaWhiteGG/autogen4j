package com.hw.autogen4j.entity;

/**
 * Whether to ask for human inputs every time a message is received.
 *
 * @author HamaWhite
 */
public enum HumanInputMode {
    /**
     * the agent prompts for human input every time a message is received. Under this mode, the conversation stops
     * when the human input is "exit", or when is_termination_msg is True and there is no human input.
     */
    ALWAYS,

    /**
     * the agent only prompts for human input only when a termination message is received or the number of auto reply
     * reaches the max_consecutive_auto_reply.
     */
    TERMINATE,

    /**
     * the agent will never prompt for human input. Under this mode, the conversation stops when the number of auto
     * reply reaches the max_consecutive_auto_reply or when is_termination_msg is True.
     */
    NEVER
}
