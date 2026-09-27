package com.example.messenger.service;

import com.example.messenger.dto.MessageResponse;
import com.example.messenger.dto.SendMessageRequest;
import com.example.messenger.repository.FriendshipRepository;
import com.example.messenger.entity.StoredMessage;
import com.example.messenger.entity.UserAccount;
import com.example.messenger.repository.MessageRepository;
import com.example.messenger.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessagingService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final FriendshipRepository friendshipRepository;

    public MessagingService(
            UserRepository userRepository,
            MessageRepository messageRepository,
            FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional
    public MessageResponse send(Long senderId, SendMessageRequest request) {
        UserAccount sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
        UserAccount recipient = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));
        if (!friendshipRepository.existsByOwner_IdAndFriend_Id(senderId, recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Add this user as a friend before sending a message");
        }
        StoredMessage message = messageRepository.save(
                new StoredMessage(sender, recipient, request.content(), request.timestamp()));
        return toResponse(message);
    }

    @Transactional
    public List<MessageResponse> receiveNew(@NonNull Long recipientId) {
        List<StoredMessage> messages = messageRepository
                .findByRecipient_IdAndDeliveredAtIsNullOrderByTimestampAscIdAsc(recipientId);
        Instant deliveredAt = Instant.now();
        messages.forEach(message -> message.setDeliveredAt(deliveredAt));
        return messages.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> conversation(@NonNull Long userId, @NonNull Long friendId) {
        if (userId.equals(friendId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A conversation requires another user");
        }
        if (!friendshipRepository.existsByOwner_IdAndFriend_Id(userId, friendId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversation history is only available for friends");
        }

        long firstUserId = Math.min(userId, friendId);
        long secondUserId = Math.max(userId, friendId);
        String conversationId = firstUserId + "_" + secondUserId;
        return messageRepository.findByConversationIdOrderByTimestampAscIdAsc(conversationId).stream()
                .map(this::toResponse)
                .toList();
    }

    private MessageResponse toResponse(@NonNull StoredMessage message) {
        return new MessageResponse(message.getId(), message.getSender().getId(),
                message.getRecipient().getId(), message.getContent(), message.getTimestamp());
    }
}