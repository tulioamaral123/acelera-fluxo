package com.verdefluxo.acelera.repository;

import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.model.entity.UserApiSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserApiSettingsRepository extends JpaRepository<UserApiSettings, Long> {
    Optional<UserApiSettings> findByUser(User user);
}
