package com.sunrise.db.repository;

import com.sunrise.db.entity.LoginHistory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> { }
