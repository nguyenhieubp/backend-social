package com.example.manager.repositories;

import com.example.manager.models.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface RoleRepository extends JpaRepository<RoleEntity,String> {
    Optional<RoleEntity> findByRoleName(String roleName);
}
