package jp.jaxa.iss.kibo.rpc.sampleapk;


import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;

import java.util.ArrayList;
import java.util.List;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;

import static android.content.ContentValues.TAG;

public final class Vision {
    public static KiboRpcApi api;

    private Vision() {}

    public static Mat cropPage(Mat img)
    {
        Rect rectCrop = new Rect(img.width()/4, img.height()/4, img.width()/2, img.height()/2);
        Mat image = new Mat(img, rectCrop);
        Core.flip(image, image, -1);
        QRCodeDetector decoder = new QRCodeDetector();
        api.saveMatImage(image, "QRCodes_Undistort_Flip.png");

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
    public Mat undistort(Mat src, Mat camMat, Mat distortionCoefficients){
        Mat dst = new Mat();
        //org.opencv.imgproc.Imgproc.undistort(src, dst, camMat, distortionCoefficients);
        return dst;
    }
}
