package com.articleshelf.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "extension_auth_codes")
public class ExtensionAuthCodeEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String codeHash;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private String clientId;
    @Column(nullable = false)
    private String extensionId;
    @Column(nullable = false)
    private String redirectUri;
    @Column(nullable = false)
    private String codeChallenge;
    @Column(nullable = false)
    private String codeChallengeMethod;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant consumedAt;
    @Column(nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getExtensionId() { return extensionId; }
    public void setExtensionId(String extensionId) { this.extensionId = extensionId; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getCodeChallenge() { return codeChallenge; }
    public void setCodeChallenge(String codeChallenge) { this.codeChallenge = codeChallenge; }
    public String getCodeChallengeMethod() { return codeChallengeMethod; }
    public void setCodeChallengeMethod(String codeChallengeMethod) { this.codeChallengeMethod = codeChallengeMethod; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
