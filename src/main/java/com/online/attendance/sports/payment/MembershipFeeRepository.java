package com.online.attendance.sports.payment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    @EntityGraph(attributePaths = {"team", "club"})
    @Query("SELECT mf FROM MembershipFee mf JOIN mf.club c WHERE c.company.id = :companyId")
    List<MembershipFee> findByClubCompanyId(@Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"team", "club"})
    @Query("SELECT mf FROM MembershipFee mf JOIN mf.club c WHERE c.company.id = :companyId AND mf.id = :id")
    Optional<MembershipFee> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
