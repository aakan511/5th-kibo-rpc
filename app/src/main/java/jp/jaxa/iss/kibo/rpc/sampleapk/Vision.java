package jp.jaxa.iss.kibo.rpc.sampleapk;


import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;

import static android.content.ContentValues.TAG;

public final class Vision {
    public static KiboRpcApi api;
    public static Mat camMat;
    public static Mat distortionCoefficients;


    public Vision(KiboRpcApi api){
        this.api = api;
        double[][] navCamIntrinsics = api.getNavCamIntrinsics(); //gets camera distortion
        camMat = new Mat().zeros(3, 3, CvType.CV_64FC(1));//intrinsic camera matrix initializer
        distortionCoefficients = new Mat().zeros(4, 1, CvType.CV_64FC(1)); //distortion coefficient initializer
        for(int r=0; r<3; r++){ //fills intrinsic camera matrix with correct values
            for(int c=0; c<3; c++) {
                camMat.put(r, c, (navCamIntrinsics[0][3*r+c]));
                Log.i(TAG, "camMat[" + r +", " + c + "] = " + camMat.get(r, c));
                Log.i(TAG, "navCamIntrinsics[" + (3*r+c) + "] = " + navCamIntrinsics[0][3*r+c]);
            }
        }
        for(int i=0; i<navCamIntrinsics[1].length-1; i++){ //fills distorition coefficient array with values
            distortionCoefficients.put(i, 0, (navCamIntrinsics[1][i]));
            Log.i(TAG, "distortionCoefficients[" + i + "] = " + distortionCoefficients.get(0,1));
            Log.i(TAG, "navCamIntrinsics[" + i + "] = " + navCamIntrinsics[1][i]);
        }
    }

    public Vision(KiboRpcApi api, Mat camMat, Mat distortionCoefficients){
        this.api = api;
        this.camMat = camMat;
        this.distortionCoefficients = distortionCoefficients;

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
    public static Mat undistort(Mat src){
        Mat dst = new Mat();
        Calib3d.undistort(src, dst, camMat, distortionCoefficients);
//        org.opencv.imgproc.Imgproc.undistort(src, dst, camMat, distortionCoefficients);
        return dst;
    }

}
