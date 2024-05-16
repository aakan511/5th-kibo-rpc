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

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.ArucoDetector;
import org.opencv.objdetect.Dictionary;
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
import static org.opencv.objdetect.Objdetect.DICT_5X5_250;
import static org.opencv.objdetect.Objdetect.getPredefinedDictionary;

public final class Vision {
    public static KiboRpcApi api;
    public static Mat camMat;
    public static Mat distortionCoefficients;
    public static String[] categories = {"beaker", "top", "wrench", "hammer", "kapton_tape", "screwdriver", "pipette", "thermometer", "watch", "goggle"};
    public static int[] categoryIds = {R.drawable.beaker, R.drawable.top , R.drawable.wrench, R.drawable.hammer , R.drawable.kapton_tape , R.drawable.screwdriver , R.drawable.pipette , R.drawable.thermometer , R.drawable.watch , R.drawable.goggle};
    public static SIFT sift = SIFT.create(10);
    public static DescriptorMatcher matcher;
    public static MatOfKeyPoint[] keyPoints;
    public static Mat[] descriptors;
    public static Dictionary dict = getPredefinedDictionary(DICT_5X5_250);

    public Vision(KiboRpcApi api, Context context){
        OpenCVLoader.initLocal();
//        if (!OpenCVLoader.initDebug())
//            Log.e("OpenCV", "Unable to load OpenCV!");
//        else
//            Log.d("OpenCV", "OpenCV loaded Successfully!");
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

        matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_L1, true);

        keyPoints = new MatOfKeyPoint[categories.length];
        descriptors = new Mat[categories.length];

        for (int i = 0; i < categories.length; i++) {
            try {
//                InputStream curr = assetManager.open(categories[i] + ".png");
//                Bitmap bmp = BitmapFactory.decodeStream(curr);
                Mat in = Utils.loadResource(context, categoryIds[i]);


                Mat img = new Mat();
//                Utils.bitmapToMat(bmp, img);
                Imgproc.cvtColor(in, img, Imgproc.COLOR_RGB2GRAY);

                keyPoints[i] = new MatOfKeyPoint();
                descriptors[i] = new Mat();

                sift.detect(img, keyPoints[i]);
                sift.compute(img, keyPoints[i], descriptors[i]);
//                sift.detectAndCompute(img, null, keyPoints[i], descriptors[i]);

//                curr.close();
            } catch (IOException e) {
                Log.i("BUGBUGBUG", e.getMessage());
            }
        }

    }


    public static Mat undistort(Mat src){
        Mat dst = new Mat();
        Calib3d.undistort(src, dst, camMat, distortionCoefficients);
        return dst;
    }

    public static double get_min(Mat descriptor, int index) {
        if (index < 0 || index >= categories.length) {
            throw new IndexOutOfBoundsException("Index out of bounds... you know the deal");
        }
        if (descriptors[index] == null || descriptors[index].empty()){
            Log.i("SiftError", "Uhoh descriptors not generating properly");
            return Double.MAX_VALUE;
        }
//        MatOfKeyPoint pts = new MatOfKeyPoint();
//        Mat descriptor = new Mat();
//        sift.detectAndCompute(img, null, pts, descriptor);

//        Mat descriptor = new Mat(descriptor1.size(), descriptor1.type());
//        descriptor1.copyTo(descriptor1);


        MatOfDMatch matches = new MatOfDMatch();

        //Mat qDescriptor = new Mat(descriptors[index].size(), descriptors[index].type());
        //descriptors[index].copyTo(qDescriptor);
        if (descriptor == null || descriptor.empty()) {
            Log.i("SiftError", "other descriptors not generating properly");
            return Double.MAX_VALUE;
        }

        Log.i("descriptorsArray", "type : " + descriptors[index].type());
        Log.i("descriptorsArray", "size : " + descriptors[index].size());
        Log.i("descriptor", "type : " + descriptor.type());
        Log.i("descriptor", "size : " + descriptor.size());
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

        sift.detect(img, pts);
        sift.compute(img, pts, descriptor);


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

        ArucoDetector arucoDetector = new ArucoDetector(dict);
        arucoDetector.detectMarkers(img, corners, ids);
//        Aruco.detectMarkers(img, dict, corners, ids);

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

    public static Mat arucoCrop(Mat in, Mat corner) {
        Log.i("arucoCrop", "width: " + corner.size().width + ", height: " + corner.height());

        double[] topLeft =  corner.get(0, 0);
        double[] topRight = corner.get(0, 1);
        double[] bottomLeft = corner.get(0, 3);
        double[] bottomRight = corner.get(0, 2);

        double[] deltaHorizontal = {topLeft[0] - topRight[0], topLeft[1] - topRight[1]};
        double[] deltaVertical = {topLeft[0] - bottomLeft[0], topLeft[1] - bottomLeft[1]};
        double distance = .05;
        double scale = 1.08;

        double[] dirHorizontal = {deltaHorizontal[0] / distance, deltaHorizontal[1] / distance};
        double[] dirVertical = {deltaVertical[0] / distance, deltaVertical[1] / distance};

        Point topLeftBound = new Point( scale * (dirHorizontal[0] * .2075 + dirVertical[0] * .0125) + topLeft[0],
                scale * (dirHorizontal[1] * .2075 + dirVertical[1] * .0125) + topLeft[1]);
        Point topRightBound = new Point(scale * (dirHorizontal[0] * .01 + dirVertical[0] * .0125) + topLeft[0],
                scale * (dirHorizontal[1] * .01 + dirVertical[1] * .0125) + topLeft[1]);
        Point bottomRightBound = new Point(scale * (-1 * dirVertical[0] * .0875 + dirHorizontal[0] * .01) + bottomLeft[0],
                scale * (-1 * dirVertical[1] * .0875 + dirHorizontal[1] * .01) + bottomLeft[1]);
        Point bottomLeftBound = new Point(scale * (-1 * dirVertical[0] * .15) + topLeftBound.x,
                scale * (-1 * dirVertical[1] * .15) + topLeftBound.y);
        List<MatOfPoint> points = new ArrayList<>();
        points.add(new MatOfPoint(topLeftBound, topRightBound, bottomRightBound, bottomLeftBound));

        Mat mask = Mat.zeros(in.size(), in.type());
        Imgproc.fillPoly(mask, points, new Scalar(255));
        api.saveMatImage(mask, "mask.png");

        Core.bitwise_not(in, in);
        Mat cropped = new Mat();
        Core.bitwise_and(in, mask, cropped);
        Core.bitwise_not(cropped, cropped);
        api.saveMatImage(cropped, "cropped.png");

        return cropped;

    }

    public static int countObjects(Mat in) {
        Mat thresh = new Mat();
        org.opencv.imgproc.Imgproc.threshold(in, thresh, 200, 255, Imgproc.THRESH_BINARY);
        bitwise_not(thresh, thresh);

        api.saveMatImage(thresh, "invertedThresh.png");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat heirarchy = new Mat();
        Imgproc.findContours(thresh, contours, heirarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Imgproc.drawContours(in, contours, -1, new Scalar(255));

        return contours.size();
    }
}
