package com.earthtrip.platform.adapter.out.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.earthtrip.platform.application.port.out.PdfDocumentRendererPort.TextLine;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfBoxDocumentRendererAdapterTest {

    private final PdfBoxDocumentRendererAdapter renderer = new PdfBoxDocumentRendererAdapter();

    @Test
    void embedsKoreanTextAndCreatesAdditionalPagesForLongExports() throws Exception {
        List<TextLine> lines = new java.util.ArrayList<>();
        lines.add(TextLine.title("도쿄와 교토 여름 여행"));
        lines.add(TextLine.subtitle("Earth Trip 여행 일정"));
        for (int index = 1; index <= 100; index++) {
            lines.add(TextLine.body(index + "일차  ·  맛집과 박물관 방문 일정"));
        }

        byte[] pdf = renderer.render("도쿄와 교토 여름 여행", lines);

        assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(document.getDocumentInformation().getTitle()).isEqualTo("도쿄와 교토 여름 여행");
            assertThat(text)
                    .contains("도쿄와 교토 여름 여행")
                    .contains("맛집과 박물관 방문 일정")
                    .doesNotContain("????");
        }
    }
}
