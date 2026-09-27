package com.example.messenger.repository;

import com.example.messenger.entity.Friendship;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    List<Friendship> findAllByOwner_IdOrderByFriend_UsernameAsc(Long ownerId);

    Optional<Friendship> findByOwner_IdAndFriend_Id(Long ownerId, Long friendId);

    boolean existsByOwner_IdAndFriend_Id(Long ownerId, Long friendId);
}