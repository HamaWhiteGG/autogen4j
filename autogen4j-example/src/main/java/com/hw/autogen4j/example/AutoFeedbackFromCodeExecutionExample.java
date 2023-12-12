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

package com.hw.autogen4j.example;

import com.hw.autogen4j.agent.AssistantAgent;
import com.hw.autogen4j.agent.UserProxyAgent;
import com.hw.autogen4j.entity.CodeExecutionConfig;

import static com.hw.autogen4j.entity.HumanInputMode.NEVER;

/**
 * @author HamaWhite
 */
public class AutoFeedbackFromCodeExecutionExample {

    public static void main(String[] args) {
        // create an AssistantAgent named "assistant"
        var assistant = AssistantAgent.builder()
                .name("Assistant")
                .build();

        var codeExecutionConfig = CodeExecutionConfig.builder()
                .workDir("coding")
                .build();
        // create a UserProxyAgent instance named "user_proxy"
        var userProxy = UserProxyAgent.builder()
                .name("user_proxy")
                .humanInputMode(NEVER)
                .maxConsecutiveAutoReply(10)
                .isTerminationMsg(x -> x.getOrDefault("content", "").strip().endsWith("TERMINATE"))
                .codeExecutionConfig(codeExecutionConfig)
                .build();

        // the assistant receives a message from the user_proxy, which contains the task description
        userProxy.initiateChat(assistant,
                "What date is today? Compare the year-to-date gain for META and TESLA.");

        // followup of the previous question
        // userProxy.send(assistant,"Plot a chart of their stock price change YTD and save to stock_price_ytd.png.");
    }
}
