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
import com.hw.autogen4j.exception.Autogen4jException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.exec.*;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hw.autogen4j.util.FileUtil.deleteFile;
import static com.hw.autogen4j.util.FileUtil.writeCodeToFile;

/**
 * @author HamaWhite
 */
public class CodeUtil {

    private CodeUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Matches multi-line code blocks.
     * <ul>
     * <li>[ \t]* - Matches the potential spaces before the language name.</li>
     * <li>(\w+)? - Matches the language, where the ? indicates it is optional.</li>
     * <li>[ \t]* - Matches the potential spaces (not newlines) after the language name.</li>
     * <li>\r?\n - Ensures there is a linebreak after ```.</li>
     * <li>(.*?) - Matches the code itself (non-greedy).</li>
     * <li>\r?\n - Ensures there is a linebreak before ```.</li>
     * <li>[ \t]* - Matches the potential spaces before the closing ``` (the spec allows indentation).</li>
     * </ul>
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
     * @param language The language of the code.
     * @param code     The code to execute.
     * @param config   Configuration for code execution.
     * @return CodeExecutionResult representing the result of code execution.
     */
    public static CodeExecutionResult executeCode(String language, String code, CodeExecutionConfig config) {
        if (StringUtils.isEmpty(language) || StringUtils.isEmpty(code)) {
            throw new Autogen4jException("Either language or code must be provided.");
        }

        String workDir = config.getWorkDir();
        String codeHash = DigestUtils.md5Hex(code);
        String fileExt = language.startsWith("python") ? "py" : language;
        String filename = String.format("tmp_code_%s.%s", codeHash, fileExt);

        // write the code string to a file specified by the filename.
        writeCodeToFile(workDir, filename, code);

        CodeExecutionResult executionResult = StringUtils.isEmpty(config.getDocker())
                ? executeCodeLocally(language, workDir, filename, config.getTimeout())
                : executeCodeInDocker();

        deleteFile(workDir, filename);
        return executionResult;
    }

    private static String getExecutableForLanguage(String language) throws UnsupportedOperationException {
        return switch (language) {
            case "python" -> language;
            case "shell", "bash", "sh", "powershell" -> "sh";
            default -> throw new Autogen4jException("Language not recognized in code execution: %s", language);
        };
    }

    public static CodeExecutionResult executeCodeLocally(String language, String workDir, String filename,
            int timeout) {
        // set up the command based on language
        String executable = getExecutableForLanguage(language);
        CommandLine commandLine = new CommandLine(executable);
        commandLine.addArgument(filename);

        // set up the execution environment
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File(workDir));
        executor.setExitValue(0);

        // set up the streams for the output of the subprocess
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        executor.setStreamHandler(streamHandler);

        // set up a watchdog to terminate the process if it exceeds the timeout
        ExecuteWatchdog watchdog = new ExecuteWatchdog(TimeUnit.SECONDS.toMillis(timeout));
        executor.setWatchdog(watchdog);

        try {
            // execute the command
            executor.execute(commandLine);
            // process completed before the watchdog terminated it
            String output = outputStream.toString();
            return new CodeExecutionResult(0, output.trim());
        } catch (ExecuteException e) {
            // process finished with an exit value (possibly non-zero)
            String errorOutput = errorStream.toString()
                    .replace(Path.of(workDir).toAbsolutePath() + File.separator, "");

            return new CodeExecutionResult(e.getExitValue(), errorOutput.trim());
        } catch (IOException e) {
            // returns a special result if the process was killed by the watchdog
            throw new Autogen4jException("Error executing code.", e);
        }
    }

    public static CodeExecutionResult executeCodeInDocker() {
        // TODO
        return null;
    }
}