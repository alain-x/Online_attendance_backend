package com.online.attendance.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PesapalSettingsRepository extends JpaRepository<PesapalSettings, Long> {
    Optional<PesapalSettings> findTopByOrderByIdAsc();
}
