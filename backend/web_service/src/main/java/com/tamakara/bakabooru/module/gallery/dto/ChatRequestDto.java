package com.tamakara.bakabooru.module.gallery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequestDto {
    private String model;
    private List<Message> messages;

    @Data
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    public ChatRequestDto(String systemPrompt, String userPrompt, String model) {
        this.model = model;
        this.messages = List.of(
                new ChatRequestDto.Message("system", systemPrompt),
                new ChatRequestDto.Message("user", userPrompt)
        );
    }
}