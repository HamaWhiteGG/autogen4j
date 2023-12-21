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

package com.hw.autogen4j.agent.group;

import com.hw.autogen4j.agent.Agent;
import com.hw.autogen4j.agent.ConversableAgent;
import com.hw.autogen4j.entity.ReplyResult;
import com.hw.autogen4j.exception.Autogen4jException;
import com.hw.openai.entity.chat.ChatMessage;

import java.util.List;

import static com.hw.autogen4j.entity.HumanInputMode.NEVER;
import static com.hw.openai.entity.chat.ChatMessageRole.FUNCTION;

/**
 * A chat manager agent that can manage a group chat of multiple agents.
 *
 * @author HamaWhite
 */
public class GroupChatManager extends ConversableAgent {

    private final GroupChat groupChat;

    protected GroupChatManager(Builder builder) {
        super(builder);
        this.groupChat = builder.groupChat;

        this.registerReply(this::runChat);
    }

    /**
     * Run a group chat.
     * <p>
     * In group chat, all participants send msgs to the group chat manager and the group chat manager broadcast them.
     * The participants don't directly send msgs to each other. But because of the broadcast, it creates the experience
     * that every participant sees msgs from other participants.
     *
     * @param sender   The agent object representing the sender of the message.
     * @param messages A list of message, representing the conversation history.
     * @return a reply result.
     */
    private ReplyResult runChat(Agent sender, List<ChatMessage> messages) {
        ChatMessage message = messages.get(messages.size() - 1);
        Agent speaker = sender;
        for (int i = 0; i < groupChat.getMaxRound(); i++) {
            // set the name to speaker's name if the role is not function
            if (!FUNCTION.equals(message.getRole())) {
                message.setName(speaker.getName());
            }
            groupChat.append(message);
            // the conversation is over
            if (isTerminationMsg.test(message)) {
                break;
            }
            // broadcast the message to all agents except the speaker
            for (Agent agent : groupChat.getAgents()) {
                if (!agent.equals(speaker)) {
                    send(agent, message, false, true);
                }
            }
            ChatMessage reply;
            try {
                // select the next speaker.
                speaker = groupChat.selectSpeaker(speaker, this);
                // Let the speaker speak.
                reply = speaker.generateReply(this, List.of());
            } catch (Exception e) {
                // let the admin agent speak if interrupted.
                if (groupChat.agentNames().contains(groupChat.getAdminName())) {
                    // admin agent is one of the participants.
                    speaker = groupChat.agentByName(groupChat.getAdminName());
                    reply = speaker.generateReply(this, List.of());
                } else {
                    throw new Autogen4jException("Admin agent is not found in the participants.", e);
                }
            }
            // the speaker sends the message without requesting a reply.
            speaker.send(this, reply, false, false);
            message = lastMessage(speaker);
        }
        return new ReplyResult(true, null);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ConversableAgent.Builder<Builder> {

        private GroupChat groupChat;

        public Builder groupChat(GroupChat groupChat) {
            this.groupChat = groupChat;
            return this;
        }

        private Builder() {
            super();
            this.name = "chat_manager";
            this.maxConsecutiveAutoReply = Integer.MAX_VALUE;
            this.systemMessage = "Group chat manager.";
            this.humanInputMode = NEVER;
        }

        @Override
        public GroupChatManager build() {
            return new GroupChatManager(this);
        }
    }
}
