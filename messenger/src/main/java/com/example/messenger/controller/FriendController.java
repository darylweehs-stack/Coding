package com.example.messenger.controller;

import com.example.messenger.dto.FriendResponse;
import com.example.messenger.service.FriendService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendResponse addFriend(@AuthenticationPrincipal Long ownerId, @PathVariable Long userId) {
        return friendService.addFriend(ownerId, userId);
    }

    @GetMapping
    public List<FriendResponse> listFriends(@AuthenticationPrincipal Long ownerId) {
        return friendService.listFriends(ownerId);
    }
}