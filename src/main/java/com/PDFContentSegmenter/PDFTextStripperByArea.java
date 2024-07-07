package com.PDFContentSegmenter;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class PDFTextStripperByArea extends PDFTextStripper {

    private final List<TextPosition> textPositions = new ArrayList<>();
    private final StringWriter dummyWriter = new StringWriter(); // Dummy writer to avoid null output

    public PDFTextStripperByArea() throws IOException {
        super();
        setStartPage(0);
        setEndPage(Integer.MAX_VALUE);
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        textPositions.add(text);
    }

    public void extractTextPositions(PDPage page) throws IOException {
        textPositions.clear();
        setStartPage(getCurrentPageNo());
        setEndPage(getCurrentPageNo());
        this.output = dummyWriter; // Set dummy writer
        super.processPage(page);
    }

    public List<TextPosition> getTextPositions() {
        return new ArrayList<>(textPositions);
    }
}
