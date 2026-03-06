package com.moreno.electrodomesticos.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.moreno.electrodomesticos.model.Electrodomestico;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Genera un PDF A4 con 4 etiquetas A6 (2 columnas × 2 filas) por página.
 *
 * Layout de cada etiqueta:
 *   ┌──────────────────────────┐
 *   │  CABECERA (marca)        │  ← azul oscuro, texto blanco grande
 *   ├──────────────────────────┤
 *   │  Modelo                  │
 *   │  Características         │
 *   │    ➤ spec1               │
 *   │    ➤ spec2               │
 *   │  Clase Energética: [B►]  │  ← flecha coloreada
 *   │  ╔══════════════════╗    │
 *   │  ║  999,00 €        ║    │  ← pastilla azul oscuro
 *   │  ╚══════════════════╝    │
 *   │  FINANCIACIÓN DISPONIBLE │
 *   ├──────────────────────────┤
 *   │  Moreno                  │  ← pie azul oscuro, texto blanco
 *   └──────────────────────────┘
 */
@Service
public class PdfService {

    // ── Dimensiones ──────────────────────────────────────────────────────────
    private static final float A4_W     = PageSize.A4.getWidth();   // 595.28 pt
    private static final float A4_H     = PageSize.A4.getHeight();  // 841.89 pt
    private static final float A6_W     = A4_W / 2;                 // 297.64 pt
    private static final float A6_H     = A4_H / 2;                 // 420.94 pt
    private static final float HEADER_H = 55f;
    private static final float FOOTER_H = 45f;
    private static final float PAD      = 12f;   // padding horizontal interior

    // ── Color corporativo (azul pizarra) ─────────────────────────────────────
    private static final DeviceRgb COLOR_DARK = new DeviceRgb(0x2d, 0x4a, 0x6e);

    // ── Logo cacheado ─────────────────────────────────────────────────────────
    private ImageData logoData;
    private boolean   logoLoaded = false;

    private ImageData getLogoData() {
        if (!logoLoaded) {
            logoLoaded = true;
            try (InputStream is = getClass().getResourceAsStream("/images/tituloLogo.png")) {
                if (is != null) logoData = ImageDataFactory.create(is.readAllBytes());
            } catch (Exception ex) {
                logoData = null;
            }
        }
        return logoData;
    }

    /** Esquinas inferiores-izquierdas de las 4 posiciones (origen iText = abajo-izq). */
    private static final float[][] POSITIONS = {
        {0,     A6_H},   // 0: superior-izquierda
        {A6_W,  A6_H},   // 1: superior-derecha
        {0,     0   },   // 2: inferior-izquierda
        {A6_W,  0   }    // 3: inferior-derecha
    };

