package org.prolegacy.atom2027.common.auth.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.checkerframework.checker.index.qual.NonNegative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

@Component
public class JwtTokenUtil {
    Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${auth.jwt-secret}")
    private String jwtSecret;

    @Value("${auth.jwt-expiration-minutes}")
    private Integer expirationMinutes;

    private final String jwtIssuer = "org.prolegacy";

    @Getter
    private final Cache<String, Object> blacklistedTokens = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, Object>() {
                @Override
                public long expireAfterCreate(String token, Object value, long currentTime) {
                    SecretKey jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                    JwtParser jwtParser = Jwts.parser()
                            .verifyWith(jwtSecretKey)
                            .build();
                    Claims claims = jwtParser
                            .parseSignedClaims(token)
                            .getPayload();

                    return Duration.between(Instant.now(), claims.getExpiration().toInstant()).toNanos();
                }

                @Override
                public long expireAfterUpdate(String key, Object value, long currentTime, @NonNegative long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(String key, Object value, long currentTime, @NonNegative long currentDuration) {
                    System.out.println(currentDuration);
                    return currentDuration;
                }
            })
            .build();

    public String generateAccessToken(String username) {
        SecretKey jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Calendar calendar = Calendar.getInstance();
        Date currentTime = calendar.getTime();
        calendar.add(Calendar.MINUTE, expirationMinutes);
        Date expirationTime = calendar.getTime();
        return Jwts.builder()
                .subject(username)
                .issuer(jwtIssuer)
                .issuedAt(currentTime)
                .expiration(expirationTime)
                .signWith(jwtSecretKey)
                .compact();
    }

    public Instant getExpirationTimeFromNow() {
        Instant instant = Instant.now();
        instant = instant.plus(expirationMinutes, ChronoUnit.MINUTES);
        return instant;
    }

    public String getUsername(String token) {
        SecretKey jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        JwtParser jwtParser = Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build();
        Claims claims = jwtParser
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validate(String token) {
        try {
            SecretKey jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            JwtParser jwtParser = Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build();
            jwtParser.parseSignedClaims(token);

            return !blacklistedTokens.asMap().containsKey(token);
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature - {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token - {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token - {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token - {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty - {}", ex.getMessage());
        }
        return false;
    }

}
