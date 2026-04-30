package com.sunrise.orchestrator.service;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import com.sunrise.cache.entity.Cache;
import com.sunrise.cache.event.CacheEvent;
import com.sunrise.cache.service.VerificationTokenCacheService;
import com.sunrise.core.creation.CreateDto;
import com.sunrise.db.entity.VerificationToken;
import com.sunrise.db.service.VerificationTokenDbService;
import com.sunrise.helpclass.mapper.VerificationTokenMapper;
import com.sunrise.orchestrator.result.Dto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenOrchestrator {

    private final ApplicationEventPublisher eventPublisher;
    
    private final VerificationTokenCacheService cacheVerificationTokenService;
    private final VerificationTokenDbService dbVerificationTokenService;


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы

    @Transactional(propagation = REQUIRES_NEW)
    public void saveVerificationToken(@NonNull CreateDto.VerificationToken verificationToken) {
        // синхронно в бд
        dbVerificationTokenService.save(
            VerificationTokenMapper.toEntity(verificationToken)
        );

        // публикуем для обновления кеша после коммита
        eventPublisher.publishEvent(new CacheEvent.VerificationTokenCreated(
            VerificationTokenMapper.toCache(verificationToken)
        ));
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void deleteVerificationToken(@NonNull String token) {
        // синхронно в бд
        dbVerificationTokenService.deleteByToken(token);

        // публикуем для обновления кеша после коммита
        eventPublisher.publishEvent(
            new CacheEvent.VerificationTokenDeleted((token))
        );
    }


    // Вспомогательные методы

    public Optional<Dto.VerificationToken> getVerificationToken(@NonNull String token) {
        // пробуем кеш
        Optional<Cache.VerificationToken> optToken = cacheVerificationTokenService.get(token);
        if(optToken.isPresent())
            return optToken.map(VerificationTokenMapper::toDTO);

        // грузим из бд
        Optional<VerificationToken> optTokenDB = dbVerificationTokenService.getByToken(token);
        optTokenDB.ifPresent(verificationTokenDB -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.VerificationTokenCreated(
                VerificationTokenMapper.toCache(verificationTokenDB)
            ));
        });
        return optTokenDB.map(VerificationTokenMapper::toDTO);
    }


    // ========== SUB METHODS ==========
    

    @Scheduled(initialDelay = 10_000, fixedRate = 86_400_000) // Каждые 24 часа
    public void cleanupExpiredTokens() {
        try {
            int numDeletedTokens = dbVerificationTokenService.cleanupExpired();
            log.info("[🔧] ✅ Expired tokens cleanup completed. Deleted --> {} tokens", numDeletedTokens);
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error during token cleanup: {}", e.getMessage());
        }
    }
}