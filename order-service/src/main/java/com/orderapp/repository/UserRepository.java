package com.orderapp.repository;

import com.orderapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByStatus(User.UserStatus status);

    @Query("SELECT u FROM User u WHERE u.firstName LIKE %:name% OR u.lastName LIKE %:name%")
    Page<User> findByNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "(:email IS NULL OR u.email LIKE %:email%) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:name IS NULL OR u.firstName LIKE %:name% OR u.lastName LIKE %:name%)")
    Page<User> findUsersWithFilters(@Param("email") String email,
                                   @Param("status") User.UserStatus status,
                                   @Param("name") String name,
                                   Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") User.UserStatus status);
}
