package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.ArucoDetector;
import org.opencv.objdetect.Dictionary;

import org.opencv.features2d.SIFT;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.BFMatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;


import static android.content.ContentValues.TAG;
import static org.opencv.calib3d.Calib3d.solvePnP;
import static org.opencv.core.Core.bitwise_not;
import static org.opencv.core.Core.min;
import static org.opencv.core.Core.minMaxLoc;
import static org.opencv.objdetect.Objdetect.DICT_5X5_250;
import static org.opencv.objdetect.Objdetect.getPredefinedDictionary;

public final class Vision {
    public static KiboRpcApi api;
    public static Mat camMat;
    public static Mat distortionCoefficients;
    public static Mat rearCamMat;
    public static Mat rearDistortionCoefficients;
    public static String[] categories = {"beaker", "top", "wrench", "hammer", "kapton_tape", "screwdriver", "pipette", "thermometer", "watch", "goggle"};
    public static int[] categoryIds = {R.drawable.beaker, R.drawable.top , R.drawable.wrench, R.drawable.hammer , R.drawable.kapton_tape , R.drawable.screwdriver , R.drawable.pipette , R.drawable.thermometer , R.drawable.watch , R.drawable.goggle};
    public static SIFT sift = SIFT.create();
    public static DescriptorMatcher matcher;
    public static MatOfKeyPoint[] keyPoints;
    public static Mat[] descriptors;
    public static Mat[] targetDescriptors = new Mat[4];
    public static Dictionary dict = getPredefinedDictionary(DICT_5X5_250);
    public static ArucoDetector arucoDetector = new ArucoDetector(dict);
    public static String[] targetCategories = new String[4];
    public static double[] ratios = {0.6670481487821136, 0.4268646448961206, 0.26110240847935834, 0.26040570295263815, 0.6203050939687318, 0.18730484126368557, 0.21930514990471245, 0.3004298433365385, 0.4955997263661711, 0.4553772661089809};//{0.9310563647676213, 0.927241607216102, 1.018599276357519, 1.0655839142379049, 0.8938588723136022, 0.8079078257333207, 0.7626616207030666, 0.7752607379017146, 0.886756275652506, 0.8558393198753002}; //ratio of contour perimeter / bounding circle circumference
    public static int currTarget = 1;

    public Vision(KiboRpcApi api, Context context){
        OpenCVLoader.initLocal();
        this.api = api;

        double[][] rearCamIntrinsics = api.getDockCamIntrinsics(); //gets camera distortion
        rearCamMat = new Mat().zeros(3, 3, CvType.CV_64FC(1));//intrinsic camera matrix initializer
        rearDistortionCoefficients = new Mat().zeros(4, 1, CvType.CV_64FC(1)); //distortion coefficient initializer
        for(int r=0; r<3; r++){ //fills intrinsic camera matrix with correct values
            for(int c=0; c<3; c++) {
                rearCamMat.put(r, c, (rearCamIntrinsics[0][3*r+c]));
//                Log.i(TAG, "camMat[" + r +", " + c + "] = " + rearCamMat.get(r, c));
//                Log.i(TAG, "navCamIntrinsics[" + (3*r+c) + "] = " + rearCamIntrinsics[0][3*r+c]);
            }
        }
        for(int i=0; i<rearCamIntrinsics[1].length-1; i++){ //fills distorition coefficient array with values
            rearDistortionCoefficients.put(i, 0, (rearCamIntrinsics[1][i]));
//            Log.i(TAG, "distortionCoefficients[" + i + "] = " + rearDistortionCoefficients.get(0,1));
//            Log.i(TAG, "navCamIntrinsics[" + i + "] = " + navCamIntrinsics[1][i]);
        }

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


        matcher = BFMatcher.create(BFMatcher.BRUTEFORCE, true);

        keyPoints = new MatOfKeyPoint[categories.length];
        descriptors = new Mat[categories.length];

        for (int i = 0; i < categories.length; i++) {
            try {
                Mat in = Utils.loadResource(context, categoryIds[i]);

                Mat img = new Mat();
                Imgproc.cvtColor(in, img, Imgproc.COLOR_RGB2GRAY);

                keyPoints[i] = new MatOfKeyPoint();
                descriptors[i] = new Mat();

                sift.detect(img, keyPoints[i]);
                sift.compute(img, keyPoints[i], descriptors[i]);
            } catch (IOException e) {
                Log.i("BUGBUGBUG", e.getMessage());
            }
        }

    }

