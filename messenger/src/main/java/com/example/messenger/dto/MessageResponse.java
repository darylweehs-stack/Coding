package com.example.messenger.dto;

import java.time.Instant;

public record MessageResponse(
        Long id,
        Long senderId,
        Long userId,
        String content,
        Instant timestamp) {
}