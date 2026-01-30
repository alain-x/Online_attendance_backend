package com.online.attendance.odoo;

public record OdooSyncResult(boolean success, String message, int recordsConsidered) {}

