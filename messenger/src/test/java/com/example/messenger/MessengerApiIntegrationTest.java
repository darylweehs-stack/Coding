package com.example.messenger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.messenger.dto.LoginRequest;
import com.example.messenger.dto.RegisterRequest;
import com.example.messenger.repository.FriendshipRepository;
import com.example.messenger.repository.MessageRepository;
import com.example.messenger.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class MessengerApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

        @Autowired
        private FriendshipRepository friendshipRepository;

    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void cleanDatabase() {
        messageRepository.deleteAll();
        friendshipRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void usersCanRegisterLoginSendAndReceiveMessagesOnce() throws Exception {
        String senderToken = register("sender");
        String recipientToken = register("recipient");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("sender", "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode login = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        senderToken = login.get("token").asText();

        Instant sentAt = Instant.parse("2026-01-02T03:04:05Z");
        String messageJson = objectMapper.writeValueAsString(new SendMessagePayload(2L, "Hello", sentAt.toString()));
        mockMvc.perform(post("/api/messages/send")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/friends/2")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.username").value("recipient"));

        mockMvc.perform(get("/api/friends")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].username").value("recipient"));

        mockMvc.perform(get("/api/friends")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].username").value("sender"));

        mockMvc.perform(post("/api/messages/send")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timestamp").value(sentAt.toString()))
                .andExpect(jsonPath("$.content").value("Hello"));

        mockMvc.perform(get("/api/messages/new")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hello"))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].timestamp").value(sentAt.toString()));

        mockMvc.perform(get("/api/messages/new")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        String replyJson = objectMapper.writeValueAsString(
                new SendMessagePayload(1L, "Reply", Instant.parse("2026-01-02T03:05:05Z").toString()));
        mockMvc.perform(post("/api/messages/send")
                        .header("Authorization", "Bearer " + recipientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Reply"));

        mockMvc.perform(get("/api/messages/new")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Reply"))
                .andExpect(jsonPath("$[0].userId").value(1));

        mockMvc.perform(get("/api/messages/conversations/2")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hello"))
                .andExpect(jsonPath("$[1].content").value("Reply"))
                .andExpect(jsonPath("$[0].timestamp").value(sentAt.toString()));

        mockMvc.perform(get("/api/messages/conversations/1")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].senderId").value(1))
                .andExpect(jsonPath("$[1].senderId").value(2));

        mockMvc.perform(get("/api/messages/conversations/999")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/messages/new"))
                .andExpect(status().isUnauthorized());
    }

        private record SendMessagePayload(Long userId, String content, String timestamp) {
        }

    private String register(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(username, "password123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}