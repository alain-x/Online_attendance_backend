package com.online.attendance.face;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.attendance.employee.Employee;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.List;

@Service
public class FaceService {

    private static final double FACE_DESCRIPTOR_THRESHOLD = 0.6;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        if (employee == null) {
            return false;
        }
        boolean hasDescriptor = employee.getFaceDescriptor() != null && !employee.getFaceDescriptor().isBlank();
        boolean hasHash = employee.getFaceTemplateRef() != null && !employee.getFaceTemplateRef().isBlank();
        return hasDescriptor || hasHash;
    }

    public boolean verify(Employee employee, MultipartFile image) {
        return verify(employee, image, null);
    }

    /**
     * Verify using image hash and/or AI descriptor.
     * If employee has faceDescriptor and candidateDescriptor is provided, use euclidean distance (AI).
     * Otherwise fall back to image hash match.
     */
    public boolean verify(Employee employee, MultipartFile image, String candidateDescriptorJson) {
        if (employee == null) {
            return false;
        }
        if (!hasEnrollment(employee)) {
            return false;
        }

        if (candidateDescriptorJson == null || candidateDescriptorJson.isBlank()) {
            if (image == null || image.isEmpty()) {
                return false;
            }
            String storedHash = employee.getFaceTemplateRef();
            if (storedHash == null || storedHash.isBlank()) {
                return false;
            }
            String candidateHash = hash(image);
            return storedHash.equals(candidateHash);
        }

        try {
            List<Double> stored = objectMapper.readValue(employee.getFaceDescriptor(), new TypeReference<>() {});
            List<Double> candidate = objectMapper.readValue(candidateDescriptorJson, new TypeReference<>() {});
            if (stored.size() != 128 || candidate.size() != 128) {
                return false;
            }
            double distance = euclideanDistance(stored, candidate);
            return distance < FACE_DESCRIPTOR_THRESHOLD;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static double euclideanDistance(List<Double> a, List<Double> b) {
        double sum = 0;
        for (int i = 0; i < a.size() && i < b.size(); i++) {
            double d = a.get(i) - b.get(i);
            sum += d * d;
        }
        return Math.sqrt(sum);
    }
}
