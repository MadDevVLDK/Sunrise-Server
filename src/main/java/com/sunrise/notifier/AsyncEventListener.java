package com.sunrise.notifier;

import com.sunrise.dataservice.DataOrchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class AsyncEventListener {

    private final DataOrchestrator dataOrchestrator;

    @Async("dbExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaveUserLoginHistory(AsyncEvent.SaveUserLoginHistory event) {
        try {
            dataOrchestrator.saveLoginHistory(event.username(), event.loginHistory());
        } catch (Exception e) {
            log.error("[🎢] ❌ Failed to ");
        }
    }

    @Async("dbExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaveVerificationToken(AsyncEvent.SaveVerificationToken event) {
        try {
            dataOrchestrator.saveVerificationToken(event.verificationToken());
        } catch (Exception e) {
            log.error("[🎢] ❌ Failed to ");
        }
    }

    @Async("dbExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleteVerificationToken(AsyncEvent.DeleteVerificationToken event) {
        try {
            dataOrchestrator.deleteVerificationToken(event.token());
        } catch (Exception e) {
            log.error("[🎢] ❌ Failed to ");
        }
    }
}
