package jp.jaxa.iss.kibo.rpc.sampleapk;


import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;

import org.opencv.features2d.SIFT;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.BFMatcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;


import static android.content.ContentValues.TAG;
import static org.opencv.core.Core.bitwise_not;

public final class Vision {
    public static KiboRpcApi api;
    public static Mat camMat;
    public static Mat distortionCoefficients;
    public static String[] categories = {"beaker", "top", "wrench", "hammer", "kapton_tape", "screwdriver", "pipette", "thermometer", "watch", "goggle"};
    public static SIFT sift = SIFT.create();
    public static BFMatcher matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_L1, true);
    public static MatOfKeyPoint[] keyPoints;
    public static Mat[] descriptors;
    public static Dictionary dict = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);

    public Vision(KiboRpcApi api, Context context){
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

        keyPoints = new MatOfKeyPoint[categories.length];
        descriptors = new Mat[categories.length];
        AssetManager assetManager = context.getAssets();
        //AssetManager manager = AssetManager.getSystem();
        for (int i = 0; i < categories.length; i++) {
            try {
                InputStream curr = assetManager.open(categories[i] + ".png");
                Bitmap bmp = BitmapFactory.decodeStream(curr);

                Mat img = new Mat();
                Utils.bitmapToMat(bmp, img);

                keyPoints[i] = new MatOfKeyPoint();
                descriptors[i] = new Mat();

                sift.detectAndCompute(img, null, keyPoints[i], descriptors[i]);

                curr.close();
            } catch (IOException e) {
                Log.i("BUGBUGBUG", e.getMessage());
            }
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

    public static double get_min(Mat descriptor, int index) {
        if (index < 0 || index >= categories.length) {
            throw new IndexOutOfBoundsException("Index out of bounds... you know the deal");
        }
        if (descriptors[index] == null){
            throw new IllegalArgumentException("Uhoh descriptors not generating properly");
        }
//        MatOfKeyPoint pts = new MatOfKeyPoint();
//        Mat descriptor = new Mat();
//        sift.detectAndCompute(img, null, pts, descriptor);

        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptors[index], descriptor, matches);

        DMatch[] matches1 = matches.toArray();
        if (matches1.length <= 1) {
            return Double.MAX_VALUE;
        }
        double[] min = {matches1[0].distance, matches1[1].distance};
        if (min[0] > min[1]) {
            double temp = min[0];
            min[0] = min[1];
            min[1] = temp;
        }

        for (int i = 2; i < matches1.length; i++) {
            if (matches1[i].distance < min[0]) {
                min[1] = min[0];
                min[0] = matches1[i].distance;
            } else if(matches1[i].distance < min[1]) {
                min[1] = matches1[i].distance;
            }
        }

        return (min[0] + min[1]) / 2;
    }

    public static String classify(Mat img) {
        MatOfKeyPoint pts = new MatOfKeyPoint();
        Mat descriptor = new Mat();
        sift.detectAndCompute(img, null, pts, descriptor);


        double min = Double.MAX_VALUE;
        String minCategory = "blank";
        for (int i = 0; i < categories.length; i++) {
            double temp = get_min(descriptor, i);
            if (temp < min) {
                min = temp;
                minCategory = categories[i];
            }
        }
        return minCategory;
    }

    public static String[] findAruco(Mat img) {
        List<Mat> corners = new ArrayList<>();
        Mat ids = new Mat();

        Aruco.detectMarkers(img, dict, corners, ids);

        if (corners.size() > 0) {
            String[] result = new String[corners.size()];

            for (int i = 0; i < corners.size(); i++) {

                Mat clean = arucoCrop(img, corners.get(i));
                String category = classify(clean);
                int numObjects = countObjects(clean);

                Log.i("findAruco", numObjects + ", " + category + ", " + (ids.get(i, 0)[0] - 100));
                api.setAreaInfo((int) ids.get(i, 0)[0] - 100, category, numObjects);
                result[i] = category;
            }
            return result;
        } else {
            return null;
        }
    }

    public static Mat arucoCrop(Mat in, Mat corners) {
        Log.i("arucoCrop", "width: " + corners.size().width + ", height: " + corners.height());
        return in;
    }

    public static int countObjects(Mat in) {
        Mat thresh = new Mat();
        org.opencv.imgproc.Imgproc.threshold(in, thresh, 200, 255, Imgproc.THRESH_BINARY);
        bitwise_not(thresh, thresh);

        api.saveMatImage(thresh, "invertedThresh.png");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat heirarchy = new Mat();
        Imgproc.findContours(thresh, contours, heirarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Imgproc.drawContours(in, contours, -1, new Scalar(0, 0, 255));

        return contours.size();
    }
}
