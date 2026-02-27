package com.online.attendance.invoicepdf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItem {
    private String description;
    /** Total amount for this line (already qty * unitPrice if you use that model). */
    private BigDecimal total;
}
