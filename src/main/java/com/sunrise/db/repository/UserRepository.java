package com.sunrise.db.repository;

import com.sunrise.db.result.UserProfileResult;
import com.sunrise.db.result.UserSecurityResult;
import com.sunrise.db.entity.User;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ========== ОБНОВЛЕНИЯ ==========

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLogin = :lastLogin, u.updatedAt = :lastLogin WHERE u.username = :username")
    void updateLastLogin(@Param("username") String username, @Param("lastLogin") Instant lastLogin);

    @Modifying
    @Transactional
    @Query("UPDATE User SET username = :username, name = :name, profileUpdatedAt = :updatedAt, updatedAt = :updatedAt WHERE id = :userId")
    int updateProfile(@Param("userId") long userId, @Param("username") String username, @Param("name") String name, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query(value = "UPDATE User SET email = :email, jwtVersion = jwtVersion + 1, updatedAt = :updatedAt WHERE id = :userId")
    int updateUserEmail(@Param("userId") long userId, @Param("email") String email, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE User SET hashPassword = :password, jwtVersion = jwtVersion + 1, updatedAt = :updatedAt WHERE id = :userId")
    int updateUserPassword(@Param("userId") long userId, @Param("password") String password, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE User SET isEnabled = true, jwtVersion = jwtVersion + 1, profileUpdatedAt = :updatedAt, updatedAt = :updatedAt WHERE id = :userId")
    int enableUser(@Param("userId") long userId, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE User SET isEnabled = false, jwtVersion = jwtVersion + 1, profileUpdatedAt = :updatedAt, updatedAt = :updatedAt WHERE id = :userId")
    int disableUser(@Param("userId") long userId, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE User SET isDeleted = true, jwtVersion = jwtVersion + 1, profileUpdatedAt = :updatedAt, updatedAt = :updatedAt WHERE id = :userId")
    int deleteUser(@Param("userId") long userId, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE User SET isDeleted = false, jwtVersion = jwtVersion + 1, profileUpdatedAt = :updatedAt, updatedAt = :updatedAt WHERE id = :userId")
    int restoreUser(@Param("userId") long userId, @Param("updatedAt") Instant updatedAt);


    // ========== ПОИСК ==========

    @Query("""
            SELECT
                u.id as id,
                u.username as username,
                u.name as name,
                u.profileUpdatedAt as profileUpdatedAt,
                u.createdAt as createdAt,
                u.isEnabled as isEnabled,
                u.deletedAt as deletedAt,
                u.isDeleted as isDeleted
            FROM User u
            WHERE u.id = :userId""")
    Optional<UserProfileResult> getUserProfile(@Param("userId") long userId);

    @Query("""
        SELECT
            u.id as id,
            u.username as username,
            u.name as name,
            u.profileUpdatedAt as profileUpdatedAt,
            u.createdAt as createdAt,
            u.isEnabled as isEnabled,
            u.deletedAt as deletedAt,
            u.isDeleted as isDeleted
        FROM User u
        WHERE u.id IN :userIds""")
    List<UserProfileResult> getUserProfileByIds(@Param("userIds") List<Long> userIds);

    @Query("""
            SELECT
                u.id as id,
                u.username as username,
                u.name as name,
                u.profileUpdatedAt as profileUpdatedAt,
                u.createdAt as createdAt,
                u.isEnabled as isEnabled,
                u.deletedAt as deletedAt,
                u.isDeleted as isDeleted
            FROM User u
            WHERE u.isDeleted = false AND u.id IN :userIds""")
    List<UserProfileResult> getActiveUserProfileByIds(@Param("userIds") List<Long> userIds);

    @Query("""
            SELECT
                u.id as id,
                u.email as email,
                u.hashPassword as hashPassword,
                u.jwtVersion as jwtVersion,
                u.isEnabled as isEnabled,
                u.deletedAt as deletedAt,
                u.isDeleted as isDeleted
            FROM User u
            WHERE u.id = :userId""")
    Optional<UserSecurityResult> getUserSecurity(@Param("userId") long userId);

    @Query("""
            SELECT
                u.id as id,
                u.email as email,
                u.hashPassword as hashPassword,
                u.jwtVersion as jwtVersion,
                u.isEnabled as isEnabled,
                u.deletedAt as deletedAt,
                u.isDeleted as isDeleted
            FROM User u
            WHERE u.username = :username""")
    Optional<UserSecurityResult> getUserSecurityByUsername(@Param("username") String username);

    @Query("""
            SELECT
                u.id as id,
                u.email as email,
                u.hashPassword as hashPassword,
                u.jwtVersion as jwtVersion,
                u.isEnabled as isEnabled,
                u.deletedAt as deletedAt,
                u.isDeleted as isDeleted
            FROM User u
            WHERE u.email = :email""")
    Optional<UserSecurityResult> getUserSecurityByEmail(@Param("email") String email);


    // ========== ПОИСК И ФИЛЬТРАЦИЯ С ПАГИНАЦИЕЙ ==========

    @Query("""
           SELECT
               u.id as id,
               u.username as username,
               u.name as name,
               u.profileUpdatedAt as profileUpdatedAt,
               u.createdAt as createdAt,
               u.isEnabled as isEnabled,
               u.deletedAt as deletedAt,
               u.isDeleted as isDeleted
           FROM User u
           WHERE u.isDeleted = false AND u.isEnabled = true
               AND (:filter = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :filter, '%'))
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :filter, '%')))
               AND (:cursor IS NULL OR u.id < :cursor)
           ORDER BY u.id DESC
           """)
    List<UserProfileResult> getActiveUsersPage(@Param("filter") String filter, @Param("cursor") Long cursor, Pageable pageable);
}
