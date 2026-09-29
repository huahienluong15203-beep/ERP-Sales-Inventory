package com.erp.backend.repository;

import com.erp.backend.entity.Role;
import com.erp.backend.entity.RoleName;
import com.erp.backend.entity.User;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * S1-08: Điều kiện tìm kiếm tài khoản.
 * - keyword: tìm theo họ tên (hỗ trợ cả tiếng Việt có dấu và không dấu), tài khoản, số điện thoại
 * - role: lọc theo vai trò
 * - status: lọc theo trạng thái (ACTIVE / LOCKED)
 */
public final class UserSpecifications {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private UserSpecifications() {
    }

    /**
     * Bỏ dấu tiếng Việt: "Nguyễn" -> "nguyen", "Đạt" -> "dat"
     */
    public static String removeAccents(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase()
                .trim();
    }

    public static Specification<User> search(String keyword, RoleName role, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String rawKw = keyword.trim().toLowerCase();
                String rawLike = "%" + rawKw + "%";
                String unaccentKw = removeAccents(keyword);
                String unaccentLike = "%" + unaccentKw + "%";

                // Sử dụng hàm unaccent trong PostgreSQL để so khớp họ tên không dấu
                Expression<String> unaccentFullName = cb.function("unaccent", String.class, cb.lower(root.get("fullName")));

                predicates.add(cb.or(
                        cb.like(unaccentFullName, unaccentLike),
                        cb.like(cb.lower(root.get("fullName")), rawLike),
                        cb.like(cb.lower(root.get("username")), rawLike),
                        cb.like(root.get("phone"), "%" + keyword.trim() + "%")));
            }

            if (role != null) {
                Join<User, Role> roles = root.join("roles");
                predicates.add(cb.equal(roles.get("name"), role));
                if (query != null) {
                    query.distinct(true); // 1 user nhiều vai trò không bị lặp dòng
                }
            }

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
