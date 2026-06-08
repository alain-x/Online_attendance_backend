package com.online.attendance.sports.payment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipFeeRepository extends JpaRepository<MembershipFee, Long> {

    @EntityGraph(attributePaths = {"team", "club"})
    @Override
    List<MembershipFee> findAll();

    @EntityGraph(attributePaths = {"team", "club"})
    List<MembershipFee> findByClubId(Long clubId);

    @EntityGraph(attributePaths = {"team", "club"})
    List<MembershipFee> findByTeamId(Long teamId);

    @EntityGraph(attributePaths = {"team", "club"})
    List<MembershipFee> findByActiveTrue();
}
