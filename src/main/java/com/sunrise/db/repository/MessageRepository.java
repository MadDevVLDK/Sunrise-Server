package com.sunrise.db.repository;

import com.sunrise.db.result.MessageReadStatusResult;
import com.sunrise.db.result.UserMessageResult;
import com.sunrise.db.entity.Message;
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
import java.util.Set;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // ========== ОПЕРАЦИИ С СООБЩЕНИЯМИ ==========

    @Query("SELECT m FROM Message m WHERE m.id = :messageId AND m.chatId = :chatId")
    Optional<Message> get(@Param("chatId") long chatId, @Param("messageId") long messageId);

    @Query("SELECT m.id FROM Message m WHERE m.chatId = :chatId ORDER BY m.id DESC")
    List<Long> findLastMessageIds(@Param("chatId") long chatId, Pageable pageable);

    @Query("""
           SELECT
               m.id AS id,
               m.chatId AS chatId,
               m.senderId AS senderId,
               u.profileUpdatedAt AS profileUpdatedAt,
               member.updatedAt AS memberUpdatedAt,
               m.messageType AS messageType,
               m.text AS text,
               m.readCount AS readCount,
               m.sentAt AS sentAt,
               m.updatedAt as updatedAt,
               m.deletedAt as deletedAt,
               m.isDeleted AS isDeleted
           FROM Message m
           INNER JOIN User u
               ON u.id = m.senderId
           INNER JOIN ChatMember member
               ON member.id.userId = m.senderId
               AND member.id.chatId = m.chatId
           WHERE m.id = :messageId AND m.chatId = :chatId
           """)
    Optional<UserMessageResult> getUserMessage(@Param("chatId") long chatId, @Param("userId") long userId, @Param("messageId") long messageId);

    @Query("""
           SELECT
               m.id AS id,
               m.chatId AS chatId,
               m.senderId AS senderId,
               u.profileUpdatedAt AS profileUpdatedAt,
               member.updatedAt AS memberUpdatedAt,
               m.messageType AS messageType,
               m.text AS text,
               m.readCount AS readCount,
               m.sentAt AS sentAt,
               m.updatedAt as updatedAt,
               m.deletedAt as deletedAt,
               m.isDeleted AS isDeleted
           FROM Message m
           INNER JOIN User u
               ON u.id = m.senderId
           INNER JOIN ChatMember member
               ON member.id.userId = m.senderId
               AND member.id.chatId = m.chatId
           WHERE m.id IN :messageIds AND m.chatId = :chatId
           """)
    List<UserMessageResult> getUserMessageBatch(@Param("chatId") long chatId, @Param("userId") long userId, @Param("messageIds") Set<Long> messageIds);

    @Query("""
           SELECT
               m.id AS id,
               m.chatId AS chatId,
               m.senderId AS senderId,
               u.profileUpdatedAt AS profileUpdatedAt,
               member.updatedAt AS memberUpdatedAt,
               m.messageType AS messageType,
               m.text AS text,
               m.readCount AS readCount,
               m.sentAt AS sentAt,
               m.updatedAt as updatedAt,
               m.deletedAt as deletedAt,
               m.isDeleted AS isDeleted
           FROM Message m
           INNER JOIN User u
               ON u.id = m.senderId
           INNER JOIN ChatMember member
               ON member.id.userId = m.senderId
               AND member.id.chatId = m.chatId
           WHERE m.chatId = :chatId
           ORDER BY m.id DESC
           """)
    List<UserMessageResult> getMessagePageFirst(@Param("chatId") long chatId, @Param("userId") long userId, Pageable pageable);

    @Query("""
           SELECT
               m.id AS id,
               m.chatId AS chatId,
               m.senderId AS senderId,
               u.profileUpdatedAt AS profileUpdatedAt,
               member.updatedAt AS memberUpdatedAt,
               m.messageType AS messageType,
               m.text AS text,
               m.readCount AS readCount,
               m.sentAt AS sentAt,
               m.updatedAt as updatedAt,
               m.deletedAt as deletedAt,
               m.isDeleted AS isDeleted
           FROM Message m
           INNER JOIN User u
               ON u.id = m.senderId
           INNER JOIN ChatMember member
               ON member.id.userId = m.senderId
               AND member.id.chatId = m.chatId
           WHERE m.chatId = :chatId AND m.id < :cursor
           ORDER BY m.id DESC
           """)
    List<UserMessageResult> getPageBefore(@Param("chatId") long chatId, @Param("userId") long userId, @Param("cursor") long cursor, Pageable pageable);

    @Query("""
           SELECT
               m.id AS id,
               m.chatId AS chatId,
               m.senderId AS senderId,
               u.profileUpdatedAt AS profileUpdatedAt,
               member.updatedAt AS memberUpdatedAt,
               m.messageType AS messageType,
               m.text AS text,
               m.readCount AS readCount,
               m.sentAt AS sentAt,
               m.updatedAt as updatedAt,
               m.deletedAt as deletedAt,
               m.isDeleted AS isDeleted
           FROM Message m
           INNER JOIN User u
               ON u.id = m.senderId
           INNER JOIN ChatMember member
               ON member.id.userId = m.senderId
               AND member.id.chatId = m.chatId
           WHERE m.chatId = :chatId AND m.id > :cursor
           ORDER BY m.id ASC
           """)
    List<UserMessageResult> getPageAfter(@Param("chatId") long chatId, @Param("userId") long userId, @Param("cursor") long cursor, Pageable pageable);


    @Transactional
    @Query(value = "SELECT * FROM mark_messages_up_to_read(:chatId, :userId, :messageId, :readAt)", nativeQuery = true)
    List<Long> markMessagesUpToRead(@Param("chatId") long chatId, @Param("userId") long userId, @Param("messageId") long messageId, @Param("readAt") Instant readAt);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.text = :newText, m.updatedAt = :updatedAt WHERE m.id = :messageId")
    int update(@Param("messageId") long messageId, @Param("newText") String text, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isDeleted = false, m.deletedAt = null, m.updatedAt = :updatedAt WHERE m.id = :messageId")
    int restore(@Param("messageId") long messageId, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isDeleted = true, m.deletedAt = :updatedAt, m.updatedAt = :updatedAt WHERE m.id = :messageId")
    int delete(@Param("messageId") long messageId, @Param("updatedAt") Instant updatedAt);

    @Query("SELECT mrs.id.userId as userId, mrs.readAt as readAt FROM MessageReadStatus mrs WHERE mrs.id.messageId = :messageId ORDER BY mrs.readAt")
    List<MessageReadStatusResult> getMessageReaders(@Param("messageId") long messageId);
}