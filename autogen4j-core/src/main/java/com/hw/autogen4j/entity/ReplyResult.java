package com.hw.autogen4j.entity;

import com.hw.openai.entity.chat.ChatMessage;

/**
 * @author HamaWhite
 */
public record ReplyResult(boolean terminate, ChatMessage reply) {
}