package com.erp.backend.security;

import com.erp.backend.entity.User;
import com.erp.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. Trích xuất chuỗi Token từ Header "Authorization: Bearer <token>"
            String jwt = parseJwt(request);

            // 2. Nếu có token và token hợp lệ
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUsernameFromJwtToken(jwt);

                // 3. Lấy thông tin user và quyền từ Database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 4. Reloading current account state invalidates existing JWTs after an admin lock.
                if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {

                    // 5. S1-04: Thu hồi phiên cũ nếu mật khẩu đã bị đổi sau khi token được cấp.
                    //    Token issuedAt phải SAU thời điểm đổi mật khẩu gần nhất.
                    boolean tokenIsValid = true;
                    Date issuedAt = jwtUtils.getIssuedAtFromToken(jwt);
                    if (issuedAt != null) {
                        User dbUser = userRepository.findByUsername(username).orElse(null);
                        if (dbUser != null && dbUser.getPasswordChangedAt() != null) {
                            long changedAtMs = dbUser.getPasswordChangedAt()
                                    .toInstant(ZoneOffset.UTC).toEpochMilli();
                            if (issuedAt.getTime() < changedAtMs) {
                                // Token được cấp TRƯỚC khi đổi mật khẩu -> thu hồi
                                logger.info("Token của '{}' bị thu hồi do đổi mật khẩu sau khi phát hành.", username);
                                tokenIsValid = false;
                            }
                        }
                    }

                    if (tokenIsValid) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // Lưu vào SecurityContext để các Controller kiểm tra quyền (@PreAuthorize)
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Không thể xác thực người dùng: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    // Hàm phụ trợ cắt bỏ chữ "Bearer " để lấy chuỗi token nguyên bản
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
