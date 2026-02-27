package com.online.attendance.invoicepdf;

import com.online.attendance.invoicepdf.dto.InvoicePdfRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/invoices")
public class InvoicePdfController {

    private final InvoicePdfService invoicePdfService;

    public InvoicePdfController(InvoicePdfService invoicePdfService) {
        this.invoicePdfService = invoicePdfService;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generatePdf(@Valid @RequestBody InvoicePdfRequest request) {
        byte[] pdf = invoicePdfService.generateInvoicePdf(request);

        String invoiceNumber = request != null ? request.getInvoiceNumber() : null;
        String filename = (invoiceNumber != null && !invoiceNumber.isBlank())
                ? ("invoice-" + invoiceNumber.trim() + ".pdf")
                : "invoice.pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
