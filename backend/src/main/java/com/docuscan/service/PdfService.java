package com.docuscan.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PdfService: Converts ALL pages of a PDF into PNG images.
 *
 * Previously we only did page 1.
 * Now we process every page, which means:
 * - ALL text from the entire document gets OCR'd
 * - The user sees all pages in the viewer
 */
@Service
public class PdfService {

    /**
     * Converts ALL pages of a PDF to individual PNG images.
     *
     * @param pdfFile    The uploaded PDF file
     * @param outputDir  Directory to save the page images
     * @param fileId     Unique ID prefix for filenames
     * @return List of image files, one per page
     */
    public List<File> convertAllPagesToImages(File pdfFile, String outputDir, String fileId) throws IOException {
        List<File> pageImages = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            System.out.println("📄 PDF has " + totalPages + " page(s)");

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                // Render each page at 300 DPI
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300, ImageType.RGB);

                String imageName = fileId + "_page" + (pageIndex + 1) + ".png";
                File outputFile = new File(outputDir, imageName);
                ImageIO.write(image, "png", outputFile);

                pageImages.add(outputFile);
                System.out.println("   🖼️ Page " + (pageIndex + 1) + "/" + totalPages
                        + " → " + imageName
                        + " (" + image.getWidth() + "x" + image.getHeight() + ")");
            }
        }

        return pageImages;
    }

    /**
     * Gets the number of pages in a PDF.
     */
    public int getPageCount(File pdfFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            return document.getNumberOfPages();
        }
    }
}
