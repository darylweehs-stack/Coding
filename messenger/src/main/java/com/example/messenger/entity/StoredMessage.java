package com.example.messenger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class StoredMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String conversationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserAccount sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(nullable = false)
    private Instant timestamp;

    private Instant deliveredAt;

    protected StoredMessage() {
    }

    public StoredMessage(UserAccount sender, UserAccount recipient, String content, Instant timestamp) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = timestamp;
        this.conversationId = generateConversationId(sender, recipient);
    }

    public Long getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public UserAccount getSender() {
        return sender;
    }

    public UserAccount getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    private String generateConversationId(UserAccount sender, UserAccount recipient) {
        if (sender.getId() < recipient.getId()) {
            return sender.getId() + "_" + recipient.getId();
        } else {
            return recipient.getId() + "_" + sender.getId();
        }
    }
}