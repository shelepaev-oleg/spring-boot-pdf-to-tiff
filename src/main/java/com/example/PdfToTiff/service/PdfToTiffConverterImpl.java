package com.example.PdfToTiff.service;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codec.TIFFField;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Конвертер PDF в TIFF
 */
@Service
@SuppressWarnings("checkstyle:MagicNumber")
public class PdfToTiffConverterImpl implements PdfToTiffConverter {

    private static Random rn = new Random();

    /**
     * Режим конвертации
     */
    public enum ConvertMode {
        /**
         * ROUND
         */
        ROUND,

        /**
         * DITHER
         */
        DITHER,

        /**
         * NORMALIZED_ROUND
         */
        NORMALIZED_ROUND,

        /**
         * NORMALIZED_DITHER
         */
        NORMALIZED_DITHER
    }

    /**
     * Служебный класс
     */
    private final class PdfImagesIterator implements Iterator<BufferedImage> {

        private final Iterator<PDPage> pdIt;
        private final ConvertMode mode;

        /**
         * Итератор
         * @param pdPages List < {@link PDPage} >
         * @param mode {@link ConvertMode}
         */
        private PdfImagesIterator(final List<PDPage> pdPages, final ConvertMode mode) {
            this.pdIt = pdPages.iterator();
            this.mode = mode;
        }

        /**
         * Есть следующий элемент
         * @return true - есть следующий элемент
         */
        @Override
        public boolean hasNext() {
            return pdIt.hasNext();
        }

