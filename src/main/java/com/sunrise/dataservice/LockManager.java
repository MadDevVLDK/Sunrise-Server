package com.sunrise.dataservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class LockManager {

    @Value("${app.locks.timeout}")
    private int LOCK_TIMEOUT_SECONDS;
    @Value("${app.locks.clean-up-expiration}")
    private int LOCK_EXPIRATION_MILLIS;

    private class MyLock {
        private final ReentrantLock lock = new ReentrantLock();
        volatile long lastAccess;

        public boolean tryLock() throws InterruptedException {
            if (lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)){
                lastAccess = System.currentTimeMillis();
                return true;
            }
            return false;
        }
        public void unLock() {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                lastAccess = System.currentTimeMillis();
            }
        }
    }


    // ========== LOCK MAPS ==========

    private final Map<String, MyLock> chatCreationLocks = new ConcurrentHashMap<>();
    private final Map<String, MyLock> chatLeaveLocks = new ConcurrentHashMap<>();
    private final Map<String, MyLock> usernameLocks = new ConcurrentHashMap<>();
    private final Map<String, MyLock> emailLocks = new ConcurrentHashMap<>();


    // ========== ABSTRACT LOCK FUNCTIONS ==========

    private <K> boolean tryLock(Map<K, MyLock> lockMap, K key) {
        MyLock entry = lockMap.computeIfAbsent(key, k -> new MyLock());
        try {
            if (entry.tryLock()) {
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
    private <K> void unLock(Map<K, MyLock> lockMap, K key) {
        if (lockMap.get(key) instanceof MyLock lock) {
            lock.unLock();
        }
    }


    // ========== REGISTRATION LOCKS ==========

    public boolean tryLockRegistration(String username, String email) {
        String usernameKey = "username:" + username;
        String emailKey = "email:" + email.toLowerCase().trim();

        if (!tryLockUsername(usernameKey)) return false;
        if (!tryLock(emailLocks, emailKey)) {
            unLockUsername(username); // откат
            return false;
        }
        return true;
    }
    public void unLockRegistration(String username, String email) {
        String usernameKey = "username:" + username;
        String emailKey = "email:" + email.toLowerCase().trim();
        unLockUsername(usernameKey);
        unLock(emailLocks, emailKey);
    }


    // ========== REGISTRATION LOCKS ==========

    public boolean tryLockUsername(String username) {
        String key = "username:" + username;
        return tryLock(usernameLocks, key);
    }
    public void unLockUsername(String username) {
        String key = "username:" + username;
        unLock(usernameLocks, key);
    }


    // ========== CHAT DATA LOCKS ==========

    public boolean tryLockPersonalChatCreation(long user1, long user2) {
        String key = "user1:" + Math.max(user1, user2) + ":user2:" + Math.min(user1, user2);
        return tryLock(chatCreationLocks, key);
    }
    public void unLockPersonalChatCreation(long user1, long user2) {
        String key = "user1:" + Math.max(user1, user2) + ":user2:" + Math.min(user1, user2);
        unLock(chatCreationLocks, key);
    }

    public boolean tryLockLeaveChatOperation(long chatId) {
        String key = "chatId:" + chatId;
        return tryLock(chatLeaveLocks, key);
    }
    public void unLockLeaveChatOperation(long chatId) {
        String key = "chatId:" + chatId;
        unLock(chatLeaveLocks, key);
    }


    // ========== STATS ==========

    public Map<String, Object> getLockStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("chatCreationLocks.size", chatCreationLocks.size());
        stats.put("chatLeaveLocks.size", chatLeaveLocks.size());
        stats.put("usernameLocks.size", usernameLocks.size());
        stats.put("emailLocks.size", emailLocks.size());
        return stats;
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 20_000) // 1000 * 60 * 60
    public void printLockStats() {
        Map<String, Object> stats = getLockStats();
        log.info("---------------------------");
        log.info("📊 Lock Statistics:");
        log.info("   ├─ Chat Creation: size={}", stats.get("chatCreationLocks.size"));
        log.info("   ├─ Chat Leave: size={}", stats.get("chatLeaveLocks.size"));
        log.info("   ├─ Username: size={}", stats.get("usernameLocks.size"));
        log.info("   └─ Email: size={}", stats.get("emailLocks.size"));
        log.info("---------------------------");
    }


    // ========== CLEANUP ==========

    @Scheduled(fixedDelayString = "${app.locks.clean-up-schedule}", initialDelay = 30_000)
    public void cleanupOldLocks() {
        long now = System.currentTimeMillis();
        cleanupLockMap(chatCreationLocks, now);
        cleanupLockMap(chatLeaveLocks, now);
        cleanupLockMap(usernameLocks, now);
        cleanupLockMap(emailLocks, now);
        log.info("[🔧] ✅ Locks cleanup successful completed");
    }
    private void cleanupLockMap(Map<?, MyLock> lockMap, long now) {
        lockMap.entrySet().removeIf(entry -> {
            MyLock lock = entry.getValue();
            return !lock.lock.isLocked() && now - lock.lastAccess > LOCK_EXPIRATION_MILLIS;
        });
    }
}