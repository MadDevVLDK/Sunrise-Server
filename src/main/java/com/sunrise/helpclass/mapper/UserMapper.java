package com.sunrise.helpclass.mapper;

import com.sunrise.cache.entity.*;
import com.sunrise.core.creation.*;
import com.sunrise.db.entity.*;
import com.sunrise.db.result.*;
import com.sunrise.orchestrator.result.*;

import java.util.*;

public class UserMapper {

    // ========== USER ==========

    public static Cache.UserSecurity copy(Cache.UserSecurity user) {
        if (user == null) return null;

        return new Cache.UserSecurity(
            user.id(),
            user.email(),
            user.hashPassword(),
            user.jwtVersion(),
            user.isEnabled(),
            user.deletedAt(),
            user.isDeleted()
        );
    }

    public static Cache.UserProfile copy(Cache.UserProfile user) {
        if (user == null) return null;

        return new Cache.UserProfile(
            user.id(),
            user.username(),
            user.name(),
            user.profileUpdatedAt(),
            user.createdAt(),
            user.deletedAt(),
            user.isDeleted()
        );
    }

    public static Cache.UserProfile toProfileCache(CreateDto.User user) {
        if (user == null) return null;

        return new Cache.UserProfile(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }

    public static Cache.UserProfile toProfileCache(UserProfileResult user) {
        if (user == null) return null;

        return new Cache.UserProfile(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getCreatedAt(),
            user.getCreatedAt(),
            user.getCreatedAt(),
            user.getIsDeleted()
        );
    }

    public static Cache.UserSecurity toSecurityCache(CreateDto.User user) {
        if (user == null) return null;

        return new Cache.UserSecurity(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }

    public static Cache.UserSecurity toSecurityCache(UserSecurityResult user) {
        if (user == null) return null;

        return new Cache.UserSecurity(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.getIsEnabled(),
            user.getDeletedAt(),
            user.getIsDeleted()
        );
    }

    public static User toEntity(CreateDto.User user) {
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

    public static Dto.UserSecurity toSecurityDTO(UserSecurityResult user) {
        if (user == null) return null;

        return new Dto.UserSecurity(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.getIsEnabled(),
            user.getDeletedAt(),
            user.getIsDeleted()
        );
    }

    public static Dto.UserSecurity toSecurityDTO(Cache.UserSecurity user) {
        if (user == null) return null;

        return new Dto.UserSecurity(
            user.id(),
            user.email(),
            user.hashPassword(),
            user.jwtVersion(),
            user.isEnabled(),
            user.deletedAt(),
            user.isDeleted()
        );
    }

    public static Dto.UserProfileLight toProfileLightDTO(Cache.UserProfile user) {
        if (user == null) return null;

        return new Dto.UserProfileLight(
            user.id(),
            user.username(),
            user.name(),
            user.profileUpdatedAt(),
            user.createdAt()
        );
    }

    public static Dto.UserProfileLight toProfileLightDTO(UserProfileResult user) {
        if (user == null) return null;

        return new Dto.UserProfileLight(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt()
        );
    }

    // TODO: ЭТО ВРЕМЕННО, ПОКА НЕ РЕАЛИЗОВАНО ХРАНЕНИЕ АВАТАРОВ И ДРУГОЙ ИНФОРМАЦИИ
    public static Dto.UserProfileFull toProfileFullDTO(Cache.UserProfile user) {
        if (user == null) return null;

        return new Dto.UserProfileFull(
            user.id(),
            user.username(),
            user.name(),
            user.profileUpdatedAt(),
            user.createdAt()
        );
    }

    public static Dto.UserProfileFull toProfileFullDTO(UserProfileResult user) {
        if (user == null) return null;

        return new Dto.UserProfileFull(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt()
        );
    }

    public static List<Dto.UserProfileLight> toProfileDTOs(Collection<UserProfileResult> users) {
        if (users == null) return null;

        List<Dto.UserProfileLight> resultMap = new LinkedList<>();
        for (UserProfileResult user : users) {
            resultMap.add(toProfileLightDTO(user));
        }
        return resultMap;
    }

    // ========== USER AVATAR (закомментировано) ==========
    // public static UserAvatarDTO toDtoUserAvatar(UserProfileResult user) { ... }
    // public static UserAvatarDTO toDtoUserAvatar(Cache.UserProfile user) { ... }
}