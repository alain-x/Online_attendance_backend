package com.online.attendance.sports.club;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SportsClubRepository extends JpaRepository<SportsClub, Long> {
    Optional<SportsClub> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
