package jp.jaxa.iss.kibo.rpc.sampleapk;


import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
//import org.opencv.objdetect.QRCodeDetector;
//import org.tensorflow.lite.support.image.TensorImage;
//import org.tensorflow.lite.task.core.BaseOptions;
//import org.tensorflow.lite.task.vision.detector.Detection;
//import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;

import static android.content.ContentValues.TAG;

public final class Vision {
    public static KiboRpcApi api;

//    public static ObjectDetector.ObjectDetectorOptions options;
//
//    public static ObjectDetector objectDetector;

    public static String modelFile = "ogModel.tflite";

    public Vision(KiboRpcApi api, Context context) throws IOException {
        this.api = api;
//        options =
//                ObjectDetector.ObjectDetectorOptions.builder()
//                        .setBaseOptions(BaseOptions.builder().useGpu().build())
//                        .setMaxResults(1)
//                        .build();
//        objectDetector =
//                ObjectDetector.createFromFileAndOptions(
//                       context , modelFile, options);

    }

    public static Mat cropPage(Mat image)
    {
        Rect rectCrop;// = new Rect(img.width()/4, img.height()/4, img.width()/2, img.height()/2);
//        Mat image = new Mat(img, rectCrop);
//        Core.flip(image, image, -1);
//        api.saveMatImage(image, "QRCodes_Undistort_Flip.png");

        Mat thresh = new Mat();
        Imgproc.threshold(image, thresh, 230, 255, Imgproc.THRESH_BINARY);
        api.saveMatImage(thresh, "simpleThresh.png");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat heirarchy = new Mat();
        Imgproc.findContours(thresh, contours, heirarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.size() == 0) { //could save a few lines later
            for (int threshold = 225; threshold > 100 && contours.size() == 0; threshold -= 5) {
                Imgproc.threshold(image, thresh, threshold, 255, Imgproc.THRESH_BINARY);
                Imgproc.findContours(thresh, contours, heirarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            }
        }

        rectCrop = Imgproc.boundingRect(contours.get(0));
        Mat img_contourCrop = new Mat(image, rectCrop);

        return img_contourCrop;
    }
    public static Mat undistort(Mat src, Mat camMat, Mat distortionCoefficients){
        Mat dst = new Mat();
        //org.opencv.imgproc.Imgproc.undistort(src, dst, camMat, distortionCoefficients);
        return dst;
    }

//    public static List<Detection> detect(Bitmap image) {
//        if (api == null || options == null || objectDetector == null || modelFile == null) {
//            throw new IllegalArgumentException("Vision was not started properly");
//        }
//        TensorImage img = TensorImage.fromBitmap(image);
//
//        List<Detection> results = objectDetector.detect(img);
//
//        Canvas c = new Canvas(image);
//        Paint paint = new Paint();
//        paint.setColor(Color.RED);
//        paint.setStrokeWidth(5);
//
//        for (Detection d : results) {
////            float left = d.getBoundingBox().left;
////            float top = d.getBoundingBox().top;
////            float right = d.getBoundingBox().right;
////            float bottom = d.getBoundingBox().bottom;
//
//            c.drawRect(d.getBoundingBox(), paint);
//            Log.i(TAG, d.toString());
//
//        }
//        api.saveBitmapImage(image, "testingDetection");
//        return results;
//    }
}
