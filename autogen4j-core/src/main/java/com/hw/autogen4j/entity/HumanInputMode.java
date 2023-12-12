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
