package com.earthtrip.platform.adapter.out.export;

import com.earthtrip.platform.application.port.out.PdfDocumentRendererPort;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

@Component
class PdfBoxDocumentRendererAdapter implements PdfDocumentRendererPort {

    private static final String FONT_RESOURCE = "/fonts/NotoSansKR-VF.ttf";
    private static final float PAGE_MARGIN = 48f;
    private static final float FOOTER_Y = 28f;

    @Override
    public byte[] render(String documentTitle, List<TextLine> lines) {
        try (PDDocument document = new PDDocument();
                InputStream fontStream = fontStream()) {
            PDDocumentInformation information = document.getDocumentInformation();
            information.setTitle(documentTitle);
            information.setCreator("Earth Trip");

            PDType0Font font = PDType0Font.load(document, fontStream, true);
            try (PageWriter writer = new PageWriter(document, font)) {
                for (TextLine line : lines) {
                    writer.write(line);
                }
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("PDF 내보내기를 생성할 수 없습니다.", exception);
        }
    }

    private static InputStream fontStream() {
        InputStream stream = PdfBoxDocumentRendererAdapter.class.getResourceAsStream(FONT_RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("PDF 한글 폰트 리소스를 찾을 수 없습니다.");
        }
        return stream;
    }

    private static final class PageWriter implements AutoCloseable {

        private final PDDocument document;
        private final PDType0Font font;
        private final float textWidth;
        private PDPageContentStream stream;
        private float y;
        private int pageNumber;

        private PageWriter(PDDocument document, PDType0Font font) throws IOException {
            this.document = document;
            this.font = font;
            this.textWidth = PDRectangle.A4.getWidth() - (PAGE_MARGIN * 2);
            nextPage();
        }

        private void write(TextLine line) throws IOException {
            TextStyle style = line.style();
            if (style == TextStyle.SPACER) {
                ensureSpace(style.lineHeight());
                y -= style.lineHeight();
                return;
            }
            List<String> wrapped = wrap(supportedText(line.text()), style.fontSize());
            ensureSpace(style.topSpace() + style.lineHeight());
            y -= style.topSpace();
            for (String value : wrapped) {
                ensureSpace(style.lineHeight());
                stream.beginText();
                stream.setFont(font, style.fontSize());
                stream.newLineAtOffset(PAGE_MARGIN, y);
                stream.showText(value);
                stream.endText();
                y -= style.lineHeight();
            }
        }

        private List<String> wrap(String value, float fontSize) throws IOException {
            if (value.isBlank()) {
                return List.of("");
            }
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (int offset = 0; offset < value.length(); ) {
                int codePoint = value.codePointAt(offset);
                String character = new String(Character.toChars(codePoint));
                String candidate = current + character;
                if (!current.isEmpty() && width(candidate, fontSize) > textWidth) {
                    lines.add(current.toString().stripTrailing());
                    current.setLength(0);
                    if (!character.isBlank()) {
                        current.append(character);
                    }
                } else {
                    current.append(character);
                }
                offset += Character.charCount(codePoint);
            }
            if (!current.isEmpty()) {
                lines.add(current.toString().stripTrailing());
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        private String supportedText(String value) throws IOException {
            StringBuilder supported = new StringBuilder();
            for (int offset = 0; offset < value.length(); ) {
                int codePoint = value.codePointAt(offset);
                String character = new String(Character.toChars(codePoint));
                try {
                    font.getStringWidth(character);
                    supported.append(character);
                } catch (IllegalArgumentException exception) {
                    supported.append('?');
                }
                offset += Character.charCount(codePoint);
            }
            return supported.toString();
        }

        private float width(String value, float fontSize) throws IOException {
            return font.getStringWidth(value) / 1000f * fontSize;
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < PAGE_MARGIN) {
                nextPage();
            }
        }

        private void nextPage() throws IOException {
            closeCurrentPage();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - PAGE_MARGIN;
            pageNumber++;
        }

        private void closeCurrentPage() throws IOException {
            if (stream == null) {
                return;
            }
            stream.beginText();
            stream.setFont(font, 8f);
            stream.newLineAtOffset(PAGE_MARGIN, FOOTER_Y);
            stream.showText("Earth Trip  ·  " + pageNumber);
            stream.endText();
            stream.close();
            stream = null;
        }

        @Override
        public void close() throws IOException {
            closeCurrentPage();
        }
    }
}
