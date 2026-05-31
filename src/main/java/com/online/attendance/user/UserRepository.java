package com.online.attendance.user;

import com.online.attendance.user.dto.UserCompanyContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT new com.online.attendance.user.dto.UserCompanyContext(u.role, c.id, pc.id)
            FROM AppUser u
            JOIN u.company c
            LEFT JOIN c.parentCompany pc
            WHERE u.username = :username AND c.slug = :companySlug
            """)
    Optional<UserCompanyContext> findUserCompanyContext(
            @Param("username") String username,
            @Param("companySlug") String companySlug
    );
}
