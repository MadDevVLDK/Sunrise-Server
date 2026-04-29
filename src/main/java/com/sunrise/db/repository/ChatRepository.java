package com.sunrise.db.repository;

import com.sunrise.db.result.ChatMetaResult;
import com.sunrise.db.result.ChatProfileResult;
import com.sunrise.db.result.ChatStatsResult;
import com.sunrise.db.result.UserChatResult;
import com.sunrise.orchestrator.type.ChatType;
import com.sunrise.db.entity.Chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public interface ChatRepository extends JpaRepository<Chat, Long> {


    // ========== ОПЕРАЦИИ С ЧАТОМ ==========

    @Transactional
    @Query(value = "SELECT create_personal_chat_with_members(:chatId, :chatType, :user1Id, :user2Id, :createdAt)", nativeQuery = true)
    void savePersonalChatAndMembers(@Param("chatId") long chatId, @Param("chatType") String chatType,
                                    @Param("user1Id") long user1Id, @Param("user2Id") long user2Id,
                                    @Param("createdAt") Instant createdAt);


    @Transactional
    @Query(value = "SELECT create_group_chat_with_members(:chatId, :name, :description, :chatType, :memberIds, :creatorId, :createdAt)", nativeQuery = true)
    void saveGroupChatAndMembers(@Param("chatId") long chatId, @Param("name") String name, @Param("description") String description,
                                 @Param("chatType") String chatType, @Param("memberIds") Long[] memberIds,
                                 @Param("creatorId") long creatorId, @Param("createdAt") Instant createdAt);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.name = :name, c.description = :description, c.updatedAt = :updatedAt WHERE c.id = :chatId")
    int updateChatInfo(@Param("chatId") long chatId, @Param("name") String name, @Param("description") String description, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = false, c.deletedAt = null, c.updatedAt = :updatedAt WHERE c.id = :chatId")
    int restoreChat(@Param("chatId") long chatId, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = true, c.deletedAt = :updatedAt, c.updatedAt = :updatedAt WHERE c.id = :chatId")
    int deleteChat(@Param("chatId") long chatId, @Param("updatedAt") Instant updatedAt);


    // ========== ПОИСК ==========


    @Query("""
            SELECT
                c.id AS id,
                c.name AS name,
                c.description AS description,
                c.chatType AS chatType,
                c.opponentId AS opponentId,
                c.membersCount AS membersCount,
                c.updatedAt AS updatedAt,
                c.createdAt AS createdAt,
                c.createdBy AS createdBy,
                c.deletedAt AS deletedAt,
                c.isDeleted AS isDeleted
            FROM Chat c
            WHERE c.id = :chatId
            """)
    Optional<ChatProfileResult> getChat(@Param("chatId") long chatId);

    @Query("""
            SELECT
              c.id AS id,
              c.name AS name,
              c.description AS description,
              c.chatType AS chatType,
              c.opponentId AS opponentId,
              c.membersCount AS membersCount,
              c.updatedAt AS updatedAt,
              c.createdAt AS createdAt,
              c.createdBy AS createdBy,
              c.deletedAt AS deletedAt,
              c.isDeleted AS isDeleted
            FROM Chat c
            INNER JOIN ChatMember cm1 ON cm1.id.chatId = c.id AND cm1.id.userId = :userId1 AND cm1.isDeleted = false
            INNER JOIN ChatMember cm2 ON cm2.id.chatId = c.id AND cm2.id.userId = :userId2 AND cm2.isDeleted = false
            WHERE c.chatType = :chatType
           """)
    Optional<ChatProfileResult> getPersonalChat(@Param("userId1") long userId1, @Param("userId2") long userId2, @Param("chatType") ChatType chatType);

    @Query(value = 
        """
        SELECT
            c.id AS chatId,
            cm.is_pinned AS isPinned,
            (
                SELECT m.id
                FROM messages m
                WHERE m.chat_id = c.id
                ORDER BY m.id DESC
                LIMIT 1
            ) AS lastMsgId,
            COALESCE((
                SELECT COUNT(*)
                FROM messages m
                WHERE m.chat_id = c.id
                AND (ucrs.last_read_message_id IS NULL OR m.id > ucrs.last_read_message_id)
            ), 0) AS unreadCount,
            COALESCE(cs.current_seq, 0) AS seq
        FROM chats c
        JOIN chat_members cm 
            ON cm.chat_id = c.id AND cm.user_id = :userId AND cm.is_deleted = FALSE
        LEFT JOIN user_chat_read_state ucrs 
            ON ucrs.chat_id = c.id AND ucrs.user_id = :userId
        LEFT JOIN chat_seq cs
            ON cs.chat_id = c.id
        WHERE c.is_deleted = FALSE
        ORDER BY cm.is_pinned DESC, lastMsgId DESC NULLS LAST, c.id DESC
        """, nativeQuery = true)
    List<ChatMetaResult> getChatsMeta(@Param("userId") long userId);

    @Query(value = "SELECT * FROM get_chats_by_ids(:userId, :chatsIds)", nativeQuery = true)
    List<UserChatResult> getChatsByIds(@Param("userId") long userId, @Param("chatsIds") Long[] chatsIds);

    @Query(value = "SELECT * FROM get_chat_by_id(:chatId, :userId)", nativeQuery = true)
    Optional<UserChatResult> getUserChat(@Param("chatId") long chatId, @Param("userId") long userId);


    // ========== ДЕЙСТВИЯ С ИСТОРИЕЙ ЧАТОВ ==========


    // Статистика
    @Query(value = "SELECT * FROM get_chat_clear_stats(:chatId, :userId)", nativeQuery = true)
    ChatStatsResult getChatClearStats(@Param("chatId") long chatId, @Param("userId") long userId);
}