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
import com.hw.langchain.base.language.BaseLanguageModel;
import com.hw.langchain.chat.models.openai.ChatOpenAI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

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
    protected String name;


    /**
     * system message for the ChatCompletion inference.
     */
    protected String systemMessage;

    /**
     * a function that takes a message in the form of a dictionary and
     * returns a boolean value indicating if this received message is a termination message.
     * The dict can contain the following keys: "content", "role", "name", "function_call".
     */
    protected Predicate<Map<String, String>> isTerminationMsg;

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

    /**
     * default language model is gpt-4.
     */
    protected BaseLanguageModel llm;

    private Map<Agent, Integer> consecutiveAutoReplyCounter = new HashMap<>();
    private Map<Agent, Boolean> replyAtReceive = new HashMap<>();
    private Map<Agent, List<?>> oaiMessages = new HashMap<>();

    protected ConversableAgent(Builder<?> builder) {
        this.name = builder.name;
        this.systemMessage = builder.systemMessage;
        this.isTerminationMsg = builder.isTerminationMsg;
        this.maxConsecutiveAutoReply = builder.maxConsecutiveAutoReply;
        this.humanInputMode = builder.humanInputMode;
        this.functionMap = builder.functionMap;
        this.codeExecutionConfig = builder.codeExecutionConfig;
        this.llm = builder.llm;
    }

    @Override
    public void send(Agent recipient, Map<String, ?> messages, boolean requestReply) {

    }

    @Override
    public void receive(Agent sender, Map<String, ?> messages, boolean requestReply) {

    }

    private void prepareChat(ConversableAgent recipient, boolean clearHistory) {
        this.resetConsecutiveAutoReplyCounter(recipient);
        recipient.resetConsecutiveAutoReplyCounter(this);

        this.replyAtReceive.put(recipient, true);
        recipient.replyAtReceive.put(this, true);

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
        send(recipient, message, silent);
    }

    private void resetConsecutiveAutoReplyCounter(Agent sender) {
        Optional.ofNullable(sender).ifPresentOrElse(
                value -> consecutiveAutoReplyCounter.put(value, 0),
                consecutiveAutoReplyCounter::clear
        );
    }

    /**
     * Clear the chat history of the agent.
     *
     * @param agent the agent with whom the chat history to clear. If null, clear the chat history with all agents.
     */
    private void clearHistory(Agent agent) {
        Optional.ofNullable(agent).ifPresentOrElse(
                value -> oaiMessages.get(value).clear(),
                oaiMessages::clear
        );
    }

    @Override
    public void generateReply(Agent sender, List<Map<String, ?>> messages) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConversableAgent that = (ConversableAgent) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
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
         * The dict can contain the following keys: "content", "role", "name", "function_call".
         */
        protected Predicate<Map<String, String>> isTerminationMsg;

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

        /**
         * default language model is gpt-4.
         */
        protected BaseLanguageModel llm;

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

        public T isTerminationMsg(Predicate<Map<String, String>> isTerminationMsg) {
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

        public T llm(BaseLanguageModel llm) {
            this.llm = llm;
            return (T) this;
        }


        public ConversableAgent build() {
            if (llm == null) {
                llm = ChatOpenAI.builder()
                        .model("gpt-4")
                        .temperature(0)
                        .build()
                        .init();
            }
            return new ConversableAgent(this);
        }
    }
}
