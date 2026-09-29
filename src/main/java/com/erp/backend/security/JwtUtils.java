package com.erp.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${erp.app.jwtSecret}")
    private String jwtSecret;

    @Value("${erp.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    // Lấy khoá bí mật từ chuỗi cấu hình để ký Token
    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // 1. Tạo chuỗi Token JWT khi đăng nhập thành công (S1-01)
    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. Trích xuất Username từ Token gửi lên (S1-02)
    public String getUsernameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 3. Kiểm tra Token có hợp lệ, còn hạn sử dụng không (S1-02)
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
            return true;
        } catch (MalformedJwtException e) {
            System.err.println("Token không đúng định dạng: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("Token đã hết hạn: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("Token không được hỗ trợ: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Chuỗi claims rỗng: " + e.getMessage());
        }
        return false;
    }
}
