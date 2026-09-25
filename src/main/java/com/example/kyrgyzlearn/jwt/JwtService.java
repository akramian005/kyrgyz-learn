package com.example.kyrgyzlearn.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.DecodingException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final String secret;
    private final long jwtExpiration;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms}") long jwtExpiration
    ) {
        this.secret = secret;
        this.jwtExpiration = jwtExpiration;
    }

    @PostConstruct
    void validateConfiguration() {
        getSigningKey();
        if (jwtExpiration <= 0) {
            throw new IllegalStateException("security.jwt.expiration-ms must be greater than 0");
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = decodeSecret(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
//    private SecretKey getSigningKey() {
//        return Keys.hmacShaKeyFor(secret.getBytes());
//    }

    private byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("security.jwt.secret must not be blank");
        }

        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException | DecodingException ex) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        extraClaims.put("roles", roles);

        return generateToken(extraClaims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims) //без
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // без
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // без
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractEmail(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

}



