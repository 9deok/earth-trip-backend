package com.earthtrip.platform.application.port.out;

import java.util.List;

public interface PdfDocumentRendererPort {

    byte[] render(String documentTitle, List<TextLine> lines);

    enum TextStyle {
        TITLE(22f, 31f, 0f),
        SUBTITLE(13f, 21f, 0f),
        SECTION(15f, 24f, 13f),
        BODY(10.5f, 17f, 0f),
        CAPTION(9f, 15f, 0f),
        SPACER(1f, 10f, 0f);

        private final float fontSize;
        private final float lineHeight;
        private final float topSpace;

        TextStyle(float fontSize, float lineHeight, float topSpace) {
            this.fontSize = fontSize;
            this.lineHeight = lineHeight;
            this.topSpace = topSpace;
        }

        public float fontSize() {
            return fontSize;
        }

        public float lineHeight() {
            return lineHeight;
        }

        public float topSpace() {
            return topSpace;
        }
    }

    record TextLine(String text, TextStyle style) {

        public TextLine {
            text = text == null ? "" : text;
        }

        public static TextLine title(String text) {
            return new TextLine(text, TextStyle.TITLE);
        }

        public static TextLine subtitle(String text) {
            return new TextLine(text, TextStyle.SUBTITLE);
        }

        public static TextLine section(String text) {
            return new TextLine(text, TextStyle.SECTION);
        }

        public static TextLine body(String text) {
            return new TextLine(text, TextStyle.BODY);
        }

        public static TextLine caption(String text) {
            return new TextLine(text, TextStyle.CAPTION);
        }

        public static TextLine spacer() {
            return new TextLine("", TextStyle.SPACER);
        }
    }
}
