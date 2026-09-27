package com.example.messenger.dto;

public record TokenResponse(Long userId, String username, String token, String tokenType) {
}