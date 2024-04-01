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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class YourService extends KiboRpcService {

    final int LOOP_MAX=5;
    final String TAG = this.getClass().getSimpleName();
    final float markerLength = 0.05f;

    @Override
    protected void runPlan1(){


        // the mission starts
        api.startMission();

        double[][] navCamIntrinsics = api.getNavCamIntrinsics(); //gets camera distortion
        Mat camMat = new Mat().zeros(3, 3, CvType.CV_64FC(1));//intrinsic camera matrix initializer
        Mat distortionCoefficients = new Mat().zeros(4, 1, CvType.CV_64FC(1)); //distortion coefficient initializer
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

        Point p1 = new Point(11f,-10f, 5.2f);
        Point p2 = new Point(10.5f,-9.3f, 5.2f);
        Point p3 = new Point(10.9f,-8.925f, 5.2f);
        Point p4 = new Point(11f,-8.3f, 5.2f);
        Point p5 = new Point(10.9f,-7.9f, 5.2f);
        Point p6 = new Point(11f,-7.1f, 4.6f);
        Point p7 = new Point(10.9f,-7.9f, 5.2f);
        Point p8 = new Point(11.13f, -6.76f, 4.96f);
        Quaternion q = new Quaternion(0f, 0f, 0f, 0f);
        Quaternion q2 = new Quaternion(0f, -1f, 0f, 0f);

        // Move to area1 and take a picture
        moveAstrobee(p1, q, 'n', true);
        Mat img = api.getMatNavCam();
        Mat image2 = undistort(img, camMat, distortionCoefficients);
        api.saveMatImage(image2, "PlaneArea1Unoriented.png");

        // Move to area2 and take a picture
        moveAstrobee(p2, q2, 'n', true);
        moveAstrobee(p3, q2, 'n', true);
        Mat A2img = api.getMatNavCam();
        Mat A2image2 = undistort(A2img, camMat, distortionCoefficients);
        api.saveMatImage(A2image2, "PlaneArea2.png");

        // Move to area3 and take a picture
        moveAstrobee(p4, q2, 'n', true);
        moveAstrobee(p5, q2, 'n', true);
        Mat A3img = api.getMatNavCam();
        Mat A3image2 = undistort(A3img, camMat, distortionCoefficients);
        api.saveMatImage(A3image2, "PlaneArea3.png");

        // Move to area4 and take a picture
        moveAstrobee(p6, q, 'n', true);
        moveAstrobee(p7, q, 'n', true);
        Mat A4img = api.getMatNavCam();
        Mat A4image2 = undistort(A4img, camMat, distortionCoefficients);
        api.saveMatImage(A4image2, "PlaneArea4Unoriented.png");

        // Move to the astronaut and report mission finished
        moveAstrobee(p8,q,'n', true);
        api.reportMissionCompletion("Foobar");
    }

    @Override
    protected void runPlan2(){
        // write your plan 2 here
    }

    @Override
    protected void runPlan3(){
        // write your plan 3 here
    }


    protected Kinematics.Confidence moveAstrobee(Point point, Quaternion quaternion, char moveType, Boolean printRobotPosition)
    {
        //Qua_x 0.7071068 Astrobee spin right
        //QUa_x -0.7071068 Astrobee spin left
        //Qua_y  1    Astrobee look up
        //Qua_y -1    Astrobee look down
        //Qua_w  1    Astrobee turn right
        //Qua_W -1    Astrobee turn left

        final boolean DO_FOR_R = false; // Do not repeatedly try for relative move

        Result result;

        double dX, dY, dZ;
        float dOX,dOY,dOZ,dOW;

        Point currentPoint ;

        Point startPoint ;
        Point targetPoint ;

        Quaternion currentQuaternion1;
        Kinematics.Confidence currentPositionConfidence;
        Kinematics kinematics = null;

        currentPositionConfidence = Kinematics.Confidence.GOOD;
        final double [] kIZ1_min_data = {10.3, -10.2, 4.32};
        final double [] kIZ1_max_data = {11.44, -6, 5.57};
        final double [] kIZ2_min_data = {9.5, -10.5, 4.02};
        final double [] kIZ2_max_data = {10.5 ,-9.6, 4.8};

        final Vector kIZ2_min = new Vector(kIZ2_min_data);
        final Vector kIZ2_max = new Vector(kIZ2_max_data);
        final Vector kIZ1_min = new Vector(kIZ1_min_data);
        final Vector kIZ1_max = new Vector(kIZ1_max_data);

        Vector moveToPoint = new Vector(point);
        if(moveToPoint.distanceTo(kIZ1_max)<0.025){
            point = moveToPoint.minusScalar(0.025).toPoint();
            Log.i(TAG,"Restricted movement due to KIZ 1 max violation");
        }
        if(kIZ1_min.distanceTo(moveToPoint)<0.025){
            point = moveToPoint.minusScalar(-0.025).toPoint();
            Log.i(TAG,"Restricted movement due to KIZ 1 min violation");
        }

        startPoint = api.getRobotKinematics().getPosition();
        currentPoint = startPoint;
        if(moveType=='R') {
            targetPoint = new Point(startPoint.getX()+ point.getX(), startPoint.getY()+point.getY(),
                    startPoint.getZ()+point.getZ());
            result = api.relativeMoveTo(point, quaternion, printRobotPosition);
        }else{
            targetPoint=point;
            result = api.moveTo(point, quaternion, printRobotPosition);
            Log.i(TAG,"Moved");
        }
        int noOfTry = 0;
        if(result.hasSucceeded()){
            kinematics = api.getRobotKinematics();
            if(kinematics.getConfidence() == Kinematics.Confidence.GOOD){
                currentPoint = kinematics.getPosition();
                if(currentPoint.getX() != point.getX() ||
                        currentPoint.getY() != point.getY() ||
                        currentPoint.getZ() != point.getZ()){
                    currentPositionConfidence = Kinematics.Confidence.POOR;
                }
            }
        }
        while((!result.hasSucceeded() || currentPositionConfidence == Kinematics.Confidence.POOR) && noOfTry < LOOP_MAX ){

            if(moveType=='R') {
                if( DO_FOR_R == true) {
                    point = new Point(targetPoint.getX() - currentPoint.getX(), targetPoint.getY() - currentPoint.getY(),
                            targetPoint.getZ() - currentPoint.getZ());
                    result = api.relativeMoveTo(point, quaternion, printRobotPosition);
                }
            }else{
                // point = new Point(point.getX()-0.015f,point.getY()-0.015f,point.getZ()-0.015f);
                result = api.moveTo(point, quaternion, printRobotPosition);
                Log.i(TAG,"Moving Loop " + noOfTry);
            }
            if(result.hasSucceeded()){
                kinematics = api.getRobotKinematics();
                if(kinematics.getConfidence() == Kinematics.Confidence.GOOD){
                    currentPoint = kinematics.getPosition();
                    if(currentPoint.getX() != point.getX() ||
                            currentPoint.getY() != point.getY() ||
                            currentPoint.getZ() != point.getZ()){
                        currentPositionConfidence = Kinematics.Confidence.POOR;
                    }
                }
            }
            ++noOfTry;
        }

        return (Kinematics.Confidence.GOOD);
    }
    /************************************************************/
    /* computeQuaternion()
    /* Uses global values of yaw, pitch, roll to compute the
    /* quaternion.
    /*      rotation x - roll
    /*              y - pitch
     /*             z - yaw
    /************************************************************/
    protected Quaternion computeQuaternion(double roll,double pitch, double yaw)
    {
        //Precompute sine and cosine values.
        //Input Euler Angles are in degrees
        double su = (double) Math.sin(roll  *Math.PI/360);
        double sv = (double)Math.sin(pitch*Math.PI/360);
        double sw = (double)Math.sin(yaw *Math.PI/360);
        double cu = (double)Math.cos(roll  *Math.PI/360);
        double cv = (double)Math.cos(pitch*Math.PI/360);
        double cw = (double)Math.cos(yaw *Math.PI/360);

        //Quaternion
        float q0 = 1.0f;
        float q1 = 0.0f;
        float q2 = 0.0f;
        float q3 = 0.0f;

        q0 = (float)(cu*cv*cw + su*sv*sw);
        q1 = (float)(su*cv*cw - cu*sv*sw);
        q2 = (float)(cu*sv*cw + su*cv*sw);
        q3 = (float)(cu*cv*sw - su*sv*cw);

        //Display Quaternion, rounded to three sig. figs.
        q0 = (float)Math.floor(q0*10000)/10000;
        q1 = (float)Math.floor(q1*10000)/10000;
        q2 = (float)Math.floor(q2*10000)/10000;
        q3 = (float)Math.floor(q3*10000)/10000;


        return(new Quaternion (q1,q2,q3,q0));
    }


    protected static void wait(int sec)
    {
        int ms = sec* 1000;

        try
        {
            Thread.sleep(ms);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }

    protected String getQRData(Mat img)
    {
        Rect rectCrop = new Rect(img.width()/4, img.height()/4, img.width()/2, img.height()/2);
        Mat image = new Mat(img, rectCrop);
        Core.flip(image, image, -1);
        QRCodeDetector decoder = new QRCodeDetector();
        //api.saveMatImage(image, "3_QRCodes_Undistort_Flip.png");
        Mat bw = image;//new Mat();

        Mat thresh = new Mat();
        Imgproc.threshold(bw, thresh, 230, 255, Imgproc.THRESH_BINARY);
        //api.saveMatImage(thresh, "simpleThresh.png");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat heirarchy = new Mat();
        Imgproc.findContours(thresh, contours, heirarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        rectCrop = Imgproc.boundingRect(contours.get(0));
        Mat img_contourCrop = new Mat(image, rectCrop);
        //api.saveMatImage(img_contourCrop, "contourCrop.png");

        Mat points = new Mat();
        String data = decoder.detectAndDecode(img_contourCrop, points);
        if(!data.equals("")){
            Log.i(TAG, "QR DATA(qr crop not needed): " + data);
            return data;
        }

        rectCrop = Imgproc.boundingRect(points);
        Mat qrCropTest = new Mat(img_contourCrop, rectCrop);
        api.saveMatImage(qrCropTest, "qrCropTest.png");
        data = decoder.detectAndDecode(qrCropTest);
        Log.i(TAG, "QR DATA(qr crop): " + data);
        if(!data.equals("")){
            return data;
        }
        rectCrop = new Rect(17, 20, img_contourCrop.width()-20, (2*(img_contourCrop.height()/3))-20);
        Mat img3 = new Mat(img_contourCrop, rectCrop);
        api.saveMatImage(img3, "moreCrop.png");
        data = decoder.detectAndDecode(img3);

        Log.i(TAG, "QR DATA: " + data);
        return data;
    }
    protected String QRDataToReport(String data)
    {
        if (data.equals("JEM")){
            return "STAY_AT_JEM";
        }
        if (data.equals("COLUMBUS")){
            return "GO_TO_COLUMBUS";
        }
        if (data.equals("RACK1")){
            return "CHECK_RACK_1";
        }
        if (data.equals("ASTROBEE")){
            return "I_AM_HERE";
        }
        if (data.equals("INTBALL")){
            return "LOOKING_FORWARD_TO_SEE_YOU";
        }
        if (data.equals("BLANK")){
            return "NO_PROBLEM";
        }
        return "LOOKING_FORWARD_TO_SEE_YOU";
    }
    public Mat undistort(Mat src, Mat camMat, Mat distortionCoefficients){
        Mat dst = new Mat();//output image
        org.opencv.imgproc.Imgproc.undistort(src, dst, camMat, distortionCoefficients); //undistorts image
        return dst;
    }
}
