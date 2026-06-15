package com.yoon.pickz.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String issuer;
    private String secret;
    private String refreshKey;
    private long accessTokenValiditySeconds;
    private long refreshTokenValiditySeconds;
}
