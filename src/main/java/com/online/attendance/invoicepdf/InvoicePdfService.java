package com.online.attendance.invoicepdf;

import com.online.attendance.invoicepdf.dto.InvoiceLineItem;
import com.online.attendance.invoicepdf.dto.InvoicePdfRequest;
import com.online.attendance.invoicepdf.dto.InvoiceTransaction;
import com.online.attendance.invoicepdf.dto.PartyInfo;
import com.online.attendance.system.SystemBranding;
import com.online.attendance.system.SystemBrandingRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@Service
public class InvoicePdfService {

    private static final String SYSTEM_BRANDING_ID = "SYSTEM";

    private final SystemBrandingRepository systemBrandingRepository;

    public InvoicePdfService(SystemBrandingRepository systemBrandingRepository) {
        this.systemBrandingRepository = systemBrandingRepository;
    }

    public byte[] generateInvoicePdf(InvoicePdfRequest req) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDRectangle mediaBox = page.getMediaBox();
            float pageW = mediaBox.getWidth();
            float pageH = mediaBox.getHeight();

            float margin = 48f;
            float y = pageH - margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDImageXObject logo = loadSystemLogo(doc);
                if (logo != null) {
                    float logoW = 110f;
                    float logoH = logo.getHeight() * (logoW / logo.getWidth());
                    cs.drawImage(logo, margin, y - logoH, logoW, logoH);
                }

                String status = safe(req.getStatus(), "UNPAID");
                drawStatusRibbon(cs, pageW, pageH, status);

                float headerRightX = pageW - margin;
                float invoiceTitleY = y - 8f;
                drawTextRight(cs, PDType1Font.HELVETICA_BOLD, 20f, headerRightX, invoiceTitleY, "Invoice");

                float metaY = invoiceTitleY - 24f;
                float metaFont = 10.5f;
                drawKeyValueRight(cs, headerRightX, metaY, metaFont, "Invoice", safe(req.getInvoiceNumber(), ""));
                metaY -= 14f;
                drawKeyValueRight(cs, headerRightX, metaY, metaFont, "Invoice Date", safe(req.getInvoiceDate(), ""));
                metaY -= 14f;
                drawKeyValueRight(cs, headerRightX, metaY, metaFont, "Due Date", safe(req.getDueDate(), ""));

                y = y - 80f;

                float colGap = 36f;
                float colW = (pageW - 2 * margin - colGap) / 2f;

                float leftX = margin;
                float rightX = margin + colW + colGap;

                float boxTitleFont = 11.5f;
                float boxBodyFont = 10.5f;

                float leftY = y;
                drawText(cs, PDType1Font.HELVETICA_BOLD, boxTitleFont, leftX, leftY, "Invoice From");
                leftY -= 14f;
                leftY = drawPartyBlock(cs, leftX, leftY, colW, boxBodyFont, req.getSeller());

                float rightY = y;
                drawText(cs, PDType1Font.HELVETICA_BOLD, boxTitleFont, rightX, rightY, "Billed To");
                rightY -= 14f;
                rightY = drawPartyBlock(cs, rightX, rightY, colW, boxBodyFont, req.getBilledTo());

                y = Math.min(leftY, rightY) - 22f;

                BigDecimal subTotal = computeSubTotal(req);
                BigDecimal vatAmount = computeVatAmount(req, subTotal);
                BigDecimal credit = nz(req.getCredit());
                BigDecimal total = computeTotal(req, subTotal, vatAmount, credit);

                String currency = safe(req.getCurrency(), "");

                y = drawItemsTable(cs, margin, y, pageW - 2 * margin, req.getItems(), currency);
                y -= 18f;

                float totalsX = pageW - margin;
                float totalsFont = 10.5f;

                drawKeyMoneyRight(cs, totalsX, y, totalsFont, "Sub Total", currency, subTotal);
                y -= 14f;

                BigDecimal vatRate = req.getVatRatePercent();
                String vatLabel = vatRate != null ? ("VAT (" + stripTrailingZeros(vatRate) + "%)") : "VAT";
                drawKeyMoneyRight(cs, totalsX, y, totalsFont, vatLabel, currency, vatAmount);
                y -= 14f;

                if (credit.compareTo(BigDecimal.ZERO) != 0) {
                    drawKeyMoneyRight(cs, totalsX, y, totalsFont, "Credit", currency, credit.negate());
                    y -= 14f;
                }

                drawKeyMoneyRight(cs, totalsX, y, 12f, "Total", currency, total);
                y -= 24f;

                List<InvoiceTransaction> txs = req.getTransactions();
                if (txs != null && !txs.isEmpty()) {
                    y = drawTransactionsTable(cs, margin, y, pageW - 2 * margin, txs, currency);
                    y -= 14f;
                }

