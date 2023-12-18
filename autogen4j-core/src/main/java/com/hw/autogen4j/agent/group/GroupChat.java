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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hw.openai.entity.chat.ChatMessageRole.SYSTEM;

/**
 * @author HamaWhite
 */
@Getter
@Builder
public class GroupChat {

    private static final Logger LOG = LoggerFactory.getLogger(GroupChat.class);

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
    public List<String> agentNames() {
        return extractAgentNames(agents);
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
    public Agent agentByName(String name) {
        return agents.stream()
                .filter(agent -> agent.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No agent found with the given name."));
    }

    private List<String> extractAgentNames(List<Agent> agents) {
        return agents.stream().map(Agent::getName).toList();

    }

    /**
     * Return the message for selecting the next speaker.
     *
     * @param agents A list of agents in the role play game.
     * @return The message for selecting the next speaker.
     */
    public String selectSpeakerMsg(List<Agent> agents) {
        return """
                You are in a role play game. The following roles are available:
                %s

                Read the following conversation.
                Then select the next role from %s to play. Only return the role.
                """.formatted(participantRoles(agents), extractAgentNames(agents));
    }

    /**
     * Selects the next speaker in a conversation.
     *
     * @param lastSpeaker The last speaker who spoken in conversation.
     * @param selector    The ConversableAgent object to provide possible behavior in the conversation.
     * @return An Agent object representing the selected next speaker.
     */
    public Agent selectSpeaker(Agent lastSpeaker, ConversableAgent selector) {
        if (agents.size() < 2) {
            throw new IllegalArgumentException("""
                    GroupChat is underpopulated with %d agents.
                    Please add more agents to the GroupChat or use direct communication instead.
                    """.formatted(agents.size()));
        }
        List<Agent> updatedAgents = new ArrayList<>(agents);
        // remove the last speaker from the list to avoid selecting the same speaker if allowRepeatSpeaker is False
        if (!allowRepeatSpeaker) {
            updatedAgents.remove(lastSpeaker);
        }
        selector.updateSystemMessage(selectSpeakerMsg(updatedAgents));

        List<ChatMessage> updatedMessages = new ArrayList<>(messages);
        updatedMessages.add(new ChatMessage(SYSTEM,
                "Read the above conversation. Then select the next role from %s to play. Only return the role."
                        .formatted(extractAgentNames(updatedAgents))));

        ReplyResult replyResult = selector.generateOaiReply(selector, updatedMessages);
        String content = replyResult.reply().getContent();

        // if exactly one agent is mentioned, use it. Otherwise, leave the OAI response unmodified
        Map<String, Integer> mentions = mentionedAgents(content, updatedAgents);

        if (mentions.size() == 1) {
            String name = mentions.keySet().stream().findFirst().get();
            return agentByName(name);
        } else {
            throw new Autogen4jException("GroupChat selectSpeaker failed to resolve the next speaker's name. " +
                    "This is because the speaker selection OAI call returned:\n %s", content);
        }
    }

    /**
     * Get the roles of a list of agents.
     *
     * @param agents A list of agents.
     * @return A string of the roles of each agent in the list.
     */
    private String participantRoles(List<Agent> agents) {
        List<String> roles = agents.stream()
                .map(agent -> {
                    if (StringUtils.isEmpty(agent.getSystemMessage())) {
                        LOG.warn("The agent {} has an empty systemMessage, and may not work well with GroupChat.",
                                agent.getName());
                    }
                    return agent.getName() + ": " + agent.getSystemMessage();
                }).toList();
        return String.join("\n", roles);
    }

    /**
     * Counts the number of times each agent is mentioned in the provided message content.
     *
     * @param content The content of the message, either as a single string or a list of strings.
     * @param agents  A list of Agent objects
     * @return a map counter for mentioned agents.
     */
    private Map<String, Integer> mentionedAgents(String content, List<Agent> agents) {
        Map<String, Integer> mentions = new HashMap<>();
        for (Agent agent : agents) {
            // finds agent mentions, taking word boundaries into account
            Pattern pattern = Pattern.compile("(?<=\\W)" + Pattern.quote(agent.getName()) + "(?=\\W)");
            // pad the message to help with matching
            Matcher matcher = pattern.matcher(" " + content + " ");
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            if (count > 0) {
                mentions.put(agent.getName(), count);
            }
        }
        return mentions;
    }

}
