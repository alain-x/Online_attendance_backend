package com.online.attendance.sports.sport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportRepository extends JpaRepository<Sport, Long> {
    List<Sport> findByActiveTrue();
}
