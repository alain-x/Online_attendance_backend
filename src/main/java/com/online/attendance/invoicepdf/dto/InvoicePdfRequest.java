package com.online.attendance.invoicepdf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoicePdfRequest {
    /** Example: 11281 */
    private String invoiceNumber;

    /** ISO date string (you control formatting) */
    private String invoiceDate;

    /** ISO date string (you control formatting) */
    private String dueDate;

    /** UNPAID / PAID / OVERDUE etc */
    private String status;

    /** Example: RWF, USD, EUR, etc */
    private String currency;

    /** VAT rate percent (e.g. 18.0). */
    private BigDecimal vatRatePercent;

    /** Optional credit/discount applied (positive number reduces total). */
    private BigDecimal credit;

    /** Optional: override computed subTotal. If null, computed from items. */
    private BigDecimal subTotal;

    /** Optional: override computed vatAmount. If null, computed from subTotal and vatRatePercent. */
    private BigDecimal vatAmount;

    /** Optional: override computed total. If null, computed: subTotal + vatAmount - credit. */
    private BigDecimal total;

    private PartyInfo seller;
    private PartyInfo billedTo;

    private List<InvoiceLineItem> items;
    private List<InvoiceTransaction> transactions;

    /** Optional footer override. If null, service prints "PDF Generated on <today>" */
    private String generatedOn;
}
