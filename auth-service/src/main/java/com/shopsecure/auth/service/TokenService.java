package com.shopsecure.auth.service;

import com.shopsecure.auth.config.AppProperties;
import com.shopsecure.auth.domain.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Creates signed JWT access tokens.
 * <p>
 * A JWT = base64(header).base64(claims).signature. Claims are only ENCODED, not encrypted:
 * anyone can read them (try jwt.io), so never put secrets in them. The signature only proves
 * the token was issued by us and was not modified.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final AppProperties props;

    public TokenService(JwtEncoder encoder, AppProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.jwt().issuer())                       // iss: who issued it
                .subject(user.getUsername())                        // sub: who it is about
                .issuedAt(now)                                      // iat
                .expiresAt(now.plus(props.jwt().accessTokenTtl()))  // exp: verified by every service
                .id(UUID.randomUUID().toString())                   // jti: unique id (useful for deny-lists)
                .claim("uid", user.getId())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().stream().map(Enum::name).sorted().toList())
                .claim("type", "user")
                .build();
        return encode(claims);
    }

    /** Token for machine-to-machine calls (client-credentials style). */
    public String createServiceToken(String clientId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.jwt().issuer())
                .subject(clientId)
                .issuedAt(now)
                .expiresAt(now.plus(props.jwt().serviceTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of("SERVICE"))
                .claim("type", "service")
                .build();
        return encode(claims);
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
