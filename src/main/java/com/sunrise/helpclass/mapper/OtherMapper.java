package com.sunrise.helpclass.mapper;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.sunrise.core.creation.CreateDto;
import com.sunrise.db.entity.LoginHistory;
import com.sunrise.db.result.MessageReadStatusResult;
import com.sunrise.orchestrator.result.Dto;

public class OtherMapper {
    

    // ========== MESSAGE READ STATUS ==========

    public static List<Dto.MessageReadStatus> toMessageReadDTOs(Collection<MessageReadStatusResult> items) {
        if (items == null) return null;

        List<Dto.MessageReadStatus> resultMap = new LinkedList<>();
        for (MessageReadStatusResult item : items) {
            resultMap.add(new Dto.MessageReadStatus(item.getUserId(), item.getReadAt()));
        }
        return resultMap;
    }


    // ========== LOGIN HISTORY ==========

    public static LoginHistory toLoginHistoryEntity(CreateDto.LoginHistory loginHistory) {
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