        /**
         * Возвращает {@link BufferedImage}
         * @return {@link BufferedImage}
         */
        @Override
        public BufferedImage next() {
            try {
                PDPage page = pdIt.next();
                return preparePage(page.convertToImage(BufferedImage.TYPE_INT_RGB, 500), mode);
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Удаляет
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        /**
         * Конвертация в соответствии с режимом
         * @param page {@link BufferedImage}
         * @param convertMode {@link ConvertMode}
         * @return {@link BufferedImage}
         * @throws NoSuchAlgorithmException {@link NoSuchAlgorithmException}
         */
        private BufferedImage preparePage(final BufferedImage page, final ConvertMode convertMode)
                throws NoSuchAlgorithmException {
            switch (convertMode) {
                case ROUND:
                    return getBinaryImage(page);
                case NORMALIZED_ROUND:
                    return getBinaryImage(normalize(page));
                case NORMALIZED_DITHER:
                    return dither(normalize(page));
                case DITHER:
                    return dither(page);
                default:
                    throw new NoSuchAlgorithmException("unknown conversion mode " + convertMode);
            }
        }

        /**
         * normalize
         * @param image {@link BufferedImage}
         * @return {@link BufferedImage}
         */
        private BufferedImage normalize(final BufferedImage image) {
            return normalize(image, getMedianLum(image), getMinLum(image));
        }

        /**
         * dither
         * @param image {@link BufferedImage}
         * @return {@link BufferedImage}
         */
        @SuppressWarnings("squid:S2164")
        private BufferedImage dither(final BufferedImage image) {
            BufferedImage imRes = new BufferedImage(image.getWidth(), image.getHeight(),
                    BufferedImage.TYPE_BYTE_BINARY);

            for (int i = 0; i < image.getWidth(); i++) {
                for (int j = 0; j < image.getHeight(); j++) {
                    int color = image.getRGB(i, j);
                    imRes.setRGB(i, j, (((color >>> 16) & 0xFF) * 0.21f + ((color >>> 8) & 0xFF) * 0.71f
                            + ((color) & 0xFF) * 0.07f) / 255.0f <= rn.nextFloat() * 0.99f ? 0x000000 : 0xFFFFFF);
                }
            }

            return imRes;
        }

        /**
         * getMedianLum
         * @param image {@link BufferedImage}
         * @return int
         */
        @SuppressWarnings("squid:S2164")
        private int getMedianLum(final BufferedImage image) {
            int[] lums = new int[256];

            for (int i = 0; i < 256; i++) {
                lums[i] = 0;
            }

            for (int i = 0; i < image.getWidth(); i++) {
                for (int j = 0; j < image.getHeight(); j++) {
                    int color = image.getRGB(i, j);
                    int lum = Math.round(((color >>> 16) & 0xFF) * 0.21f
                            + ((color >>> 8) & 0xFF) * 0.71f + ((color) & 0xFF) * 0.07f);
                    lums[lum]++;
                }
            }

            int middle = image.getWidth() * image.getHeight() / 2;
            int sum = 0;

            for (int i = 0; i < 256; i++) {
                sum += lums[i];
                if (sum >= middle) {
                    return i;
                }
            }

            return 0;
        }

        /**
         * getMinLum
         * @param image {@link BufferedImage}
         * @return int
         */
        @SuppressWarnings("squid:S2164")
        private int getMinLum(final BufferedImage image) {
            int res = 255;

            for (int i = 0; i < image.getWidth(); i++) {
                for (int j = 0; j < image.getHeight(); j++) {
                    int color = image.getRGB(i, j);
                    int lum = Math.round(((color >>> 16) & 0xFF) * 0.21f
                            + ((color >>> 8) & 0xFF) * 0.71f + ((color) & 0xFF) * 0.07f);
                    if (lum < res) {
                        res = lum;
                    }
                }
            }

            return res;
        }

        /**
         * normalize
         * @param image {@link BufferedImage}
         * @param whiteLum whiteLum
         * @param blackLum blackLum
         * @return {@link BufferedImage}
         */
        @SuppressWarnings("squid:S2164")
        private BufferedImage normalize(final BufferedImage image, final int whiteLum, final int blackLum) {

            BufferedImage res = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

            for (int i = 0; i < image.getWidth(); i++) {
                for (int j = 0; j < image.getHeight(); j++) {
                    int color = image.getRGB(i, j);
                    int lum = Math.round(((color >>> 16) & 0xFF) * 0.21f
                            + ((color >>> 8) & 0xFF) * 0.71f + ((color) & 0xFF) * 0.07f);

                    if (lum <= blackLum) {
                        res.setRGB(i, j, 0x000000);
                    } else if (lum >= whiteLum) {
                        res.setRGB(i, j, 0xFFFFFF);
                    } else {
                        int resLum =
                                Math.round(((float) lum - (float) blackLum) * 255.0f / ((float) (whiteLum - blackLum)));
                        int rgb = resLum;
                        rgb = (rgb << 8) + resLum;
                        rgb = (rgb << 8) + resLum;
                        res.setRGB(i, j, rgb);
                    }
                }
            }

            return res;
        }

        /**
         * {@link BufferedImage}
         * @param image {@link BufferedImage}
         * @return {@link BufferedImage}
         */
        private BufferedImage getBinaryImage(final BufferedImage image) {
            BufferedImage res = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D g = (Graphics2D) res.getGraphics();
            g.drawImage(image, 0, 0, null);
            return res;
        }
    }

    /**
     * Конвертер pdf -> tiff
     * @param inputStream   pdf
     * @return              tiff
     * @throws IOException  исключение
     */
    @Override
    public InputStream convert(final InputStream inputStream) throws IOException {

        ConvertMode mode = ConvertMode.DITHER;

        PDDocument document = PDDocument.loadNonSeq(inputStream, null);
        List<PDPage> pdPages = document.getDocumentCatalog().getAllPages();

        return convertImagesToTiff(new PdfImagesIterator(pdPages, mode));
    }

    /**
     * Изображение в TIFF
     * @param imagesIt Iterator < {@link BufferedImage} >
     * @return {@link InputStream}
     * @throws IOException исключение
     */
    @SuppressWarnings("squid:S2164")
    private InputStream convertImagesToTiff(final Iterator<BufferedImage> imagesIt) throws IOException {

        TIFFEncodeParam tiffParams = new TIFFEncodeParam();

        ByteArrayOutputStream byteOs = new ByteArrayOutputStream(64 * 1000);

        ImageEncoder encoder = ImageCodec.createImageEncoder("tiff", byteOs, tiffParams);

        tiffParams.setCompression(TIFFEncodeParam.COMPRESSION_GROUP4);

        // Разрешение 300 dpi * 300 dpi
        TIFFField xres = new TIFFField(0x11A, TIFFField.TIFF_RATIONAL, 1, new long[][] {{300, 1}});
        TIFFField yres = new TIFFField(0x11B, TIFFField.TIFF_RATIONAL, 1, new long[][] {{300, 1}});
        tiffParams.setExtraFields(new TIFFField[] {xres, yres});

        BufferedImage firstImage = imagesIt.next();
        if (imagesIt.hasNext()) {
            tiffParams.setExtraImages(imagesIt);
        }
        encoder.encode(firstImage);

        return new ByteArrayInputStream(byteOs.toByteArray());
    }
}
