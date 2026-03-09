package com.online.attendance.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, Long> {
    Optional<CompanySubscription> findByCompany_Id(Long companyId);
}
