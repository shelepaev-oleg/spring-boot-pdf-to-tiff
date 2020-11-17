package com.example.PdfToTiff.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertNotNull;

/**
 * Тестирование {@link PdfToTiffConverter}
 */
public class PdfToTiffConverterTest {

    @InjectMocks
    private PdfToTiffConverterImpl pdfToTiffConverter;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Тестирование {@link PdfToTiffConverterImpl#convert(java.io.InputStream)}
     * @throws IOException {@link IOException}
     */
    @Test
    @SuppressWarnings("squid:S2699")
    public void convertTest() throws IOException {
        InputStream inputStreamPdf = getClass().getResourceAsStream("/test.pdf");

        InputStream inputStreamTiff = pdfToTiffConverter.convert(inputStreamPdf);
        assertNotNull(inputStreamTiff);

        // Код ниже сохранит полученный tiff-файл
        byte[] buffer = new byte[inputStreamTiff.available()];
        inputStreamTiff.read(buffer);
        File targetFile = new File("src/test/resources/test.tiff");
        OutputStream outStream = new FileOutputStream(targetFile);
        outStream.write(buffer);
    }
}
