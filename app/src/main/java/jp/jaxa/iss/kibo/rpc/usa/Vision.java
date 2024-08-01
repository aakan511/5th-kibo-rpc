package jp.jaxa.iss.kibo.rpc.usa;

import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.aruco.Aruco;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.ArucoDetector;
import org.opencv.objdetect.Dictionary;

import java.util.ArrayList;
import java.util.List;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;

import static android.content.ContentValues.TAG;
import static org.opencv.objdetect.Objdetect.DICT_5X5_250;
import static org.opencv.objdetect.Objdetect.getPredefinedDictionary;

public final class Vision {
    public static KiboRpcApi api;
    public static Mat camMat;
    public static Mat distortionCoefficients;
    public static Mat rearCamMat;
    public static Mat rearDistortionCoefficients;
    public static Dictionary dict = getPredefinedDictionary(DICT_5X5_250);
    public static ArucoDetector arucoDetector = new ArucoDetector(dict);
    public static int currTarget = 1;

    public Vision(KiboRpcApi api) {
        OpenCVLoader.initLocal();
        this.api = api;

        double[][] rearCamIntrinsics = api.getDockCamIntrinsics(); //gets camera distortion
        rearCamMat = new Mat().zeros(3, 3, CvType.CV_64FC(1));//intrinsic camera matrix initializer
        rearDistortionCoefficients = new Mat().zeros(4, 1, CvType.CV_64FC(1)); //distortion coefficient initializer
        for(int r=0; r<3; r++){ //fills intrinsic camera matrix with correct values
            for(int c=0; c<3; c++) {
                rearCamMat.put(r, c, (rearCamIntrinsics[0][3*r+c]));
            }
        }
        for(int i=0; i<rearCamIntrinsics[1].length-1; i++){ //fills distorition coefficient array with values
            rearDistortionCoefficients.put(i, 0, (rearCamIntrinsics[1][i]));
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

    public static Mat undistortRear(Mat src){
        Mat dst = new Mat();
        Calib3d.undistort(src, dst, rearCamMat, rearDistortionCoefficients);
        return dst;
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
        double scale = 1.10; //1.03

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
        MatOfPoint rectanglePoints = new MatOfPoint(topLeftBound, topRightBound, bottomRightBound, bottomLeftBound);
        points.add(rectanglePoints);

        Mat mask = Mat.zeros(in.size(), in.type());
        Imgproc.fillPoly(mask, points, new Scalar(255));

        Mat invert = new Mat();
        Core.bitwise_not(in, invert);
        Mat cropped = new Mat();
        Core.bitwise_and(invert, mask, cropped);
        Core.bitwise_not(cropped, cropped);

        return cropped;
    }

    public static gov.nasa.arc.astrobee.types.Point[] arucoOffsetCenter(Mat img, int target) {
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

                double[] deltaHorizontal = {topLeft[0] - topRight[0], topLeft[1] - topRight[1]};
                double[] deltaVertical = {topLeft[0] - bottomLeft[0], topLeft[1] - bottomLeft[1]};

                double distHorizontal = Math.sqrt(deltaHorizontal[0] * deltaHorizontal[0] + deltaHorizontal[1] * deltaHorizontal[1]);
                double distVertical = Math.sqrt(deltaVertical[0] * deltaVertical[0] + deltaVertical[1] * deltaVertical[1]);

                deltaHorizontal[0] = deltaHorizontal[0] / distHorizontal;
                deltaHorizontal[1] = deltaHorizontal[1] / distHorizontal;

                deltaVertical[0] = deltaVertical[0] / distVertical;
                deltaVertical[1] = deltaVertical[1] / distVertical;

                double[] centerAdj = {(.1375 * deltaHorizontal[0]) + (-.0375 * deltaVertical[0]), (.1375 * deltaHorizontal[1]) + (-.0375 * deltaVertical[1])};

                double[] pt = tvec.row(i).get(0, 0);
                pt[0] = pt[0] + centerAdj[0];
                pt[1] = pt[1] + centerAdj[1];
                gov.nasa.arc.astrobee.types.Point dist =  new gov.nasa.arc.astrobee.types.Point(pt[0], pt[1], pt[2]);

                pt[2] = 0;

                if (target == 1) {
                    return new gov.nasa.arc.astrobee.types.Point[]{new gov.nasa.arc.astrobee.types.Point(pt[0], -pt[2], pt[1]), dist};
                } else if (target == 2 || target == 3) {
                    return new gov.nasa.arc.astrobee.types.Point[]{new gov.nasa.arc.astrobee.types.Point(pt[1], pt[0], -pt[2]), dist};
                } else{
                    return new gov.nasa.arc.astrobee.types.Point[]{new gov.nasa.arc.astrobee.types.Point(-pt[2], -pt[1], -pt[0]), dist};
                }
            }
        }
        Log.i("ERROR", currTarget + " target's aruco marker not found");
        return new gov.nasa.arc.astrobee.types.Point[]{new gov.nasa.arc.astrobee.types.Point()};
    }


    public static double distance(gov.nasa.arc.astrobee.types.Point p) {
        if (p == null) {
            Log.i("ERRORERROR", "point passed into distance is null");
            return 0;

        }
        return Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY() + p.getZ() * p.getZ());
    }

    public static double angle(gov.nasa.arc.astrobee.types.Point p) {
        double distXY = distance (new gov.nasa.arc.astrobee.types.Point(p.getX(), p.getY(), 0));
        double angle = Math.toDegrees((Math.atan(distXY/p.getZ())));

        return angle;
    }

    public static boolean targetItemReadjust(gov.nasa.arc.astrobee.types.Point p) {
        double dist = distance(p);
        double angle = angle(p);
        boolean result = angle >= 24 || dist >= .85;

        Log.i("TargetItemStats", "distance : " + dist + ", angle : " + angle + ", will be readjusting : " + result);

        return result;
    }


    public static String randName () {
        int random = (int) (Math.random() * 10000000);
        return "" + random;
    }

    public static ArucoDetection waitForTarget(int seconds){
        int duration = seconds * 1000;
        long startTime = System.currentTimeMillis();
        api.flashlightControlFront(.05f);
        while (System.currentTimeMillis() - startTime <= duration) {
            Mat image = api.getMatNavCam();
//            ArrayList corners = new ArrayList<>();
//            Mat ids = new Mat();
//            arucoDetector.detectMarkers(image, corners, ids);
            image = undistort(image);
            ArucoDetection arucoDetection = new ArucoDetection(image, arucoDetector);

            if (!arucoDetection.corners.isEmpty()) {
//                image = undistort(image);
                api.flashlightControlFront(0.00f);
                Log.i("Vision", "Target image detected: " + System.currentTimeMillis() + ", " + startTime);
                return arucoDetection;

            }

//            try {
//                Thread.sleep(250);
//            } catch (InterruptedException e) {
//                Log.i("astronautItemWait", "wait failed");
//                e.printStackTrace();
//            }
            Movement.wait(.05);
        }
        return null;
    }
}

class ArucoDetection {
    public ArrayList<Mat> corners;
    public Mat ids;
    public Mat image;

    public ArucoDetection(Mat image, ArucoDetector arucoDetector) {
        this.image = image;
        corners = new ArrayList<>();
        ids = new Mat();
        arucoDetector.detectMarkers(image, corners, ids);
    }
}
