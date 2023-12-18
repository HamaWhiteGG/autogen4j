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

import com.hw.openai.entity.chat.ChatMessage;

import lombok.Getter;

import java.util.List;

/**
 * An agent can communicate with other agents and perform actions.
 * Different agents can differ in what actions they perform in the `receive` method.
 *
 * @author HamaWhite
 */
@Getter
public abstract class Agent {

    /**
     * name of the agent.
     */
    protected String name;

    /**
     * Send a string message to another agent.
     *
     * @param recipient the recipient of the message.
     * @param message   string message to be sent.
     */
    public void send(Agent recipient, String message) {
        send(recipient, new ChatMessage(message), false, false);
    }

    /**
     * Send a message to another agent.
     *
     * @param recipient    the recipient of the message.
     * @param message      message to be sent.
     * @param requestReply whether to request a reply from the recipient.
     * @param silent       whether to print the message sent.
     */
    public abstract void send(Agent recipient, ChatMessage message, boolean requestReply, boolean silent);

    /**
     * Receive a message from another agent. Once a message is received, this function sends a reply to the sender
     * or stop. The reply can be generated automatically or entered manually by a human.
     *
     * @param sender       sender of an Agent instance.
     * @param message      message from the sender.
     * @param requestReply whether a reply is requested from the sender.
     * @param silent       whether to print the message received.
     */
    public abstract void receive(Agent sender, ChatMessage message, boolean requestReply, boolean silent);

    /**
     * Generate a reply based on the received messages.
     *
     * @param sender   sender of an Agent instance.
     * @param messages a list of messages received.
     * @return a reply message.
     */
    public abstract ChatMessage generateReply(Agent sender, List<ChatMessage> messages);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Agent agent = (Agent) o;

        return name.equals(agent.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
