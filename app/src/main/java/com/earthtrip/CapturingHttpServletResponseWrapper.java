package com.earthtrip;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

final class CapturingHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private final int captureLimit;
    private final boolean captureSuccessfulPayloads;
    private ByteArrayOutputStream capturedContent;
    private CapturingServletOutputStream outputStream;
    private PrintWriter writer;
    private long totalBytes;

    CapturingHttpServletResponseWrapper(
            HttpServletResponse response, int captureLimit, boolean captureSuccessfulPayloads) {
        super(response);
        this.captureLimit = captureLimit;
        this.captureSuccessfulPayloads = captureSuccessfulPayloads;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called");
        }
        return capturingOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            if (outputStream != null) {
                throw new IllegalStateException("getOutputStream() has already been called");
            }
            Charset charset = responseCharset();
            writer =
                    new PrintWriter(
                            new OutputStreamWriter(capturingOutputStream(), charset), false);
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        flushCapturedWriter();
        super.flushBuffer();
    }

    void flushCapturedWriter() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        if (outputStream != null) {
            outputStream.flush();
        }
    }

    byte[] getCapturedContent() {
        return capturedContent == null ? new byte[0] : capturedContent.toByteArray();
    }

    long getTotalBytes() {
        return totalBytes;
    }

    boolean isOverflowed() {
        return totalBytes > captureLimit;
    }

    private CapturingServletOutputStream capturingOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream =
                    new CapturingServletOutputStream(
                            ((HttpServletResponse) getResponse()).getOutputStream());
        }
        return outputStream;
    }

    private Charset responseCharset() {
        String encoding = getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.ISO_8859_1;
        }
        try {
            return Charset.forName(encoding);
        } catch (RuntimeException ignored) {
            return StandardCharsets.ISO_8859_1;
        }
    }

    private final class CapturingServletOutputStream extends ServletOutputStream {

        private final ServletOutputStream delegate;

        private CapturingServletOutputStream(ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int value) throws IOException {
            delegate.write(value);
            if (shouldCapture() && totalBytes < captureLimit) {
                capturedContent().write(value);
            }
            totalBytes++;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            delegate.write(bytes, offset, length);
            int remaining = (int) Math.max(0, captureLimit - totalBytes);
            int capturedLength = shouldCapture() ? Math.min(remaining, length) : 0;
            if (capturedLength > 0) {
                capturedContent().write(bytes, offset, capturedLength);
            }
            totalBytes += length;
        }

        private boolean shouldCapture() {
            return captureSuccessfulPayloads
                    || CapturingHttpServletResponseWrapper.this.getStatus() >= 400;
        }

        private ByteArrayOutputStream capturedContent() {
            if (capturedContent == null) {
                capturedContent = new ByteArrayOutputStream(Math.min(captureLimit, 1024));
            }
            return capturedContent;
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }
    }
}
