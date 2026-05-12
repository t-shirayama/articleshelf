package com.articleshelf.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "extension_access_tokens")
public class ExtensionAccessTokenEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String tokenHash;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private String clientId;
    @Column(nullable = false)
    private String extensionId;
    @Column(nullable = false)
    private String scopes;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant revokedAt;
    @Column(nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getExtensionId() { return extensionId; }
    public void setExtensionId(String extensionId) { this.extensionId = extensionId; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
