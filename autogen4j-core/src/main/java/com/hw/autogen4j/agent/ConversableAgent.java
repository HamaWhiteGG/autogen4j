/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hw.autogen4j.agent;

import com.hw.autogen4j.entity.CodeExecutionConfig;
import com.hw.autogen4j.entity.HumanInputMode;
import com.hw.openai.entity.chat.ChatCompletion;
import com.hw.openai.entity.chat.ChatMessage;
import com.hw.openai.entity.chat.ChatMessageRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.hw.autogen4j.entity.HumanInputMode.TERMINATE;
import static com.hw.openai.entity.chat.ChatMessageRole.*;

/**
 * A class for generic conversable agents which can be configured as assistant or user proxy.
 * <p>
 * After receiving each message, the agent will send a reply to the sender unless the msg is a termination msg.
 * For example, AssistantAgent and UserProxyAgent are subclasses of this class, configured with different settings.
 *
 * @author HamaWhite
 */
public class ConversableAgent extends Agent {

    private static final Logger LOG = LoggerFactory.getLogger(ConversableAgent.class);

    /**
     * system message for the ChatCompletion inference.
     */
    protected String systemMessage;

    /**
     * a function that takes a message in the form of a dictionary and
     * returns a boolean value indicating if this received message is a termination message.
     */
    protected Predicate<ChatMessage> isTerminationMsg;

    /**
     * maximum number of consecutive auto replies
     */
    protected int maxConsecutiveAutoReply;

    /**
     * whether to ask for human inputs every time a message is received.
     */
    protected HumanInputMode humanInputMode;

    /**
     * mapping function names (passed to llm) to functions.
     */
    protected Map<String, Function<?, ?>> functionMap;

    /**
     * config for the code execution.
     */
    protected CodeExecutionConfig codeExecutionConfig;

    protected ChatCompletion chatCompletion;

    private final Map<Agent, Integer> consecutiveAutoReplyCounter = new HashMap<>();
    private final Map<Agent, Boolean> replyAtReceive = new HashMap<>();
    private final Map<Agent, List<ChatMessage>> oaiMessages = new HashMap<>();

    protected ConversableAgent(Builder<?> builder) {
        this.name = builder.name;
        this.systemMessage = builder.systemMessage;
        this.isTerminationMsg = builder.isTerminationMsg;
        this.maxConsecutiveAutoReply = builder.maxConsecutiveAutoReply;
        this.humanInputMode = builder.humanInputMode;
        this.functionMap = builder.functionMap;
        this.codeExecutionConfig = builder.codeExecutionConfig;
        this.chatCompletion = builder.chatCompletion;
    }

    /**
     * Append a message to the ChatCompletion conversation.
     */
    private void appendMessage(Agent agent, ChatMessage message, ChatMessageRole role) {
        ChatMessage oaiMessage = new ChatMessage(message);
        if (!FUNCTION.equals(message.getRole())) {
            oaiMessage.setRole(role);
        }
        oaiMessages.computeIfAbsent(agent, key -> new ArrayList<>()).add(oaiMessage);
    }

    @Override
    public void send(Agent recipient, ChatMessage message, boolean requestReply, boolean silent) {
        // when the agent composes and sends the message, the role of the message is "assistant" unless it's "function".
        appendMessage(recipient, message, ASSISTANT);

        recipient.receive(this, message, requestReply, silent);
    }

    private void printReceivedMessage(Agent sender, ChatMessage message) {
        LOG.info("{} (to {}):\n", sender.getName(), this.getName());
        LOG.info("{}", message.getContent());

        String repeatedDashes = "-".repeat(80);
        LOG.info("\n{}", repeatedDashes);
    }

    private void processReceivedMessage(Agent sender, ChatMessage message, boolean silent) {
        // when the agent receives a message, the role of the message is "user" unless it's "function".
        appendMessage(sender, message, USER);

        if (silent) {
            printReceivedMessage(sender, message);
        }
    }

    @Override
    public void receive(Agent sender, ChatMessage message, boolean requestReply, boolean silent) {
        processReceivedMessage(sender, message, silent);

        if (requestReply) {
            var reply = generateReply(sender, oaiMessages.get(sender));
            if (reply != null) {
                send(sender, reply, requestReply, silent);
            }
        }
    }

