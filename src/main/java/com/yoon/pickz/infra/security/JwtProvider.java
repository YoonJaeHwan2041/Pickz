package com.yoon.pickz.infra.security;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    public TokenIssueResult generateAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getAccessTokenValiditySeconds());

        String token = Jwts.builder()
            .issuer(jwtProperties.getIssuer())
            .subject(String.valueOf(userId))
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(getAccessKey())
            .compact();

        return new TokenIssueResult(token, expiresAt);
    }

    public TokenIssueResult generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getRefreshTokenValiditySeconds());

        String token = Jwts.builder()
            .issuer(jwtProperties.getIssuer())
            .subject(String.valueOf(userId))
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(getRefreshKey())
            .compact();

        return new TokenIssueResult(token, expiresAt);
    }

    /**
     * Access Token에서 Claims 파싱. 서명 불일치·만료 등 이상 시 null 반환.
     */
    public Claims parseAccessToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(getAccessKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Refresh Token에서 userId(subject) 파싱. 이상 시 null 반환.
     */
    public Long parseRefreshTokenUserId(String token) {
        try {
            String subject = Jwts.parser()
                .verifyWith(getRefreshKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
            return Long.valueOf(subject);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    private SecretKey getAccessKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    private SecretKey getRefreshKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getRefreshKey()));
    }

    public record TokenIssueResult(String token, Instant expiresAt) {}
}
