package com.online.attendance.face;

import com.online.attendance.employee.Employee;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;

@Service
public class FaceService {

    public String hash(MultipartFile image) {
        try {
            byte[] bytes = image.getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash face image", ex);
        }
    }

    public boolean hasEnrollment(Employee employee) {
        return employee.getFaceTemplateRef() != null && !employee.getFaceTemplateRef().isBlank();
    }

    public boolean verify(Employee employee, MultipartFile image) {
        if (!hasEnrollment(employee)) {
            return false;
        }
        String candidate = hash(image);
        return employee.getFaceTemplateRef().equals(candidate);
    }
}
