package com.erp.backend.repository;

import com.erp.backend.entity.Role;
import com.erp.backend.entity.RoleName;
import com.erp.backend.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * S1-08: Điều kiện tìm kiếm tài khoản.
 * - keyword: tìm theo họ tên, tài khoản, số điện thoại
 * - role: lọc theo vai trò
 * - status: lọc theo trạng thái (ACTIVE / LOCKED)
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> search(String keyword, RoleName role, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String kw = keyword.trim().toLowerCase();
                String like = "%" + kw + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("username")), like),
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
