package mx.trazable.gps.security;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import mx.trazable.gps.core.config.CoreConfig;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class TokenUtils {

    private final CoreConfig coreConfig;

    private SecretKey privateKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                coreConfig.getAccessToken().getPrivateKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey publicKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                coreConfig.getAccessToken().getPublicKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /* ================== CREACIÓN ================== */
    public String createToken(Integer supplierGpsId, Integer carrierId, Integer orgId) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(supplierGpsId.toString())
                .claim("supplierGpsId", supplierGpsId)
                .claim("carrierId", carrierId)
                .claim("orgId", orgId)
                .issuedAt(Date.from(now))
                .signWith(privateKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /* ================== PARSE ================== */
    public Claims parse(String jwt) {
        return Jwts.parser()
                .verifyWith(privateKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }


    public String createTokenPublic(Integer userId, Integer orgId,
                                    long amount, ChronoUnit unit) {

        Instant now = Instant.now();
        Instant expiration = now.atZone(ZoneOffset.UTC)
                .plus(amount, unit)
                .toInstant();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId)
                .claim("orgId", orgId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(publicKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /* ================== PARSE ================== */
    public Claims parsePublic(String jwt) {
        return Jwts.parser()
                .verifyWith(publicKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }


    /* ================== AUTH ================== */
    public Object getAuthorization(String jwt) {
        try {
            Claims c = parse(jwt);

            Integer userId = c.get("userId", Integer.class);
            Integer orgId = c.get("orgId", Integer.class);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId, null, Collections.emptyList()
                    );

            auth.setDetails(orgId);
            return auth;

        } catch (ExpiredJwtException e) {
            return "Token expirado";
        } catch (JwtException e) {
            return "Token inválido";
        }
    }

    public Integer getOrgIdFromToken(String token) {
        Claims c = parse(cleanBearer(token));
        Integer orgId = c.get("orgId", Integer.class);

        if (orgId == null) {
            throw new JwtException("Token no contiene orgId");
        }
        return orgId;
    }

    public Integer getSupplierGpsIdFromToken(String token) {
        Claims c = parse(cleanBearer(token));
        Integer supplierGpsId = c.get("supplierGpsId", Integer.class);

        if (supplierGpsId == null) {
            throw new JwtException("Token no contiene supplierGpsId");
        }
        return supplierGpsId;
    }

    public Integer getCarrierIdFromToken(String token) {
        Claims c = parse(cleanBearer(token));
        Integer carrierId = c.get("carrierId", Integer.class);

        if (carrierId == null) {
            throw new JwtException("Token no contiene carrierId");
        }
        return carrierId;
    }

    private String cleanBearer(String token) {
        if (token == null) throw new JwtException("Token null");
        token = token.trim();
        if (token.startsWith("Bearer ")) token = token.substring(7);
        return token;
    }
}
