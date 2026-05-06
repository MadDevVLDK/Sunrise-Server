package com.sunrise.orchestrator.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sunrise.orchestrator.service.UserOrchestrator;
import com.sunrise.orchestrator.service.VerificationTokenOrchestrator;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncEventListener {

    private final UserOrchestrator userOrchestrator;
    private final VerificationTokenOrchestrator verificationTokenOrchestrator;


    @Async("dbExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaveUserLoginHistory(AsyncEvent.SaveUserLoginHistory event) {
        try {
            userOrchestrator.saveLoginHistory(event.username(), event.loginHistory());
        } catch (Exception e) {
            log.error("[🎢] ❌ Failed to ");
        }
    }

    @Async("dbExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaveVerificationToken(AsyncEvent.SaveVerificationToken event) {
        try {
            verificationTokenOrchestrator.saveVerificationToken(event.verificationToken());
        } catch (Exception e) {
            log.error("[🎢] ❌ Failed to ");
        }
    }

    @Async("dbExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleteVerificationToken(AsyncEvent.DeleteVerificationToken event) {
        try {
            verificationTokenOrchestrator.deleteVerificationToken(event.token());
        } catch (Exception e) {
            log.error("[🎢] ❌ Failed to ");
        }
    }
}
