package com.online.attendance.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    List<AppUser> findAllByUsername(String username);
    boolean existsByUsername(String username);

    Optional<AppUser> findByUsernameAndCompanySlug(String username, String companySlug);
    boolean existsByUsernameAndCompanyId(String username, Long companyId);
    boolean existsByEmailAndCompanyId(String email, Long companyId);

    List<AppUser> findAllByCompanyId(Long companyId);
    Optional<AppUser> findByIdAndCompanyId(Long id, Long companyId);
    long countByCompanyId(Long companyId);
}
