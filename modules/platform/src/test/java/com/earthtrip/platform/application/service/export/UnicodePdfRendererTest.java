package com.earthtrip.platform.application.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class UnicodePdfRendererTest {

    private final UnicodePdfRenderer renderer = new UnicodePdfRenderer();

    @Test
    void embedsKoreanTextAndCreatesAdditionalPagesForLongExports() throws Exception {
        List<UnicodePdfRenderer.TextLine> lines = new java.util.ArrayList<>();
        lines.add(UnicodePdfRenderer.TextLine.title("도쿄와 교토 여름 여행"));
        lines.add(UnicodePdfRenderer.TextLine.subtitle("Earth Trip 여행 일정"));
        for (int index = 1; index <= 100; index++) {
            lines.add(UnicodePdfRenderer.TextLine.body(
                index + "일차  ·  맛집과 박물관 방문 일정"
            ));
        }

        byte[] pdf = renderer.render("도쿄와 교토 여름 여행", lines);

        assertThat(pdf).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(document.getDocumentInformation().getTitle())
                .isEqualTo("도쿄와 교토 여름 여행");
            assertThat(text)
                .contains("도쿄와 교토 여름 여행")
                .contains("맛집과 박물관 방문 일정")
                .doesNotContain("????");
        }
    }
}
