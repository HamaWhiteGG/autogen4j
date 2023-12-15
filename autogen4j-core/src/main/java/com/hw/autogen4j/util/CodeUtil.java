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

package com.hw.autogen4j.util;

import com.hw.autogen4j.entity.CodeBlock;
import com.hw.autogen4j.entity.CodeExecutionConfig;
import com.hw.autogen4j.entity.CodeExecutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author HamaWhite
 */
public class CodeUtil {

    /**
     * matches multi-line code blocks.
     * <p>
     * the [ \t]* matches the potential spaces before language name.
     * the (\w+)? matches the language, where the ? indicates it is optional.
     * the [ \t]* matches the potential spaces (not newlines) after language name.
     * the \r?\n makes sure there is a linebreak after ```.
     * the (.*?) matches the code itself (non-greedy).
     * the \r?\n makes sure there is a linebreak before ```.
     * the [ \t]* matches the potential spaces before closing ``` (the spec allows indentation).
     */
    private static final String CODE_BLOCK_PATTERN = "```[ \\t]*(\\w+)?[ \\t]*\\r?\\n(.*?)\\r?\\n[ \\t]*```";

    /**
     * Extract code from a text.
     *
     * @param text the content to extract code from.
     * @return a list of code blocks, each containing the language and the code.
     */
    public static List<CodeBlock> extractCode(String text) {
        return extractCode(text, false);
    }

    /**
     * Extract code from a text.
     *
     * @param text                 the content to extract code from.
     * @param detectSingleLineCode extracting single line code.
     * @return a list of code blocks, each containing the language and the code.
     */
    public static List<CodeBlock> extractCode(String text, boolean detectSingleLineCode) {
        List<CodeBlock> extracted = new ArrayList<>();
        if (!detectSingleLineCode) {
            Matcher matcher = Pattern.compile(CODE_BLOCK_PATTERN, Pattern.DOTALL).matcher(text);
            while (matcher.find()) {
                extracted.add(new CodeBlock(matcher.group(1), matcher.group(2)));
            }
            return extracted;
        }

        // for extracting multi-line and single-line code block, `([^`]+)` matches inline code.
        Matcher matcher = Pattern.compile(CODE_BLOCK_PATTERN + "|`([^`]+)`").matcher(text);
        // extract the individual code blocks and languages from the matched groups
        while (matcher.find()) {
            String multiLineCode = matcher.group(2);
            String singleLineCode = matcher.group(3);
            if (multiLineCode != null) {
                extracted.add(new CodeBlock(matcher.group(1).strip(), multiLineCode.strip()));
            } else if (singleLineCode != null) {
                extracted.add(new CodeBlock("", singleLineCode.strip()));
            }
        }
        return extracted;
    }


    /**
     * Execute code in a docker container.
     *
     * @param language        The language of the code.
     * @param code            The code to execute.
     * @param executionConfig Configuration for code execution.
     * @return CodeExecutionResult representing the result of code execution.
     */
    public static CodeExecutionResult executeCode(String language, String code, CodeExecutionConfig executionConfig) {
        System.out.println("LLM");
        // Your implementation here
        return null;
    }

}
