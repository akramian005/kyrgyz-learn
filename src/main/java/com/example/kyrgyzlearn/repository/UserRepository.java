package com.example.kyrgyzlearn.repository;

//package com.example.demo2.identity.repository;

import com.example.kyrgyzlearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}