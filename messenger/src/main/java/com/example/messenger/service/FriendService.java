package com.example.messenger.service;

import com.example.messenger.dto.FriendResponse;
import com.example.messenger.entity.Friendship;
import com.example.messenger.entity.UserAccount;
import com.example.messenger.repository.FriendshipRepository;
import com.example.messenger.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FriendService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public FriendService(UserRepository userRepository, FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional
    public FriendResponse addFriend(Long ownerId, Long friendId) {
        if (ownerId.equals(friendId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot add yourself as a friend");
        }

        UserAccount owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
        UserAccount friend = userRepository.findById(friendId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Friendship friendship = friendshipRepository.findByOwner_IdAndFriend_Id(ownerId, friendId)
                .orElseGet(() -> friendshipRepository.save(new Friendship(owner, friend)));
        friendshipRepository.findByOwner_IdAndFriend_Id(friendId, ownerId)
            .orElseGet(() -> friendshipRepository.save(new Friendship(friend, owner)));
        return toResponse(friendship);
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> listFriends(Long ownerId) {
        return friendshipRepository.findAllByOwner_IdOrderByFriend_UsernameAsc(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private FriendResponse toResponse(Friendship friendship) {
        UserAccount friend = friendship.getFriend();
        return new FriendResponse(friend.getId(), friend.getUsername());
    }
}