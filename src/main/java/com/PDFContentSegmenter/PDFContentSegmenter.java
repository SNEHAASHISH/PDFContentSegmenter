package com.PDFContentSegmenter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PDFContentSegmenter {

    public static void segmentPDF(String inputFilePath, String outputDir, int numberOfSegments) throws IOException {
        PDDocument document = PDDocument.load(new File(inputFilePath));
        PDFTextStripperByArea stripper = new PDFTextStripperByArea();

        List<TextPosition> allTextPositions = new ArrayList<>();
        List<Integer> pageIndices = new ArrayList<>();

        for (int page = 0; page < document.getNumberOfPages(); page++) {
            PDPage pdPage = document.getPage(page);
            stripper.extractTextPositions(pdPage);
            allTextPositions.addAll(stripper.getTextPositions());

            for (int i = 0; i < stripper.getTextPositions().size(); i++) {
                pageIndices.add(page);
            }
        }

        if (allTextPositions.isEmpty()) {
            System.err.println("No text positions were extracted from the document.");
            return;
        }

        List<WhitespaceGap> whitespaceGaps = getWhitespaceHeights(allTextPositions, pageIndices);
        if (whitespaceGaps.size() < numberOfSegments - 1) {
            throw new IllegalArgumentException("Not enough whitespace to create the requested number of segments");
        }

        List<WhitespaceGap> largestWhitespaces = findLargestWhitespaces(whitespaceGaps, numberOfSegments - 1);
        splitDocument(document, largestWhitespaces, outputDir);

        document.close();
    }

    private static List<WhitespaceGap> getWhitespaceHeights(List<TextPosition> textPositions, List<Integer> pageIndices) {
        List<WhitespaceGap> whitespaceGaps = new ArrayList<>();
        float lastBottomY = 0;
        int lastPageIndex = -1;

        for (int i = 0; i < textPositions.size(); i++) {
            float currentTopY = textPositions.get(i).getY();
            int currentPageIndex = pageIndices.get(i);

            if (i == 0) {
                lastBottomY = currentTopY + textPositions.get(i).getHeight();
                lastPageIndex = currentPageIndex;
                continue;
            }

            float whitespaceHeight = (currentPageIndex == lastPageIndex) ? currentTopY - lastBottomY : currentTopY;
            if (whitespaceHeight > 5) { // Only consider significant whitespaces
                whitespaceGaps.add(new WhitespaceGap(whitespaceHeight, currentPageIndex, i));
            }

            lastBottomY = currentTopY + textPositions.get(i).getHeight();
            lastPageIndex = currentPageIndex;
        }
        return whitespaceGaps;
    }

    private static List<WhitespaceGap> findLargestWhitespaces(List<WhitespaceGap> whitespaceGaps, int numberOfCuts) {
        Collections.sort(whitespaceGaps, (wg1, wg2) -> Float.compare(wg2.height, wg1.height));
        return whitespaceGaps.subList(0, Math.min(numberOfCuts, whitespaceGaps.size()));
    }

    private static void splitDocument(PDDocument document, List<WhitespaceGap> largestWhitespaces, String outputDir) throws IOException {
        int currentSegment = 1;
        PDDocument newDocument = new PDDocument();
        List<PDPage> currentSegmentPages = new ArrayList<>();
        int currentPageIndex = 0;
        int currentGapIndex = 0;

        for (WhitespaceGap gap : largestWhitespaces) {
            while (currentPageIndex <= gap.pageIndex) {
                PDPage pdPage = document.getPage(currentPageIndex);
                currentSegmentPages.add(pdPage);

                if (currentPageIndex == gap.pageIndex) {
                    saveSegment(newDocument, currentSegmentPages, outputDir, currentSegment);
                    newDocument = new PDDocument();
                    currentSegment++;
                    currentSegmentPages.clear();
                    currentGapIndex++;
                }

                currentPageIndex++;
            }
        }

        // Add remaining pages to the last segment
        while (currentPageIndex < document.getNumberOfPages()) {
            PDPage pdPage = document.getPage(currentPageIndex);
            currentSegmentPages.add(pdPage);
            currentPageIndex++;
        }

        if (!currentSegmentPages.isEmpty()) {
            saveSegment(newDocument, currentSegmentPages, outputDir, currentSegment);
        }
    }

    private static void saveSegment(PDDocument newDocument, List<PDPage> currentSegmentPages, String outputDir, int currentSegment) throws IOException {
        for (PDPage segmentPage : currentSegmentPages) {
            newDocument.addPage(segmentPage);
        }
        newDocument.save(new File(outputDir, "segment_" + currentSegment + ".pdf"));
        newDocument.close();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: PDFContentSegmenter <input-pdf> <output-directory> <number-of-segments>");
            System.exit(1);
        }

        String inputFilePath = args[0];
        String outputDir = args[1];
        int numberOfSegments = Integer.parseInt(args[2]);

        try {
            segmentPDF(inputFilePath, outputDir, numberOfSegments);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class WhitespaceGap {
        float height;
        int pageIndex;
        int index;

        WhitespaceGap(float height, int pageIndex, int index) {
            this.height = height;
            this.pageIndex = pageIndex;
            this.index = index;
        }

        @Override
        public String toString() {
            return "WhitespaceGap{" +
                    "height=" + height +
                    ", pageIndex=" + pageIndex +
                    ", index=" + index +
                    '}';
        }
    }
}