    private void prepareChat(ConversableAgent recipient, boolean clearHistory) {
        this.resetConsecutiveAutoReplyCounter(recipient);
        recipient.resetConsecutiveAutoReplyCounter(this);

        this.replyAtReceive.putIfAbsent(recipient, true);
        recipient.replyAtReceive.putIfAbsent(this, true);

        if (clearHistory) {
            this.clearHistory(recipient);
            recipient.clearHistory(this);
        }
    }

    /**
     * Initiate a chat with the recipient agent.
     * This method will clear the chat history with the agent, but it won't print the messages for this conversation.
     *
     * @param recipient the recipient agent.
     * @param message   the message to send.
     */
    public void initiateChat(ConversableAgent recipient, String message) {
        initiateChat(recipient, message, true, false);
    }

    /**
     * Initiate a chat with the recipient agent.
     *
     * @param recipient    the recipient agent.
     * @param message      the message to send.
     * @param clearHistory whether to clear the chat history with the agent.
     * @param silent       whether to print the messages for this conversation.
     */
    public void initiateChat(ConversableAgent recipient, String message, boolean clearHistory, boolean silent) {
        prepareChat(recipient, clearHistory);
        send(recipient, new ChatMessage(message), false, silent);
    }

    private void resetConsecutiveAutoReplyCounter(Agent sender) {
        Optional.ofNullable(sender).ifPresentOrElse(
                value -> consecutiveAutoReplyCounter.put(value, 0),
                consecutiveAutoReplyCounter::clear);
    }

    /**
     * Clear the chat history of the agent.
     *
     * @param agent the agent with whom the chat history to clear. If null, clear the chat history with all agents.
     */
    private void clearHistory(Agent agent) {
        if (agent != null && oaiMessages.containsKey(agent)) {
            oaiMessages.get(agent).clear();
        } else {
            oaiMessages.clear();
        }
    }

    @Override
    public ChatMessage generateReply(Agent sender, List<ChatMessage> messages) {
        return null;
    }

    @SuppressWarnings("unchecked")
    protected static class Builder<T extends Builder<T>> {

        /**
         * name of the agent.
         */
        protected String name;

        /**
         * system message for the ChatCompletion inference.
         */
        protected String systemMessage = "You are a helpful AI Assistant.";

        /**
         * a function that takes a message in the form of a dictionary and
         * returns a boolean value indicating if this received message is a termination message.
         */
        protected Predicate<ChatMessage> isTerminationMsg;

        /**
         * maximum number of consecutive auto replies
         */
        protected int maxConsecutiveAutoReply = 10;

        /**
         * whether to ask for human inputs every time a message is received.
         */
        protected HumanInputMode humanInputMode = TERMINATE;

        /**
         * mapping function names (passed to llm) to functions.
         */
        protected Map<String, Function<?, ?>> functionMap;

        /**
         * config for the code execution.
         */
        protected CodeExecutionConfig codeExecutionConfig;

        protected ChatCompletion chatCompletion;

        protected Builder() {
        }

        public T name(String name) {
            this.name = name;
            return (T) this;
        }

        public T systemMessage(String systemMessage) {
            this.systemMessage = systemMessage;
            return (T) this;
        }

        public T isTerminationMsg(Predicate<ChatMessage> isTerminationMsg) {
            this.isTerminationMsg = isTerminationMsg;
            return (T) this;
        }

        public T maxConsecutiveAutoReply(int maxConsecutiveAutoReply) {
            this.maxConsecutiveAutoReply = maxConsecutiveAutoReply;
            return (T) this;
        }

        public T humanInputMode(HumanInputMode humanInputMode) {
            this.humanInputMode = humanInputMode;
            return (T) this;
        }

        public T functionMap(Map<String, Function<?, ?>> functionMap) {
            this.functionMap = functionMap;
            return (T) this;
        }

        public T codeExecutionConfig(CodeExecutionConfig codeExecutionConfig) {
            this.codeExecutionConfig = codeExecutionConfig;
            return (T) this;
        }

        public T chatCompletion(ChatCompletion chatCompletion) {
            this.chatCompletion = chatCompletion;
            return (T) this;
        }

        public ConversableAgent build() {
            return new ConversableAgent(this);
        }
    }
}
