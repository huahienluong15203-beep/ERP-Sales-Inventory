package com.erp.backend.repository;

import com.erp.backend.entity.Role;
import com.erp.backend.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);

    List<Role> findByNameIn(Collection<RoleName> names);
}
