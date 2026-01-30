package com.online.attendance.odoo;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/odoo")
public class OdooController {

    private final OdooSyncService odooSyncService;

    public OdooController(OdooSyncService odooSyncService) {
        this.odooSyncService = odooSyncService;
    }

    @PostMapping("/sync/today")
    public OdooSyncResult syncToday(Authentication authentication) {
        return odooSyncService.syncToday(authentication);
    }
}

