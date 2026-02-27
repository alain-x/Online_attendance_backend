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
public class InvoiceTransaction {
    private String transactionDate;
    private String gateway;
    private String transactionId;
    private BigDecimal amount;
}
