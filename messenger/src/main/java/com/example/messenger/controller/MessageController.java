package com.example.messenger.controller;

import com.example.messenger.dto.MessageResponse;
import com.example.messenger.dto.SendMessageRequest;
import com.example.messenger.service.MessagingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessagingService messagingService;

    public MessageController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(
            @AuthenticationPrincipal Long senderId,
            @Valid @RequestBody SendMessageRequest request) {
        return messagingService.send(senderId, request);
    }

    @GetMapping("/new")
    public List<MessageResponse> receiveNew(@AuthenticationPrincipal Long recipientId) {
        return messagingService.receiveNew(recipientId);
    }
}