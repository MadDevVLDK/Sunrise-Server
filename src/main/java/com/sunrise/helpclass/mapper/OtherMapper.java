package com.sunrise.helpclass.mapper;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.sunrise.core.creation.CreateLoginHistoryDTO;
import com.sunrise.db.entity.LoginHistory;
import com.sunrise.db.result.MessageReadStatusResult;
import com.sunrise.orchestrator.result.MessageReadStatusDTO;

public class OtherMapper {
    

    // ========== MESSAGE READ STATUS ==========

    public static List<MessageReadStatusDTO> toMessageReadDTOs(Collection<MessageReadStatusResult> items) {
        if (items == null) return null;

        List<MessageReadStatusDTO> resultMap = new LinkedList<>();
        for (MessageReadStatusResult item : items) {
            resultMap.add(new MessageReadStatusDTO(item.getUserId(), item.getReadAt()));
        }
        return resultMap;
    }


    // ========== LOGIN HISTORY ==========

    public static LoginHistory toLoginHistoryEntity(CreateLoginHistoryDTO loginHistory) {
        if (loginHistory == null) return null;

        return new LoginHistory(
            loginHistory.getId(),
            loginHistory.getUserId(),
            loginHistory.getIpAddress(),
            loginHistory.getDeviceInfo(),
            loginHistory.getLoginAt()
        );
    }
}
