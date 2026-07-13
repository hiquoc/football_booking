package com.project.user.dto;

public record ChatWebhookPayload(
        User user,
        Conversation conversation,
        Message message,
        Context context
) {
    public record User(String id, String role, String name) {
    }

    public record Conversation(String id, String messageId) {
    }

    public record Message(String text, String language) {
    }

    public record Context(String platform, String timezone) {
    }
}