    public Vision(KiboRpcApi api) {
        OpenCVLoader.initLocal();
        this.api = api;

        double[][] rearCamIntrinsics = api.getDockCamIntrinsics(); //gets camera distortion
        rearCamMat = new Mat().zeros(3, 3, CvType.CV_64FC(1));//intrinsic camera matrix initializer
        rearDistortionCoefficients = new Mat().zeros(4, 1, CvType.CV_64FC(1)); //distortion coefficient initializer
        for(int r=0; r<3; r++){ //fills intrinsic camera matrix with correct values
            for(int c=0; c<3; c++) {
                rearCamMat.put(r, c, (rearCamIntrinsics[0][3*r+c]));
//                Log.i(TAG, "camMat[" + r +", " + c + "] = " + rearCamMat.get(r, c));
//                Log.i(TAG, "navCamIntrinsics[" + (3*r+c) + "] = " + rearCamIntrinsics[0][3*r+c]);
            }
        }
        for(int i=0; i<rearCamIntrinsics[1].length-1; i++){ //fills distorition coefficient array with values
            rearDistortionCoefficients.put(i, 0, (rearCamIntrinsics[1][i]));
//            Log.i(TAG, "distortionCoefficients[" + i + "] = " + rearDistortionCoefficients.get(0,1));
//            Log.i(TAG, "navCamIntrinsics[" + i + "] = " + navCamIntrinsics[1][i]);
        }

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


    public static Mat undistort(Mat src){
        Mat dst = new Mat();
        Calib3d.undistort(src, dst, camMat, distortionCoefficients);
        return dst;
    }

    public static Mat undistort(Mat src, boolean navCam){
        Mat dst = new Mat();
        if (navCam) {
            Calib3d.undistort(src, dst, camMat, distortionCoefficients);
        } else {
            Calib3d.undistort(src, dst, rearCamMat, rearDistortionCoefficients);
        }
        return dst;
    }

    public static double get_min(Mat descriptor, Mat oDescriptor) {
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(oDescriptor, descriptor, matches);

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

    public static double get_min(Mat descriptor, int index) {
        if (index < 0 || index >= categories.length) {
            throw new IndexOutOfBoundsException("Index out of bounds... you know the deal");
        }
        if (descriptors[index] == null || descriptors[index].empty()){
            Log.i("SiftError", "Uhoh descriptors not generating properly");
            return Double.MAX_VALUE;
        }

        MatOfDMatch matches = new MatOfDMatch();
        if (descriptor == null || descriptor.empty()) {
            Log.i("SiftError", "other descriptors not generating properly");
            return Double.MAX_VALUE;
        }

        return get_min(descriptor, descriptors[index]);
    }

    public static int classify(Mat descriptor) {

        double min = Double.MAX_VALUE;
        //String minCategory = "blank";
        int minCategory = -1;
        for (int i = 0; i < categories.length; i++) {
            double temp = get_min(descriptor, i);
            if (temp < min) {
                min = temp;
                //minCategory = categories[i];
                minCategory = i;
            }
            Log.i("classify", categories[i] + " : " + temp);
        }
        return minCategory;
    }


    public static Mat getDescriptors(Mat img) {
        MatOfKeyPoint pts = new MatOfKeyPoint();
        Mat descriptor = new Mat();

        sift.detect(img, pts);
        sift.compute(img, pts, descriptor);

        return descriptor;
    }

    public static int findTarget(Mat descriptors) {
        int minTarget = 1;
        double minDistance = get_min(descriptors, targetDescriptors[minTarget - 1]);

        for (int curr = 2; curr <= 4; curr++) {
            double temp = get_min(descriptors, targetDescriptors[curr - 1]);
            if (temp < minDistance) {
                minDistance = temp;
                minTarget = curr;
            }
        }
        return minTarget;
    }


    public static String[] findAruco(Mat img) {
        List<Mat> corners = new ArrayList<>();
        Mat ids = new Mat();

        arucoDetector.detectMarkers(img, corners, ids);

        if (corners.size() > 0) {
            String[] result = new String[corners.size()];
            boolean allowReport = true;
            for (int i = 0; i < corners.size(); i++) {

                Mat clean = arucoCrop(img, corners.get(i));
                Mat descriptor = getDescriptors(clean);
                int categoryNum = classify(descriptor);
                if (categoryNum == -1) {
                    Log.i("ERRORERRORERROR", "categoryNum was -1 please investigate");
                    categoryNum = 0;
                }
                String category = (ids.get(i, 0)[0] == 100) ? "blank" : categories[categoryNum];
                int numObjects = (ids.get(i, 0)[0] == 100) ? 1 : countObjects(clean);

                if (allowReport && (int) ids.get(i, 0)[0] - 100 == currTarget) {
                    targetCategories[currTarget - 1] = category;
                    targetDescriptors[currTarget - 1] = descriptor;

//                    api.setAreaInfo((int) ids.get(i, 0)[0] - 100, category, numObjects);
                    allowReport = false;
                    currTarget++;
                }

                if (ids.get(i, 0)[0] == 100) {
                    int target = findTarget(descriptor);
                    Log.i("FinalLocation", "Final target : " + target);
                    currTarget = target;
                    return new String[]{"" + target};
                }

                Log.i("findAruco", numObjects + ", " + category + ", " + (ids.get(i, 0)[0] - 100));
                api.saveMatImage(clean, "target_" + ((int) (ids.get(i, 0)[0] - 100)) + "_" + randName() + "_cropped.png");
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
        //double[] bottomRight = corner.get(0, 2);

        double[] deltaHorizontal = {topLeft[0] - topRight[0], topLeft[1] - topRight[1]};
        double[] deltaVertical = {topLeft[0] - bottomLeft[0], topLeft[1] - bottomLeft[1]};
        double distance = .05;
        double scale = 1.00;

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

        Mat invert = new Mat();
        Core.bitwise_not(in, invert);
        Mat cropped = new Mat();
        Core.bitwise_and(invert, mask, cropped);
        Core.bitwise_not(cropped, cropped);
        return cropped;

    }

    public static int countObjects(Mat in) {
        Mat thresh = new Mat();
        Imgproc.threshold(in, thresh, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        bitwise_not(thresh, thresh);
//        api.saveMatImage(thresh, "invertedThresh_" + currTarget + ".png");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat heirarchy = new Mat();
        Imgproc.findContours(thresh, contours, heirarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        return contours.size();

    }

    public static gov.nasa.arc.astrobee.types.Point arucoOffset(Mat img, int target) {
        ArrayList<Mat> corners = new ArrayList<>();
        Mat ids = new Mat();
        Mat rvec = new Mat();
        Mat tvec = new Mat();


        arucoDetector.detectMarkers(img, corners, ids);
        Aruco.estimatePoseSingleMarkers(corners, .05f, camMat, distortionCoefficients, rvec, tvec);

        for (int i = 0; i < corners.size(); i++) {
            if (ids.get(i, 0)[0] - 100 == target) {
                double[] pt = tvec.row(i).get(0, 0);
                pt[2] = 0;

                if (target == 1) {
                    return new gov.nasa.arc.astrobee.types.Point(pt[0], -pt[2], pt[1]);
                } else if (target == 2 || target == 3) {
                    return new gov.nasa.arc.astrobee.types.Point(pt[1], pt[0], -pt[2]);
                } else if (target == 4) {
                    return new gov.nasa.arc.astrobee.types.Point(-pt[2], pt[0], -pt[1]);
                }
            }
        }
        Log.i("ERROR", currTarget + " target's aruco marker not found(arucoOffset)");
        return new gov.nasa.arc.astrobee.types.Point();
    }

    public static gov.nasa.arc.astrobee.types.Point arucoOffsetDebug(Mat img, int target) {
        ArrayList<Mat> corners = new ArrayList<>();
        Mat ids = new Mat();
        Mat rvec = new Mat();
        Mat tvec = new Mat();


        arucoDetector.detectMarkers(img, corners, ids);
        Aruco.estimatePoseSingleMarkers(corners, .05f, camMat, distortionCoefficients, rvec, tvec);

        for (int i = 0; i < corners.size(); i++) {
            if (ids.get(i, 0)[0] - 100 == target) {

                Mat corner = corners.get(i);
                double[] topLeft =  corner.get(0, 0);
                double[] topRight = corner.get(0, 1);
                double[] bottomLeft = corner.get(0, 3);
                //double[] bottomRight = corner.get(0, 2);

                double[] deltaHorizontal = {topLeft[0] - topRight[0], topLeft[1] - topRight[1]};
                double[] deltaVertical = {topLeft[0] - bottomLeft[0], topLeft[1] - bottomLeft[1]};

                double distHorizontal = Math.sqrt(deltaHorizontal[0] * deltaHorizontal[0] + deltaHorizontal[1] * deltaHorizontal[1]);
                double distVertical = Math.sqrt(deltaVertical[0] * deltaVertical[0] + deltaVertical[1] * deltaVertical[1]);

                deltaHorizontal[0] = deltaHorizontal[0] / distHorizontal;
                deltaHorizontal[1] = deltaHorizontal[1] / distHorizontal;

                deltaVertical[0] = deltaVertical[0] / distVertical;
                deltaVertical[1] = deltaVertical[1] / distVertical;

                double[] centerAdj = {(.1375 * deltaHorizontal[0]) + (-.0375 * deltaVertical[0]), (.1375 * deltaHorizontal[0]) + (-.0375 * deltaVertical[0])};

                double[] pt = tvec.row(i).get(0, 0);
                pt[2] = 0;
                pt[0] = pt[0] + centerAdj[0];
                pt[1] = pt[1] + centerAdj[1];

                if (target == 1) {
                    return new gov.nasa.arc.astrobee.types.Point(pt[0], -pt[2], pt[1]);
                } else if (target == 2 || target == 3) {
                    return new gov.nasa.arc.astrobee.types.Point(pt[1], pt[0], -pt[2]);
                } else{
                    return new gov.nasa.arc.astrobee.types.Point(-pt[2], pt[0], -pt[1]);
                }
            }
        }
        Log.i("ERROR", currTarget + " target's aruco marker not found");
        return new gov.nasa.arc.astrobee.types.Point();
    }

    public static double distance (gov.nasa.arc.astrobee.types.Point p) {
        if (p == null) {
            Log.i("ERRORERROR", "point passed into distance is null");
            return 0;
        }
        return Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY() + p.getZ() * p.getZ());
    }


    public static String randName () {
        int random = (int) (Math.random() * 10000000);
        return "" + random;
    }
}
