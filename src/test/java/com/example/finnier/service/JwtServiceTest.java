package com.example.finnier.service;

import com.example.finnier.entity.User;
import com.example.finnier.exception.TokenValidationException;
import com.example.finnier.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private final String secretKey =
            "c2VjdXJlLWtleS1mb3ItZmluLmFsbC10ZXN0aW5nLWFuZC1qd3QtdG9rZW4tMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";


    private User user;


    @BeforeEach
    void setUp(){

        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "jwtSecretKey", secretKey);

        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3600000L);


        user = new User();

        user.setUserId(1L);
        user.setEmail("test@gmail.com");
        user.setRole(User.RoleType.CUSTOMER);
        user.setStatus(User.UserStatus.ACTIVE);
    }


    @Test
    void shouldGenerateAccessTokenSuccessfully(){

        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldExtractUserRoleFromValidToken(){

        String token = jwtService.generateAccessToken(user);
        String role = jwtService.extractUserRoleFromToken(token);

        assertEquals("CUSTOMER", role);
    }
    @Test
    void shouldValidateGeneratedToken(){

        String token = jwtService.generateAccessToken(user);

        assertDoesNotThrow(() -> jwtService.validateToken(token));
    }

    @Test
    void shouldContainCorrectSubject(){

        String token = jwtService.generateAccessToken(user);
        String subject = jwtService.validateToken(token).getSubject();
        assertEquals("test@gmail.com", subject);
    }

    @Test
    void shouldRejectExpiredToken(){

        SecretKey key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secretKey));
        String expiredToken =
                Jwts.builder()
                        .subject("test@gmail.com")
                        .issuer("Finnier")
                        .issuedAt(new Date(System.currentTimeMillis()-10000))
                        .expiration(new Date(System.currentTimeMillis()-5000))
                        .signWith(key)
                        .compact();
        assertThrows( TokenValidationException.class, () -> jwtService.validateToken(expiredToken)
        );
    }
    @Test
    void shouldRejectWrongIssuer(){

        SecretKey key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secretKey));
        String token =
                Jwts.builder()
                        .subject("test@gmail.com")
                        .issuer("WrongIssuer")
                        .issuedAt(new Date())
                        .expiration(new Date(System.currentTimeMillis()+60000))
                        .signWith(key)
                        .compact();

        assertThrows( TokenValidationException.class, () -> jwtService.validateToken(token));
    }

    @Test
    void shouldRejectMalformedToken(){

        assertThrows( TokenValidationException.class, () -> jwtService.validateToken(
                        "invalid.jwt.token"
                )
        );
    }



    @Test
    void shouldRejectModifiedToken(){

        String token = jwtService.generateAccessToken(user);
        String modifiedToken = token.substring(0, token.length()-5)+ "xxxxx";

        assertThrows(TokenValidationException.class, () -> jwtService.validateToken(modifiedToken));
    }
}