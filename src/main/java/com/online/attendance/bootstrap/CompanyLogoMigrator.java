package com.online.attendance.bootstrap;

import com.online.attendance.company.Company;
import com.online.attendance.company.CompanyLogoUrls;
import com.online.attendance.company.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Repairs company logo_url values and imports legacy on-disk logos into logo_bytes.
 */
@Component
@Order(3)
public class CompanyLogoMigrator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CompanyLogoMigrator.class);

    private final CompanyRepository companyRepository;

    public CompanyLogoMigrator(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public void run(String... args) {
        for (Company company : companyRepository.findAll()) {
            boolean changed = false;

            if (!hasBytes(company.getLogoBytes())) {
                byte[] fromDisk = readLegacyLogoFromDisk(company.getId());
                if (fromDisk != null) {
                    company.setLogoBytes(fromDisk);
                    company.setLogoContentType(guessContentType(fromDisk));
                    company.setLogoUrl(CompanyLogoUrls.apiImagePath(company.getId()));
                    changed = true;
                    log.info("Imported company {} logo from disk ({} bytes)", company.getId(), fromDisk.length);
                }
            }

            String normalized = CompanyLogoUrls.normalizeStoredUrl(company.getLogoUrl(), company.getId());
            if (normalized != null && !normalized.equals(company.getLogoUrl())) {
                company.setLogoUrl(normalized);
                changed = true;
            }

            if (hasBytes(company.getLogoBytes())
                    && (company.getLogoUrl() == null || company.getLogoUrl().isBlank())) {
                company.setLogoUrl(CompanyLogoUrls.apiImagePath(company.getId()));
                changed = true;
            }

            if (changed) {
                companyRepository.save(company);
            }
        }
    }

    private static boolean hasBytes(byte[] bytes) {
        return bytes != null && bytes.length > 0;
    }

    private static byte[] readLegacyLogoFromDisk(Long companyId) {
        if (companyId == null) {
            return null;
        }
        Path[] dirs = {
                Paths.get("uploads", "company-logos", String.valueOf(companyId)),
                Paths.get("uploads", "companies", String.valueOf(companyId)),
                Paths.get("uploads", "logos", String.valueOf(companyId)),
        };
        for (Path dir : dirs) {
            byte[] file = readFirstFile(dir);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    private static byte[] readFirstFile(Path dir) {
        try {
            if (!Files.isDirectory(dir)) {
                return null;
            }
            try (Stream<Path> stream = Files.list(dir)) {
                Path file = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                                    || name.endsWith(".webp") || name.endsWith(".gif");
                        })
                        .findFirst()
                        .orElse(null);
                if (file == null) {
                    return null;
                }
                return Files.readAllBytes(file);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private static String guessContentType(byte[] bytes) {
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50) {
            return "image/png";
        }
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
            return "image/jpeg";
        }
        return "image/jpeg";
    }
}
