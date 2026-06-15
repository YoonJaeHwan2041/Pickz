package com.yoon.pickz.infra.security;

public record AuthenticatedUser(Long userId, String email) {}
