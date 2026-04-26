package com.sunrise.db.transaction;

import com.sunrise.db.DBService;
import com.sunrise.db.entity.LoginHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginHistoryDbService {

    private final DBService dbService;

    @Transactional
    public void saveLoginHistory(LoginHistory loginHistory) {
        dbService.saveLoginHistory(loginHistory);
    }
}