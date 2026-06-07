package com.online.attendance.sports.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipFeeRepository extends JpaRepository<MembershipFee, Long> {
    List<MembershipFee> findByClubId(Long clubId);
    List<MembershipFee> findByTeamId(Long teamId);
    List<MembershipFee> findByActiveTrue();
}
