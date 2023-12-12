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

import lombok.Builder;

/**
 * Config for the code execution.
 *
 * @author HamaWhite
 */
@Builder
public class CodeExecutionConfig {

    /**
     * the working directory for the code execution.
     */
    @Builder.Default
    private String workDir = "extensions";

    /**
     * the docker image to use for code execution.
     */
    private String docker;

    /**
     * the maximum execution time in seconds.
     */
    private int timeout;

    /**
     * the number of messages to look back for code execution
     */
    @Builder.Default
    private int lastMessages = 1;
}
