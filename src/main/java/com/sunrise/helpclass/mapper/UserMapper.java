package com.sunrise.helpclass.mapper;

import com.sunrise.cache.entity.*;
import com.sunrise.core.creation.*;
import com.sunrise.db.entity.*;
import com.sunrise.db.result.*;
import com.sunrise.orchestrator.result.*;

import java.util.*;

public class UserMapper {

    // ========== USER ==========

    public static CacheUserSecurity copy(CacheUserSecurity user) {
        if (user == null) return null;

        return new CacheUserSecurity(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }

    public static CacheUserProfile copy(CacheUserProfile user) {
        if (user == null) return null;

        return new CacheUserProfile(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }

    public static CacheUserProfile toProfileCache(CreateUserDTO user) {
        if (user == null) return null;

        return new CacheUserProfile(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }

    public static CacheUserProfile toProfileCache(UserProfileResult user) {
        if (user == null) return null;

        return new CacheUserProfile(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getCreatedAt(),
            user.getCreatedAt(),
            user.getCreatedAt(),
            user.getIsDeleted()
        );
    }

    public static CacheUserSecurity toSecurityCache(CreateUserDTO user) {
        if (user == null) return null;

        return new CacheUserSecurity(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }

    public static CacheUserSecurity toSecurityCache(UserSecurityResult user) {
        if (user == null) return null;

        return new CacheUserSecurity(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.getIsEnabled(),
            user.getDeletedAt(),
            user.getIsDeleted()
        );
    }

    public static User toEntity(CreateUserDTO user) {
        if (user == null) return null;

        return new User(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getHashPassword(),
            user.getLastLogin(),
            user.getCreatedAt(),
            user.getCreatedAt(),
            user.getCreatedAt(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }

    public static UserSecurityDTO toSecurityDTO(UserSecurityResult user) {
        if (user == null) return null;

        return new UserSecurityDTO(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.getIsEnabled(),
            user.getDeletedAt(),
            user.getIsDeleted()
        );
    }

    public static UserSecurityDTO toSecurityDTO(CacheUserSecurity user) {
        if (user == null) return null;

        return new UserSecurityDTO(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }

    public static UserProfileLightDTO toProfileLightDTO(CacheUserProfile user) {
        if (user == null) return null;

        return new UserProfileLightDTO(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt()
        );
    }

    public static UserProfileLightDTO toProfileLightDTO(UserProfileResult user) {
        if (user == null) return null;

        return new UserProfileLightDTO(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt()
        );
    }

    // TODO: ЭТО ВРЕМЕННО, ПОКА НЕ РЕАЛИЗОВАНО ХРАНЕНИЕ АВАТАРОВ И ДРУГОЙ ИНФОРМАЦИИ
    public static UserProfileFullDTO toProfileFullDTO(CacheUserProfile user) {
        if (user == null) return null;

        return new UserProfileFullDTO(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt()
        );
    }

    public static UserProfileFullDTO toProfileFullDTO(UserProfileResult user) {
        if (user == null) return null;

        return new UserProfileFullDTO(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt()
        );
    }

    public static List<UserProfileLightDTO> toProfileDTOs(Collection<UserProfileResult> users) {
        if (users == null) return null;

        List<UserProfileLightDTO> resultMap = new LinkedList<>();
        for (UserProfileResult user : users) {
            resultMap.add(toProfileLightDTO(user));
        }
        return resultMap;
    }

    // ========== USER AVATAR (закомментировано) ==========
    // public static UserAvatarDTO toDtoUserAvatar(UserProfileResult user) { ... }
    // public static UserAvatarDTO toDtoUserAvatar(CacheUserProfile user) { ... }
}