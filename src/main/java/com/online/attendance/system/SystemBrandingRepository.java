package com.online.attendance.system;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SystemBrandingRepository extends JpaRepository<SystemBranding, String> {

    interface BrandingImageView {
        byte[] getImageBytes();
        String getContentType();
    }

    @Query("SELECT b.logoBytes AS imageBytes, b.logoContentType AS contentType FROM SystemBranding b WHERE b.id = :id")
    Optional<BrandingImageView> findLogoViewById(@Param("id") String id);

    @Query("SELECT b.faviconBytes AS imageBytes, b.faviconContentType AS contentType FROM SystemBranding b WHERE b.id = :id")
    Optional<BrandingImageView> findFaviconViewById(@Param("id") String id);
}
