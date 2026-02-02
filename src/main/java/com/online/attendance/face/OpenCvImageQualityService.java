package com.online.attendance.face;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OpenCvImageQualityService {

    private static final int MIN_WIDTH = 240;
    private static final int MIN_HEIGHT = 240;

    private static final double MIN_BRIGHTNESS = 40.0;
    private static final double MAX_BRIGHTNESS = 220.0;

    private static final double MIN_LAPLACIAN_VARIANCE = 50.0;

    public String validate(MultipartFile image) {
        try {
            byte[] bytes = image.getBytes();
            if (bytes.length == 0) {
                return "Invalid image. Please use a clear photo.";
            }

            BytePointer bp = new BytePointer(bytes);
            Mat buf = new Mat(1, bytes.length, opencv_core.CV_8U, bp);

            Mat mat = opencv_imgcodecs.imdecode(buf, opencv_imgcodecs.IMREAD_COLOR);
            if (mat == null || mat.empty()) {
                return "Invalid image. Please use a clear photo.";
            }

            int width = mat.cols();
            int height = mat.rows();
            if (width < MIN_WIDTH || height < MIN_HEIGHT) {
                return "Image too small. Please take a closer photo.";
            }

            Mat gray = new Mat();
            opencv_imgproc.cvtColor(mat, gray, opencv_imgproc.COLOR_BGR2GRAY);

            Scalar mean = opencv_core.mean(gray);
            double brightness = mean.get(0);
            if (brightness < MIN_BRIGHTNESS) {
                return "Image too dark. Please improve lighting and try again.";
            }
            if (brightness > MAX_BRIGHTNESS) {
                return "Image too bright. Please avoid strong backlight/flash and try again.";
            }

            Mat lap = new Mat();
            opencv_imgproc.Laplacian(gray, lap, opencv_core.CV_64F);

            Mat lapMean = new Mat();
            Mat lapStdDev = new Mat();
            opencv_core.meanStdDev(lap, lapMean, lapStdDev);
            double std;
            try (DoubleIndexer idx = lapStdDev.createIndexer()) {
                std = idx.get(0);
            }
            double variance = std * std;

            if (variance < MIN_LAPLACIAN_VARIANCE) {
                return "Image is too blurry. Please hold still and try again.";
            }

            return null;
        } catch (Exception ex) {
            return "Failed to process image. Please try another photo.";
        }
    }
}
