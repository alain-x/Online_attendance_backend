package com.online.attendance.user;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UserProfileImageService {

    public void saveProfileImage(AppUser user, MultipartFile image) throws IOException {
        if (user == null || image == null || image.isEmpty()) {
            return;
        }
        user.setProfileImageBytes(image.getBytes());
        user.setProfileImageContentType(resolveContentType(image.getContentType(), image.getOriginalFilename()));
        user.setProfileImagePath(null);
        user.setProfileImageUrl("/api/users/" + user.getId() + "/profile/image");
    }

    private static String resolveContentType(String fromMultipart, String filename) {
        if (fromMultipart != null && !fromMultipart.isBlank()) {
            return fromMultipart;
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".webp")) return "image/webp";
        }
        return "image/jpeg";
    }
}
