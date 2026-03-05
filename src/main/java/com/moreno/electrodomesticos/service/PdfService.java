package com.moreno.electrodomesticos.service;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.moreno.electrodomesticos.model.Electrodomestico;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Genera un PDF A4 con 4 etiquetas A6 (2 columnas × 2 filas) por página.
 *
 * Coordenadas A4 (595.28 × 841.89 pt):
 *   A6 = 297.64 × 420.94 pt
 *
 *   Etiqueta 0 (sup-izq): x=0,       y=420.94
 *   Etiqueta 1 (sup-der): x=297.64,  y=420.94
 *   Etiqueta 2 (inf-izq): x=0,       y=0
 *   Etiqueta 3 (inf-der): x=297.64,  y=0
 */
@Service
public class PdfService {

    private static final float A4_W    = PageSize.A4.getWidth();   // 595.28
    private static final float A4_H    = PageSize.A4.getHeight();  // 841.89
    private static final float A6_W    = A4_W / 2;                 // 297.64
    private static final float A6_H    = A4_H / 2;                 // 420.94
    private static final float PADDING = 10f;

    /** Esquinas inferiores-izquierdas de las 4 posiciones (iText origin = bottom-left). */
    private static final float[][] POSITIONS = {
        {0,      A6_H},   // 0: superior-izquierda
        {A6_W,   A6_H},   // 1: superior-derecha
        {0,      0},      // 2: inferior-izquierda
        {A6_W,   0}       // 3: inferior-derecha
    };

    public void generarEtiquetasA6enA4(List<Electrodomestico> productos, String rutaSalida) throws IOException {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(rutaSalida));
             Document document  = new Document(pdfDoc, PageSize.A4)) {

            document.setMargins(0, 0, 0, 0);

            int posIdx = 0;

            for (int i = 0; i < productos.size(); i++) {
                if (posIdx == 0) {
                    // Nueva página (la primera se crea automáticamente)
                    if (i > 0) pdfDoc.addNewPage();
                    dibujarGuias(pdfDoc.getLastPage());
                }

                Electrodomestico e = productos.get(i);
                float[] pos = POSITIONS[posIdx];
                dibujarEtiqueta(pdfDoc, e, pos[0], pos[1]);

                posIdx = (posIdx + 1) % 4;
            }
        }
    }

    // ─── Draw one A6 label ────────────────────────────────────────────────────
    private void dibujarEtiqueta(PdfDocument pdfDoc, Electrodomestico e, float x, float y) {
        Rectangle rect = new Rectangle(
                x + PADDING,
                y + PADDING,
                A6_W - 2 * PADDING,
                A6_H - 2 * PADDING);

        try (Canvas canvas = new Canvas(pdfDoc.getLastPage(), rect)) {
            // ── Header: Marca + Modelo ────────────────────────────────────────
            Paragraph header = new Paragraph()
                    .add(new Text(e.getMarca() + "\n").setBold().setFontSize(14))
                    .add(new Text(e.getModelo()).setFontSize(11))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(6);
            canvas.add(header);

            // ── Data table ────────────────────────────────────────────────────
            Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                    .setWidth(UnitValue.createPercentValue(100));

            addRow(table, "Precio",        formatPrecio(e));
            addRow(table, "Clase Energ.",  nvl(e.getClasificacionEnergetica()));
            addRow(table, "Dimensiones",   nvl(e.getDimensiones()));
            addRow(table, "Tipo",          nvl(e.getTipo()));

            canvas.add(table);

            // ── Specs block ───────────────────────────────────────────────────
            if (e.getEspecificacionesPrincipales() != null && !e.getEspecificacionesPrincipales().isBlank()) {
                Paragraph specs = new Paragraph(e.getEspecificacionesPrincipales())
                        .setFontSize(7.5f)
                        .setItalic()
                        .setMarginTop(6)
                        .setTextAlignment(TextAlignment.LEFT);
                canvas.add(specs);
            }
        }
    }

    // ─── Draw cut guides (dashed lines) ──────────────────────────────────────
    private void dibujarGuias(com.itextpdf.kernel.pdf.PdfPage page) {
        PdfCanvas pdfCanvas = new PdfCanvas(page);
        pdfCanvas.setLineDash(4, 4)
                 .setLineWidth(0.5f)
                 // Horizontal centre line
                 .moveTo(0, A6_H).lineTo(A4_W, A6_H)
                 // Vertical centre line
                 .moveTo(A6_W, 0).lineTo(A6_W, A4_H)
                 .stroke()
                 .release();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private void addRow(Table table, String label, String value) {
        Cell labelCell = new Cell().add(new Paragraph(label).setBold().setFontSize(8))
                .setBorderRight(null).setPadding(2);
        Cell valueCell = new Cell().add(new Paragraph(value).setFontSize(8))
                .setBorderLeft(null).setPadding(2);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatPrecio(Electrodomestico e) {
        return e.getPrecio() != null ? e.getPrecio().toPlainString() + " €" : "—";
    }

    private String nvl(String s) {
        return s != null ? s : "—";
    }
}
