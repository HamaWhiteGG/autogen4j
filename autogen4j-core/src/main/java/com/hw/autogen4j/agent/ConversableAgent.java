package com.hw.autogen4j.agent;

import com.hw.autogen4j.entity.HumanInputMode;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.hw.autogen4j.entity.HumanInputMode.TERMINATE;

/**
 * A class for generic conversable agents which can be configured as assistant or user proxy.
 * <p>
 * After receiving each message, the agent will send a reply to the sender unless the msg is a termination msg.
 * For example, AssistantAgent and UserProxyAgent are subclasses of this class, configured with different settings.
 *
 * @author HamaWhite
 */
public class ConversableAgent implements Agent {

    /**
     * name of the agent.
     */
    private String name;

    /**
     * system message for the ChatCompletion inference.
     */
    private String systemMessage = "You are a helpful AI Assistant.";

    private HumanInputMode humanInputMode = TERMINATE;

    /**
     * Mapping function names (passed to llm) to functions.
     */
    private Map<String, Function<?, ?>> functionMap;


    /**
     * maximum number of consecutive auto replies
     */
    private int maxConsecutiveAutoReply = 10;


    @Override
    public void send(Map<String, ?> messages, Agent recipient, boolean requestReply) {

    }

    @Override
    public void receive(Map<String, ?> messages, Agent sender, boolean requestReply) {

    }

    @Override
    public void generateReply(List<Map<String, ?>> messages, Agent sender) {

    }
}