    // ── Punto de entrada ──────────────────────────────────────────────────────
    public void generarEtiquetasA6enA4(List<Electrodomestico> productos, String rutaSalida) throws IOException {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(rutaSalida));
             Document document  = new Document(pdfDoc, PageSize.A4)) {

            document.setMargins(0, 0, 0, 0);
            int posIdx = 0;

            for (Electrodomestico e : productos) {
                if (posIdx == 0) {
                    pdfDoc.addNewPage();
                    dibujarGuias(pdfDoc.getLastPage());
                }
                float[] pos = POSITIONS[posIdx];
                dibujarEtiqueta(pdfDoc.getLastPage(), e, pos[0], pos[1]);
                posIdx = (posIdx + 1) % 4;
            }
        }
    }

    // ─── Dibuja una etiqueta A6 ───────────────────────────────────────────────
    private void dibujarEtiqueta(PdfPage page, Electrodomestico e, float px, float py) {
        float cw = A6_W - 2 * PAD;   // ancho del área de contenido
        float cx = px + PAD;          // X izquierda del contenido

        // ── Bandas de color (cabecera y pie) ──────────────────────────
        float headerBottom = py + A6_H - HEADER_H;
        new PdfCanvas(page)
                .setFillColor(COLOR_DARK)
                .rectangle(px, headerBottom, A6_W, HEADER_H).fill()
                .rectangle(px, py, A6_W, FOOTER_H).fill()
                .release();

        // ── Cabecera: nombre de marca ──────────────────────────────────
        texto(page, cx, headerBottom + 10, cw, HEADER_H - 14,
              brand(e), 26, true, ColorConstants.WHITE, TextAlignment.CENTER);

        // ── Pie: logo ─────────────────────────────────────────────────
        ImageData ld = getLogoData();
        if (ld != null) {
            float origW = ld.getWidth();
            float origH = ld.getHeight();
            float maxW  = cw * 0.70f;
            float maxH  = FOOTER_H - 10f;
            float scale = Math.min(maxW / origW, maxH / origH);
            float lw    = origW * scale;
            float lh    = origH * scale;
            float lx    = px + (A6_W - lw) / 2f;
            float ly    = py + (FOOTER_H - lh) / 2f;
            PdfCanvas imgCanvas = new PdfCanvas(page);
            imgCanvas.addImageWithTransformationMatrix(ld, lw, 0, 0, lh, lx, ly, false);
            imgCanvas.release();
        } else {
            texto(page, cx, py + 10, cw, FOOTER_H - 10,
                  "Moreno", 20, false, ColorConstants.WHITE, TextAlignment.CENTER);
        }

        // ── Contenido: cursores de Y descendentes ─────────────────────
        // curY = borde inferior del próximo elemento
        float curY = headerBottom - PAD;

        // Modelo
        curY -= 18f;
        texto(page, cx, curY, cw, 18f, nvl(e.getModelo()), 13, true, null, TextAlignment.LEFT);

        // "Características"
        curY -= 22f;
        texto(page, cx, curY, cw, 14f, "Características", 11, true, null, TextAlignment.LEFT);

        // Especificaciones (viñetas)
        String raw = e.getEspecificacionesPrincipales();
        String[] specs = (raw != null && !raw.isBlank()) ? raw.split("[;,\n]+") : new String[0];
        curY -= 4f;
        for (int i = 0; i < Math.min(specs.length, 5); i++) {
            String s = specs[i].trim();
            if (s.isEmpty()) continue;
            curY -= 14f;
            texto(page, cx + 8, curY, cw - 8, 14f, "➤  " + s, 9.5f, false, null, TextAlignment.LEFT);
        }

        // Clase Energética
        curY -= 14f;
        float claseH  = 20f;
        float labelW  = 98f;
        texto(page, cx, curY - claseH, labelW, claseH, "Clase Energética:", 9, false, null, TextAlignment.LEFT);
        dibujarFlecha(page, cx + labelW + 4, curY - claseH, nvl(e.getClasificacionEnergetica()), claseH);
        curY -= claseH;

        // ── Pastilla de precio (posición fija desde el pie) ────────────
        float finH  = 11f;
        float finY  = py + FOOTER_H + 6f;
        float pillH = 70f;
        float pillW = cw - 16f;
        float pillX = px + PAD + 8f;
        float pillY = finY + finH + 8f;

        // "FINANCIACIÓN DISPONIBLE"
        texto(page, cx, finY, cw, finH, "FINANCIACIÓN DISPONIBLE", 8, true, null, TextAlignment.LEFT);

        // Fondo de la pastilla (rectángulo redondeado)
        new PdfCanvas(page)
                .setFillColor(COLOR_DARK)
                .roundRectangle(pillX, pillY, pillW, pillH, 33)
                .fill()
                .release();

        // Precio dentro de la pastilla
        texto(page, pillX + 4, pillY + 4, pillW - 8, pillH - 8,
              formatPrecio(e), 34, true, ColorConstants.WHITE, TextAlignment.CENTER);
    }

    // ─── Flecha de clase energética ───────────────────────────────────────────
    private void dibujarFlecha(PdfPage page, float x, float y, String clase, float h) {
        DeviceRgb color = getClaseColor(clase);
        if (color == null) color = new DeviceRgb(0x75, 0x75, 0x75);
        float w   = 55f;
        float tip = 10f;
        new PdfCanvas(page)
                .setFillColor(color)
                .moveTo(x,           y)
                .lineTo(x + w,       y)
                .lineTo(x + w + tip, y + h / 2f)
                .lineTo(x + w,       y + h)
                .lineTo(x,           y + h)
                .closePath()
                .fill()
                .release();
        String letra = clase.equals("—") ? "" : clase;
        texto(page, x, y, w, h, letra, 11, true, ColorConstants.WHITE, TextAlignment.CENTER);
    }

    // ─── Líneas de corte ──────────────────────────────────────────────────────
    private void dibujarGuias(PdfPage page) {
        new PdfCanvas(page)
                .setLineDash(4, 4)
                .setLineWidth(0.5f)
                .moveTo(0,    A6_H).lineTo(A4_W, A6_H)
                .moveTo(A6_W, 0   ).lineTo(A6_W, A4_H)
                .stroke()
                .release();
    }

    // ─── Helper: texto dentro de un rectángulo ────────────────────────────────
    private void texto(PdfPage page, float x, float y, float w, float h,
                       String text, float fontSize, boolean bold,
                       com.itextpdf.kernel.colors.Color color, TextAlignment align) {
        try (Canvas cv = new Canvas(page, new Rectangle(x, y, w, h))) {
            Paragraph p = new Paragraph(text)
                    .setFontSize(fontSize)
                    .setTextAlignment(align)
                    .setMargin(0)
                    .setPadding(0)
                    .setBorder(Border.NO_BORDER);
            if (bold)  p.setBold();
            if (color != null) p.setFontColor(color);
            cv.add(p);
        }
    }

    // ─── Colores por clase energética ─────────────────────────────────────────
    private DeviceRgb getClaseColor(String clase) {
        return switch (clase) {
            case "A" -> new DeviceRgb(0x2e, 0x7d, 0x32);
            case "B" -> new DeviceRgb(0x38, 0x8e, 0x3c);
            case "C" -> new DeviceRgb(0x7c, 0xb3, 0x42);
            case "D" -> new DeviceRgb(0xf9, 0xa8, 0x25);
            case "E" -> new DeviceRgb(0xfb, 0x8c, 0x00);
            case "F" -> new DeviceRgb(0xe6, 0x4a, 0x19);
            case "G" -> new DeviceRgb(0xc6, 0x28, 0x28);
            default  -> null;
        };
    }

    private String brand(Electrodomestico e) {
        return e.getMarca() != null ? e.getMarca().toUpperCase() : "";
    }

    private String formatPrecio(Electrodomestico e) {
        if (e.getPrecio() == null) return "—";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-ES"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(e.getPrecio()) + " €";
    }

    private String nvl(String s) {
        return s != null ? s : "—";
    }
}
