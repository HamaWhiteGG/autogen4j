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
import com.hw.openai.entity.chat.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author HamaWhite
 */
@Getter
@Builder
public class GroupChat {

    /**
     * the name of the admin agent if there is one.
     */
    @Builder.Default
    private String adminName = "Admin";

    /**
     * a list of participating agents.
     */
    private List<Agent> agents;

    /**
     * a list of messages in the group chat.
     */
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * the maximum number of rounds.
     */
    @Builder.Default
    private int maxRound = 10;

    /**
     * whether to allow the same speaker to speak consecutively.
     */
    @Builder.Default
    private boolean allowRepeatSpeaker = true;

    /**
     * Return the names of the agents in the group chat.
     *
     * @return a list of agent names
     */
    public List<String> getAgentNames() {
        return agents.stream().map(Agent::getName).toList();
    }

    public GroupChat append(ChatMessage message) {
        messages.add(message);
        return this;
    }

    /**
     * Returns the agent with a given name.
     *
     * @param name The name of the agent to find.
     * @return An Agent object with the given name.
     * @throws IllegalArgumentException if no agent with the given name is found.
     */
    public Agent getAgentByName(String name) {
        return agents.stream()
                .filter(agent -> agent.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No agent found with the given name."));
    }

    /**
     * Selects the next speaker in a conversation.
     *
     * @param lastSpeaker The last speaker who spoken in conversation.
     * @param selector    The ConversableAgent object to provide possible behavior in the conversation.
     * @return An Agent object representing the selected next speaker.
     */
    public Agent selectSpeaker(Agent lastSpeaker, ConversableAgent selector) {
        return null;
    }
}
