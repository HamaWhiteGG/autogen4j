package com.hw.autogen4j.agent;

import java.util.List;
import java.util.Map;

/**
 * An agent can communicate with other agents and perform actions.
 * Different agents can differ in what actions they perform in the `receive` method.
 *
 * @author HamaWhite
 */
public interface Agent {

    /**
     * Send a message to another agent.
     */
    void send(Map<String, ?> messages, Agent recipient, boolean requestReply);

    /**
     * Receive a message from another agent.
     */
    void receive(Map<String, ?> messages, Agent sender, boolean requestReply);

    /**
     * Generate a reply based on the received messages.
     */
    void generateReply(List<Map<String, ?>> messages, Agent sender);
}
