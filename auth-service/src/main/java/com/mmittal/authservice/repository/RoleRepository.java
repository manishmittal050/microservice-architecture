package com.mmittal.authservice.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mmittal.authservice.entities.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
}