                String generatedOn = req.getGeneratedOn();
                String footer = (generatedOn != null && !generatedOn.isBlank()) ? generatedOn.trim() : ("PDF Generated on " + Instant.now().toString());
                drawText(cs, PDType1Font.HELVETICA, 9.5f, margin, margin - 10f, footer);
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private PDImageXObject loadSystemLogo(PDDocument doc) {
        try {
            SystemBranding branding = systemBrandingRepository.findById(SYSTEM_BRANDING_ID).orElse(null);
            if (branding == null || branding.getLogoPath() == null || branding.getLogoPath().isBlank()) {
                return null;
            }
            Path p = Paths.get(branding.getLogoPath());
            if (!Files.exists(p) || !Files.isRegularFile(p)) {
                return null;
            }
            return PDImageXObject.createFromFile(p.toString(), doc);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void drawStatusRibbon(PDPageContentStream cs, float pageW, float pageH, String status) {
        try {
            float ribbonW = 210f;
            float ribbonH = 26f;
            float centerX = pageW - 96f;
            float centerY = pageH - 62f;
            float angle = (float) Math.toRadians(35);

            cs.saveGraphicsState();
            cs.transform(Matrix.getTranslateInstance(centerX, centerY));
            cs.transform(Matrix.getRotateInstance(angle, 0, 0));

            Color fill = statusColor(status);
            cs.setNonStrokingColor(fill);
            cs.addRect(-ribbonW / 2f, -ribbonH / 2f, ribbonW, ribbonH);
            cs.fill();

            cs.setNonStrokingColor(Color.WHITE);
            drawTextCentered(cs, PDType1Font.HELVETICA_BOLD, 12f, 0, -4f, status.toUpperCase());

            cs.restoreGraphicsState();
        } catch (Exception ignored) {
        }
    }

    private Color statusColor(String status) {
        if (status == null) {
            return new Color(226, 88, 88);
        }
        String s = status.trim().toUpperCase();
        if (s.contains("PAID")) {
            return new Color(41, 163, 86);
        }
        if (s.contains("OVERDUE")) {
            return new Color(226, 88, 88);
        }
        if (s.contains("PART")) {
            return new Color(230, 156, 43);
        }
        return new Color(86, 110, 217);
    }

    private float drawPartyBlock(PDPageContentStream cs, float x, float y, float w, float fontSize, PartyInfo p) throws Exception {
        if (p == null) {
            return y;
        }
        y = drawPartyLine(cs, x, y, w, PDType1Font.HELVETICA_BOLD, fontSize, p.getName());
        y = drawPartyLine(cs, x, y, w, PDType1Font.HELVETICA, fontSize, p.getAttn() != null && !p.getAttn().isBlank() ? ("Attn: " + p.getAttn()) : null);
        y = drawPartyLine(cs, x, y, w, PDType1Font.HELVETICA, fontSize, p.getAddressLine1());
        y = drawPartyLine(cs, x, y, w, PDType1Font.HELVETICA, fontSize, p.getAddressLine2());
        y = drawPartyLine(cs, x, y, w, PDType1Font.HELVETICA, fontSize, p.getAddressLine3());
        y = drawPartyLine(cs, x, y, w, PDType1Font.HELVETICA, fontSize, p.getPhone() != null && !p.getPhone().isBlank() ? ("Phone: " + p.getPhone()) : null);
        y = drawPartyLine(cs, x, y, w, PDType1Font.HELVETICA, fontSize, p.getEmail() != null && !p.getEmail().isBlank() ? ("Email: " + p.getEmail()) : null);
        y = drawPartyLine(cs, x, y, w, PDType1Font.HELVETICA, fontSize, p.getVatNumber() != null && !p.getVatNumber().isBlank() ? ("VAT: " + p.getVatNumber()) : null);
        return y;
    }

    private float drawPartyLine(PDPageContentStream cs, float x, float y, float w, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, String line) throws Exception {
        if (line == null || line.isBlank()) {
            return y;
        }
        List<String> wrapped = wrap(font, fontSize, line.trim(), w);
        for (String s : wrapped) {
            drawText(cs, font, fontSize, x, y, s);
            y -= 13f;
        }
        return y;
    }

    private float drawItemsTable(PDPageContentStream cs, float x, float y, float w, List<InvoiceLineItem> items, String currency) throws Exception {
        drawText(cs, PDType1Font.HELVETICA_BOLD, 11.5f, x, y, "Summary");
        y -= 12f;

        float tableTop = y;
        float rowH = 18f;
        float descW = w * 0.72f;
        float amtW = w - descW;

        cs.setNonStrokingColor(new Color(245, 246, 248));
        cs.addRect(x, tableTop - rowH, w, rowH);
        cs.fill();

        cs.setNonStrokingColor(Color.BLACK);
        drawText(cs, PDType1Font.HELVETICA_BOLD, 10.5f, x + 6f, tableTop - 13f, "Description");
        drawTextRight(cs, PDType1Font.HELVETICA_BOLD, 10.5f, x + descW + amtW - 6f, tableTop - 13f, "Total");

        float curY = tableTop - rowH;

        if (items == null || items.isEmpty()) {
            curY -= rowH;
            drawText(cs, PDType1Font.HELVETICA, 10.5f, x + 6f, curY + 5f, "-");
        } else {
            for (InvoiceLineItem it : items) {
                curY -= rowH;
                String desc = it != null ? safe(it.getDescription(), "") : "";
                BigDecimal amt = it != null ? nz(it.getTotal()) : BigDecimal.ZERO;

                List<String> wrapped = wrap(PDType1Font.HELVETICA, 10.5f, desc, descW - 12f);
                String first = wrapped.isEmpty() ? "" : wrapped.get(0);
                drawText(cs, PDType1Font.HELVETICA, 10.5f, x + 6f, curY + 5f, first);

                drawTextRight(cs, PDType1Font.HELVETICA, 10.5f, x + descW + amtW - 6f, curY + 5f, formatMoney(currency, amt));

                if (wrapped.size() > 1) {
                    for (int i = 1; i < wrapped.size(); i++) {
                        curY -= rowH;
                        drawText(cs, PDType1Font.HELVETICA, 10.5f, x + 6f, curY + 5f, wrapped.get(i));
                    }
                }
            }
        }

        float bottom = curY;

        cs.setStrokingColor(new Color(218, 222, 228));
        cs.setLineWidth(0.8f);
        cs.addRect(x, bottom, w, tableTop - bottom);
        cs.stroke();

        cs.moveTo(x + descW, bottom);
        cs.lineTo(x + descW, tableTop);
        cs.stroke();

        return bottom - 6f;
    }

    private float drawTransactionsTable(PDPageContentStream cs, float x, float y, float w, List<InvoiceTransaction> txs, String currency) throws Exception {
        drawText(cs, PDType1Font.HELVETICA_BOLD, 11.5f, x, y, "Transactions");
        y -= 12f;

        float tableTop = y;
        float rowH = 18f;

        float dateW = w * 0.18f;
        float gateW = w * 0.20f;
        float idW = w * 0.38f;
        float amtW = w - dateW - gateW - idW;

        cs.setNonStrokingColor(new Color(245, 246, 248));
        cs.addRect(x, tableTop - rowH, w, rowH);
        cs.fill();

        cs.setNonStrokingColor(Color.BLACK);
        drawText(cs, PDType1Font.HELVETICA_BOLD, 10.5f, x + 6f, tableTop - 13f, "Date");
        drawText(cs, PDType1Font.HELVETICA_BOLD, 10.5f, x + dateW + 6f, tableTop - 13f, "Gateway");
        drawText(cs, PDType1Font.HELVETICA_BOLD, 10.5f, x + dateW + gateW + 6f, tableTop - 13f, "Transaction ID");
        drawTextRight(cs, PDType1Font.HELVETICA_BOLD, 10.5f, x + w - 6f, tableTop - 13f, "Amount");

        float curY = tableTop - rowH;
        for (InvoiceTransaction tx : txs) {
            curY -= rowH;
            String date = tx != null ? safe(tx.getTransactionDate(), "") : "";
            String gateway = tx != null ? safe(tx.getGateway(), "") : "";
            String id = tx != null ? safe(tx.getTransactionId(), "") : "";
            BigDecimal amt = tx != null ? nz(tx.getAmount()) : BigDecimal.ZERO;

            drawText(cs, PDType1Font.HELVETICA, 10.5f, x + 6f, curY + 5f, truncateToFit(PDType1Font.HELVETICA, 10.5f, date, dateW - 12f));
            drawText(cs, PDType1Font.HELVETICA, 10.5f, x + dateW + 6f, curY + 5f, truncateToFit(PDType1Font.HELVETICA, 10.5f, gateway, gateW - 12f));
            drawText(cs, PDType1Font.HELVETICA, 10.5f, x + dateW + gateW + 6f, curY + 5f, truncateToFit(PDType1Font.HELVETICA, 10.5f, id, idW - 12f));
            drawTextRight(cs, PDType1Font.HELVETICA, 10.5f, x + w - 6f, curY + 5f, formatMoney(currency, amt));
        }

        float bottom = curY;

        cs.setStrokingColor(new Color(218, 222, 228));
        cs.setLineWidth(0.8f);
        cs.addRect(x, bottom, w, tableTop - bottom);
        cs.stroke();

        cs.moveTo(x + dateW, bottom);
        cs.lineTo(x + dateW, tableTop);
        cs.stroke();

        cs.moveTo(x + dateW + gateW, bottom);
        cs.lineTo(x + dateW + gateW, tableTop);
        cs.stroke();

        cs.moveTo(x + dateW + gateW + idW, bottom);
        cs.lineTo(x + dateW + gateW + idW, tableTop);
        cs.stroke();

        return bottom - 6f;
    }

    private void drawKeyValueRight(PDPageContentStream cs, float rightX, float y, float fontSize, String key, String value) throws Exception {
        String k = safe(key, "");
        String v = safe(value, "");
        String text = k + ": " + v;
        drawTextRight(cs, PDType1Font.HELVETICA, fontSize, rightX, y, text);
    }

    private void drawKeyMoneyRight(PDPageContentStream cs, float rightX, float y, float fontSize, String key, String currency, BigDecimal amount) throws Exception {
        String k = safe(key, "");
        String v = formatMoney(currency, amount);
        float gap = 140f;

        drawTextRight(cs, PDType1Font.HELVETICA, fontSize, rightX - gap, y, k);
        drawTextRight(cs, PDType1Font.HELVETICA_BOLD, fontSize, rightX, y, v);
    }

    private void drawText(PDPageContentStream cs, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float x, float y, String text) throws Exception {
        if (text == null) {
            return;
        }
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void drawTextRight(PDPageContentStream cs, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float rightX, float y, String text) throws Exception {
        if (text == null) {
            return;
        }
        float w = font.getStringWidth(text) / 1000f * fontSize;
        drawText(cs, font, fontSize, rightX - w, y, text);
    }

    private void drawTextCentered(PDPageContentStream cs, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float centerX, float y, String text) throws Exception {
        if (text == null) {
            return;
        }
        float w = font.getStringWidth(text) / 1000f * fontSize;
        drawText(cs, font, fontSize, centerX - w / 2f, y, text);
    }

    private List<String> wrap(org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, String text, float maxWidth) throws Exception {
        if (text == null) {
            return List.of();
        }
        String[] words = text.trim().split("\\s+");
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();

        for (String w : words) {
            if (cur.length() == 0) {
                cur.append(w);
                continue;
            }
            String candidate = cur + " " + w;
            float cw = font.getStringWidth(candidate) / 1000f * fontSize;
            if (cw <= maxWidth) {
                cur.append(" ").append(w);
            } else {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(w);
            }
        }

        if (cur.length() > 0) {
            lines.add(cur.toString());
        }
        return lines;
    }

    private String truncateToFit(org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, String text, float maxWidth) throws Exception {
        if (text == null) {
            return "";
        }
        String t = text;
        float w = font.getStringWidth(t) / 1000f * fontSize;
        if (w <= maxWidth) {
            return t;
        }
        String ell = "...";
        while (t.length() > 0) {
            t = t.substring(0, t.length() - 1);
            String cand = t + ell;
            w = font.getStringWidth(cand) / 1000f * fontSize;
            if (w <= maxWidth) {
                return cand;
            }
        }
        return ell;
    }

    private BigDecimal computeSubTotal(InvoicePdfRequest req) {
        if (req.getSubTotal() != null) {
            return nz(req.getSubTotal());
        }
        List<InvoiceLineItem> items = req.getItems();
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (InvoiceLineItem it : items) {
                if (it != null) {
                    sum = sum.add(nz(it.getTotal()));
                }
            }
        }
        return sum;
    }

    private BigDecimal computeVatAmount(InvoicePdfRequest req, BigDecimal subTotal) {
        if (req.getVatAmount() != null) {
            return nz(req.getVatAmount());
        }
        BigDecimal rate = req.getVatRatePercent();
        if (rate == null) {
            return BigDecimal.ZERO;
        }
        return nz(subTotal).multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeTotal(InvoicePdfRequest req, BigDecimal subTotal, BigDecimal vatAmount, BigDecimal credit) {
        if (req.getTotal() != null) {
            return nz(req.getTotal());
        }
        return nz(subTotal).add(nz(vatAmount)).subtract(nz(credit));
    }

    private String formatMoney(String currency, BigDecimal amount) {
        BigDecimal a = nz(amount).setScale(2, RoundingMode.HALF_UP);
        String c = currency != null ? currency.trim() : "";
        if (c.isBlank()) {
            return a.toPlainString();
        }
        return c + " " + a.toPlainString();
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String safe(String v, String def) {
        if (v == null) {
            return def;
        }
        String t = v.trim();
        return t.isBlank() ? def : t;
    }

    private String stripTrailingZeros(BigDecimal v) {
        if (v == null) {
            return "";
        }
        return v.stripTrailingZeros().toPlainString();
    }
}
