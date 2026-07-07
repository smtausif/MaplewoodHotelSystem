package ca.senecacollege.hotelreservation.hotelreservation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A tiny, dependency-free PDF generator.
 *
 * It writes a single-page PDF using the built-in Courier font, enough to
 * export tabular reports without pulling in a library like PDFBox or iText.
 * Not a general PDF library — just enough for these reports.
 */
public final class SimplePdf {

    private SimplePdf() {
    }

    public static void write(File file, String title, List<String> lines) throws Exception {
        // Build the page content stream (text)
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 16 Tf\n50 780 Td\n(").append(escape(title)).append(") Tj\nET\n");

        int y = 750;
        content.append("BT\n/F1 10 Tf\n50 ").append(y).append(" Td\n");
        boolean first = true;
        for (String line : lines) {
            if (!first) {
                content.append("0 -14 Td\n");
            }
            content.append("(").append(escape(line)).append(") Tj\n");
            first = false;
            y -= 14;
            if (y < 40) {
                break; // one page only
            }
        }
        content.append("ET\n");

        byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

        // Assemble PDF objects
        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                + "/Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>");
        objects.add("<< /Length " + contentBytes.length + " >>\nstream\n"
                + content + "endstream");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>");

        try (OutputStream out = new FileOutputStream(file)) {
            StringBuilder pdf = new StringBuilder();
            List<Integer> offsets = new ArrayList<>();

            pdf.append("%PDF-1.4\n");
            for (int i = 0; i < objects.size(); i++) {
                offsets.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
                pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
            }

            int xrefPos = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
            pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
            pdf.append("0000000000 65535 f \n");
            for (int off : offsets) {
                pdf.append(String.format("%010d 00000 n \n", off));
            }
            pdf.append("trailer\n<< /Size ").append(objects.size() + 1)
                    .append(" /Root 1 0 R >>\nstartxref\n").append(xrefPos).append("\n%%EOF");

            out.write(pdf.toString().getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}