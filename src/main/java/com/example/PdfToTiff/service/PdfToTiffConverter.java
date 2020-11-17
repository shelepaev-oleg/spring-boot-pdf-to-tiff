package com.example.PdfToTiff.service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Конвертер PDF в TIFF
 */
public interface PdfToTiffConverter {

    /**
     * Конвертер pdf -> tiff
     * @param inputStream   pdf
     * @return              tiff
     * @throws IOException  исключение
     */
    InputStream convert(InputStream inputStream) throws IOException;
}
