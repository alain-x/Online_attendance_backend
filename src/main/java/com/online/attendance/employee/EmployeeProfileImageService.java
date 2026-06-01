package com.online.attendance.employee;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class EmployeeProfileImageService {

    public void saveProfileImage(Employee employee, MultipartFile image) throws IOException {
        if (employee == null || image == null || image.isEmpty()) {
            return;
        }
        employee.setProfileImageBytes(image.getBytes());
        employee.setProfileImageContentType(resolveContentType(image.getContentType(), image.getOriginalFilename()));
        employee.setProfileImagePath(null);
        employee.setProfileImageUrl("/api/employees/" + employee.getId() + "/profile/image");
    }

    public static String profileImageApiUrl(Long employeeId) {
        return employeeId != null ? "/api/employees/" + employeeId + "/profile/image" : null;
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
