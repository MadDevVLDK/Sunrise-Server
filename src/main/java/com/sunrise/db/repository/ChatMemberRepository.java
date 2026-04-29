package com.sunrise.db.repository;

import com.sunrise.db.entity.ChatMember;
import com.sunrise.db.entity.ChatMemberId;
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
public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {

    @Transactional
    @Query(value = "SELECT add_or_restore_chat_member(:chatId, :userId, :isAdmin, :joinedAt, TRUE)", nativeQuery = true)
    boolean saveOrRestore(@Param("chatId") long chatId, @Param("userId") long userId, @Param("isAdmin") boolean isAdmin, @Param("joinedAt") Instant joinedAt);

    @Transactional
    @Query(value = "SELECT add_or_restore_chat_members_batch(:chatId, :userIds, :joinedAt)", nativeQuery = true)
    Long[] saveOrRestoreBatch(@Param("chatId") long chatId, @Param("userIds") Long[] userIds, @Param("joinedAt") Instant joinedAt);

    @Modifying
    @Transactional
    @Query("""
           UPDATE ChatMember cm
           SET cm.tag = :tag, cm.updatedAt = :updatedAt
           WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId""")
    int updateProfile(@Param("chatId") long chatId, @Param("userId") long userId, @Param("tag") String tag, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query("""
           UPDATE ChatMember cm
           SET cm.isAdmin = :isAdmin, cm.updatedAt = :updatedAt
           WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId""")
    int updateAdminRights(@Param("chatId") long chatId, @Param("userId") long userId, @Param("isAdmin") boolean isAdmin, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query("""
           UPDATE ChatMember cm
           SET cm.isPinned = :isPinned, cm.settingsUpdatedAt = :updatedAt, cm.updatedAt = :updatedAt
           WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId""")
    int updateSettings(@Param("chatId") long chatId, @Param("userId") long userId, @Param("isPinned") boolean isPinned, @Param("updatedAt") Instant updatedAt);

    @Transactional
    @Query(value = "SELECT remove_chat_member(:chatId, :userId, :updatedAt)", nativeQuery = true)
    boolean remove(@Param("chatId") long chatId, @Param("userId") long userId, @Param("updatedAt") Instant updatedAt); // удален или нет

    @Query("SELECT cm FROM ChatMember cm " +
           "WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId AND cm.isDeleted = false")
    Optional<ChatMember> getActive(@Param("chatId") long chatId, @Param("userId") long userId);

    @Query("SELECT cm FROM ChatMember cm " +
           "WHERE cm.id.chatId = :chatId AND cm.id.userId IN :userIds")
    List<ChatMember> getBatch(@Param("chatId") long chatId, @Param("userIds") List<Long> userIds);

    @Query("SELECT cm FROM ChatMember cm " +
           "WHERE cm.id.chatId = :chatId AND cm.id.userId IN :userIds AND cm.isDeleted = false")
    List<ChatMember> getActiveBatch(@Param("chatId") long chatId, @Param("userIds") List<Long> userIds);

    @Query("""
           SELECT cm.id.userId FROM ChatMember cm
           WHERE cm.id.chatId = :chatId AND cm.isDeleted = false
           AND (:cursor IS NULL OR cm.id.userId < :cursor)
           ORDER BY cm.id.userId DESC""")
    List<Long> getIdsPage(@Param("chatId") long chatId, @Param("cursor") Long cursor, Pageable pageable);
}
