package com.online.attendance.location;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationVerificationService {

    private final WorkLocationRepository workLocationRepository;
    private final GeoService geoService;

    public LocationVerificationService(WorkLocationRepository workLocationRepository, GeoService geoService) {
        this.workLocationRepository = workLocationRepository;
        this.geoService = geoService;
    }

    public boolean isWithinAnyActiveLocation(Long companyId, double latitude, double longitude) {
        List<WorkLocation> locations = workLocationRepository.findByActiveTrueAndCompanyId(companyId);
        for (WorkLocation location : locations) {
            double dist = geoService.distanceMeters(latitude, longitude, location.getLatitude(), location.getLongitude());
            if (dist <= location.getRadiusMeters()) {
                return true;
            }
        }
        return false;
    }
}
