package com.powsybl.afs.ws.server.sb.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.CompressionCodecs;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.powsybl.afs.ws.server.utils.KeyGenerator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;

@Component
public class JwtService {
    private static final String ISSUER = "com.powsybl.afs.ws.server.sb.jwt";
    public static final String USERNAME = "username";

	@Autowired
    private KeyGenerator keyGenerator;

    public String tokenFor(String login, long tokenValidity)  throws IOException, URISyntaxException {
        Key key = keyGenerator.generateKey();
        ZonedDateTime now = ZonedDateTime.now();
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, key)
                .compressWith(CompressionCodecs.DEFLATE)
                .setSubject(login)
                .setIssuer(ISSUER)
                .setIssuedAt(Date.from(now.toInstant()))
                .setExpiration(Date.from(now.plusMinutes(tokenValidity).toInstant()))
                .compact();
    }
    public String verify(String token) throws IOException, URISyntaxException {
    	Key key = keyGenerator.generateKey();
        Jws<Claims> claims = Jwts.parser().setSigningKey(key).parseClaimsJws(token);
        
        return claims.getBody().getSubject().toString();
    }
}