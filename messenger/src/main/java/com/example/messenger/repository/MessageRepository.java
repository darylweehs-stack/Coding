package com.example.messenger.repository;

import com.example.messenger.entity.StoredMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<StoredMessage, Long> {
    List<StoredMessage> findByRecipient_IdAndDeliveredAtIsNullOrderByTimestampAscIdAsc(Long recipientId);
}