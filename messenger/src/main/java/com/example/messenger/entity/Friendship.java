package com.example.messenger.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_friends", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_friends_owner_friend", columnNames = {"owner_id", "friend_id"}))
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "friend_id", nullable = false)
    private UserAccount friend;

    protected Friendship() {
    }

    public Friendship(UserAccount owner, UserAccount friend) {
        this.owner = owner;
        this.friend = friend;
    }

    public UserAccount getFriend() {
        return friend;
    }
}