package com.sunrise.db.service;

import com.sunrise.db.entity.LoginHistory;
import com.sunrise.db.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginHistoryDbService {

    private final LoginHistoryRepository loginHistoryRepository;

    @Transactional
    public void save(LoginHistory loginHistory) {
        log.debug("[🗄️] 📝 Saving login history: userId={}, ip={}, device={}",  
            loginHistory.getUserId(), loginHistory.getIpAddress(), loginHistory.getDeviceInfo());
        loginHistoryRepository.save(loginHistory);
    }
}