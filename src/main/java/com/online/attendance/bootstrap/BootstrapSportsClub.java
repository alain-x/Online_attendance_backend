package com.online.attendance.bootstrap;

import com.online.attendance.sports.club.SportsClub;
import com.online.attendance.sports.club.SportsClubRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(3)
public class BootstrapSportsClub implements CommandLineRunner {

    private final SportsClubRepository clubRepository;

    public BootstrapSportsClub(SportsClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (clubRepository.count() > 0) return;

        SportsClub club = SportsClub.builder()
                .name("Default Sports Club")
                .slug("default-club")
                .description("Main sports club for managing teams and players")
                .build();
        clubRepository.save(club);
    }
}
