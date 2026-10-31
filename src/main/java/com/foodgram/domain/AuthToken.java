package com.foodgram.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_tokens")
public class AuthToken {

    @Id
    @Column(name = "token_key", length = 40)
    private String key;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    protected AuthToken() {
    }

    public AuthToken(String key, User user) {
        this.key = key;
        this.user = user;
    }

    public String getKey() { return key; }
    public User getUser() { return user; }
}
