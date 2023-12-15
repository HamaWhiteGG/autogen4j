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

import com.hw.autogen4j.entity.*;
import com.hw.openai.OpenAiClient;
import com.hw.openai.entity.chat.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.hw.autogen4j.entity.HumanInputMode.*;
import static com.hw.autogen4j.util.CodeUtil.executeCode;
import static com.hw.autogen4j.util.CodeUtil.extractCode;
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

    /**
     * a client for interacting with the OpenAI API.
     */
    protected OpenAiClient client;

    /**
     * Chat conversation.
     */
    protected ChatCompletion chatCompletion;

    /**
     * default auto reply when no code execution or llm-based reply is generated.
     */
    protected String defaultAutoReply;

    private final Map<Agent, Integer> consecutiveAutoReplyCounter = new HashMap<>();
    private final Map<Agent, Boolean> replyAtReceive = new HashMap<>();
    private final Map<Agent, List<ChatMessage>> oaiMessages = new HashMap<>();
    private final List<ChatMessage> oaiSystemMessage;

    protected ConversableAgent(Builder<?> builder) {
        this.name = builder.name;
        this.systemMessage = builder.systemMessage;
        this.isTerminationMsg = builder.isTerminationMsg;
        this.maxConsecutiveAutoReply = builder.maxConsecutiveAutoReply;
        this.humanInputMode = builder.humanInputMode;
        this.functionMap = builder.functionMap;
        this.codeExecutionConfig = builder.codeExecutionConfig;
        this.client = builder.client;
        this.chatCompletion = builder.chatCompletion;
        this.defaultAutoReply = builder.defaultAutoReply;

        this.oaiSystemMessage = List.of(new ChatMessage(SYSTEM, systemMessage));
    }

    /**
     * Append a message to the ChatCompletion conversation.
     */
    private void appendOaiMessage(Agent agent, ChatMessage message, ChatMessageRole role) {
        ChatMessage oaiMessage = new ChatMessage(message);
        if (!FUNCTION.equals(message.getRole())) {
            oaiMessage.setRole(role);
        }
        oaiMessages.computeIfAbsent(agent, key -> new ArrayList<>()).add(oaiMessage);
    }

    @Override
    public void send(Agent recipient, ChatMessage message, boolean requestReply, boolean silent) {
        // when the agent composes and sends the message, the role of the message is "assistant" unless it's "function".
        appendOaiMessage(recipient, message, ASSISTANT);

        recipient.receive(this, message, requestReply, silent);
    }

    private void printReceivedMessage(Agent sender, ChatMessage message) {
        LOG.info("{} (to {}):\n", sender.getName(), this.getName());

        if (FUNCTION.equals(message.getRole())) {
            String funcPrint = String.format("***** Response from calling function '%s' *****", message.getName());
            LOG.info(funcPrint);
            LOG.info(message.getContent());
            LOG.info("*".repeat(funcPrint.length()));
        } else {
            if (StringUtils.isNotEmpty(message.getContent())) {
                LOG.info(message.getContent());
            }
            if (CollectionUtils.isNotEmpty(message.getToolCalls())) {
                // only support the first function now.
                FunctionCall functionCall = message.getToolCalls().get(0).getFunction();
                String funcPrint = String.format("***** Suggested function Call: %s *****", functionCall.getName());
                LOG.info(funcPrint);
                LOG.info("Arguments: \n{}", functionCall.getArguments());
                LOG.info("*".repeat(funcPrint.length()));
            }
        }
        LOG.info("\n" + "-".repeat(80));
    }

    private void processReceivedMessage(Agent sender, ChatMessage message, boolean silent) {
        // when the agent receives a message, the role of the message is "user" unless it's "function".
        appendOaiMessage(sender, message, USER);

        if (!silent) {
            printReceivedMessage(sender, message);
        }
    }

    @Override
    public void receive(Agent sender, ChatMessage message, boolean requestReply, boolean silent) {
        processReceivedMessage(sender, message, silent);

        if (requestReply || replyAtReceive.get(sender)) {
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

    /**
     * Generate a reply using llm.
     *
     * @param sender   The agent object representing the sender of the message.
     * @param messages A list of message, representing the conversation history.
     * @return a reply using llm.
     */
    private ReplyResult generateOaiReply(Agent sender, List<ChatMessage> messages) {
        chatCompletion.setMessages(ListUtils.union(oaiSystemMessage, messages));
        ChatCompletionResp response = client.createChatCompletion(chatCompletion);
        return new ReplyResult(true, response.getChoices().get(0).getMessage());
    }

    /**
     * Generate a reply using code execution.
     *
     * @param sender   The agent object representing the sender of the message.
     * @param messages A list of message, representing the conversation history.
     * @return a reply using code execution.
     */
    private ReplyResult generateCodeExecutionReply(Agent sender, List<ChatMessage> messages) {
        if (codeExecutionConfig == null) {
            return new ReplyResult(false, null);
        }

        int lastMessagesNumber = codeExecutionConfig.getLastMessagesNumber();
        int messagesToScan = lastMessagesNumber;
        // indicates auto mode, find when the agent last spoke
        if (lastMessagesNumber == -1) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage message = messages.get(i);
                if (USER.equals(message.getRole())) {
                    break;
                }
                messagesToScan += 1;
            }
        }

        /*
         * iterate through the last n messages reversely. if code blocks are found, execute the code blocks and
         * return the output, if no code blocks are found, continue
         */
        for (int i = 0; i < Math.min(messages.size(), messagesToScan); i++) {
            ChatMessage message = messages.get(messages.size() - 1 - i);
            String content = message.getContent();
            if (StringUtils.isEmpty(content)) {
                continue;
            }

            List<CodeBlock> codeBlocks = extractCode(content);
            CodeExecutionResult result = executeCodeBlocks(codeBlocks);

            String exitCodeToStr = result.exitCode() == 0 ? "execution succeeded" : "execution failed";
            String reply = String.format("exitcode: %s (%s})\nCode output: %s", result.exitCode(), exitCodeToStr, result.logs());
            return new ReplyResult(true, new ChatMessage(reply));
        }
        return new ReplyResult(false, null);
    }

    /**
     * Generate a reply using function call.
     *
     * @param sender   The agent object representing the sender of the message.
     * @param messages A list of message, representing the conversation history.
     * @return a reply using function call.
     */
    private ReplyResult generateFunctionCallReply(Agent sender, List<ChatMessage> messages) {
        ChatMessage message = messages.get(messages.size() - 1);
        if (CollectionUtils.isNotEmpty(message.getToolCalls())) {
            // only supports executing the first function now.
            ChatMessage functionResult = executeFunction(message.getToolCalls().get(0).getFunction());
            return new ReplyResult(true, functionResult);
        }
        return new ReplyResult(false, null);
    }

    /**
     * Check if the conversation should be terminated, and if human reply is provided.
     * <p>
     * This method checks for conditions that require the conversation to be terminated, such as reaching
     * a maximum number of consecutive auto-replies or encountering a termination message. Additionally,
     * it prompts for and processes human input based on the configured human input mode, which can be
     * 'ALWAYS', 'NEVER', or 'TERMINATE'. The method also manages the consecutive auto-reply counter
     * for the conversation and prints relevant messages based on the human input received.
     *
     * @param sender   The agent object representing the sender of the message.
     * @param messages A list of message, representing the conversation history.
     * @return a boolean indicating if the conversation should be terminated and a human reply
     */
    private ReplyResult checkTerminationAndHumanReply(Agent sender, List<ChatMessage> messages) {
        ChatMessage message = messages.get(messages.size() - 1);
        String reply = "";
        String noHumanInputMsg = "";
        if (humanInputMode.equals(ALWAYS)) {
            reply = getHumanInput(
                    String.format("Provide feedback to %s. Press enter to skip and use auto-reply, or type 'exit' to end the conversation: ", sender.getName()));

            noHumanInputMsg = reply.isEmpty() ? "NO HUMAN INPUT RECEIVED." : "";
            // if the human input is empty, and the message is a termination message, then we will terminate the conversation
            reply = !reply.isEmpty() || !isTerminationMsg.test(message) ? reply : "exit";
        } else {
            if (consecutiveAutoReplyCounter.get(sender) > maxConsecutiveAutoReply) {
                if (humanInputMode.equals(NEVER)) {
                    reply = "exit";
                } else {
                    // if humanInputMode equals "TERMINATE"
                    boolean terminate = isTerminationMsg.test(message);

                    String prompt = terminate
                            ? String.format("Please give feedback to %s. Press enter or type 'exit' to stop the conversation: ", sender.getName())
                            : String.format("Please give feedback to %s. Press enter to skip and use auto-reply, or type 'exit' to stop the conversation: ", sender.getName());
                    reply = getHumanInput(prompt);
                    noHumanInputMsg = reply.isEmpty() ? "NO HUMAN INPUT RECEIVED." : "";
                    // if the human input is empty, and the message is a termination message, then we will terminate the conversation
                    reply = !reply.isEmpty() || !terminate ? reply : "exit";
                }
            } else if (isTerminationMsg.test(message)) {
                if (humanInputMode.equals(NEVER)) {
                    reply = "exit";
                } else {
                    // if humanInputMode equals "TERMINATE":
                    reply = getHumanInput(
                            String.format("Please give feedback to %s. Press enter or type 'exit' to stop the conversation: ", sender.getName()));

                    noHumanInputMsg = reply.isEmpty() ? "NO HUMAN INPUT RECEIVED." : "";
                    // If the human input is empty, then we will terminate the conversation
                    reply = reply.isEmpty() ? "exit" : reply;
                }
            }
        }
        // print the noHumanInputMsg
        if (!noHumanInputMsg.isEmpty()) {
            LOG.info("\n>>>>>>>> {}", noHumanInputMsg);
        }
        // stop the conversation
        if ("exit".equals(reply)) {
            // reset the consecutiveAutoReplyCounter
            consecutiveAutoReplyCounter.put(sender, 0);
            return new ReplyResult(true, null);
        }
        // send the human reply
        if (!reply.isEmpty()) {
            // reset the consecutiveAutoReplyCounter
            consecutiveAutoReplyCounter.put(sender, 0);
            return new ReplyResult(true, new ChatMessage(reply));
        }
        // increment the consecutiveAutoReplyCounter
        consecutiveAutoReplyCounter.put(sender, consecutiveAutoReplyCounter.get(sender) + 1);
        if (!humanInputMode.equals(NEVER)) {
            LOG.info("\n>>>>>>>> USING AUTO REPLY...");
        }
        return new ReplyResult(false, null);
    }

    @Override
    public ChatMessage generateReply(Agent sender, List<ChatMessage> messages) {
        // creating a list of method references
        List<BiFunction<Agent, List<ChatMessage>, ReplyResult>> replyFuncList = List.of(
                this::checkTerminationAndHumanReply,
                this::generateFunctionCallReply,
                this::generateCodeExecutionReply,
                this::generateOaiReply);

        // loop through each method
        for (var replyFunc : replyFuncList) {
            ReplyResult replyResult = replyFunc.apply(sender, messages);
            // if termination is required, immediately return the reply
            if (replyResult.terminate()) {
                return replyResult.reply();
            }
        }
        // if no termination occurred, return default auto reply
        return new ChatMessage(defaultAutoReply);
    }

    /**
     * Get human input.
     * Override this method to customize the way to get human input.
     *
     * @param prompt prompt for the human input.
     * @return human input.
     */
    protected String getHumanInput(String prompt) {
        LOG.info(prompt);
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    /**
     * Execute the code blocks and return the result.
     *
     * @param codeBlocks List of code blocks to execute.
     * @return CodeExecutionResult representing the result of code execution.
     */
    private CodeExecutionResult executeCodeBlocks(List<CodeBlock> codeBlocks) {
        StringBuilder allLogs = new StringBuilder();
        CodeExecutionResult result = null;
        for (int i = 0; i < codeBlocks.size(); i++) {
            CodeBlock codeBlock = codeBlocks.get(i);
            String language = codeBlock.language();
            String code = codeBlock.code();
            LOG.info("\n>>>>>>>> EXECUTING CODE BLOCK {} (inferred language is {})...", i + 1, language);

            if (Set.of("bash", "shell", "sh", "python").contains(language.toLowerCase())) {
                result = executeCode(language, code, codeExecutionConfig);
            } else {
                // the language is not supported, then return an error message.
                result = new CodeExecutionResult(1, "unknown language " + language);
            }

            allLogs.append("\n").append(result.logs());
            if (result.exitCode() != 0) {
                return new CodeExecutionResult(result.exitCode(), allLogs.toString());
            }
        }
        return new CodeExecutionResult(result.exitCode(), allLogs.toString());
    }


    /**
     * Execute a function call and return the result.
     *
     * @param functionCall The function call to be executed.
     * @return The result of the function call
     */
    private ChatMessage executeFunction(FunctionCall functionCall) {
        // TODO
        return null;
    }

    @SuppressWarnings("unchecked")
    protected abstract static class Builder<T extends Builder<T>> {

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
        protected Predicate<ChatMessage> isTerminationMsg = e -> "TERMINATE".equals(e.getContent());

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
         * a client for interacting with the OpenAI API.
         */
        protected OpenAiClient client;

        /**
         * Chat conversation.
         */
        protected ChatCompletion chatCompletion;

        /**
         * default auto reply when no code execution or llm-based reply is generated.
         */
        protected String defaultAutoReply = "";


        protected Builder() {
            this.client = OpenAiClient.builder()
                    .requestTimeout(60)
                    .build()
                    .init();

            this.chatCompletion = ChatCompletion.builder()
                    .model("gpt-4")
                    .temperature(0)
                    .build();
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

        public T client(OpenAiClient client) {
            this.client = client;
            return (T) this;
        }

        public T chatCompletion(ChatCompletion chatCompletion) {
            this.chatCompletion = chatCompletion;
            return (T) this;
        }

        public T defaultAutoReply(String defaultAutoReply) {
            this.defaultAutoReply = defaultAutoReply;
            return (T) this;
        }

        protected abstract ConversableAgent build();
    }
}
