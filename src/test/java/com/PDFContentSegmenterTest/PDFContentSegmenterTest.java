package com.PDFContentSegmenterTest;

import com.PDFContentSegmenter.PDFContentSegmenter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class PDFContentSegmenterTest {

    @Test
    public void testSegmentPDF() throws IOException {
        String inputFilePath = "src/test/resources/sample01.pdf";
        String outputDir = "src/test/resources/output";
        int numberOfSegments = 3;

        PDFContentSegmenter.segmentPDF(inputFilePath, outputDir, numberOfSegments);

        File outputDirectory = new File(outputDir);
        assertTrue(outputDirectory.exists());
        assertTrue(outputDirectory.isDirectory());

        File[] segmentedFiles = outputDirectory.listFiles((dir, name) -> name.startsWith("segment_"));
        assertTrue(segmentedFiles != null);
        assertTrue(segmentedFiles.length == numberOfSegments);
    }
}

