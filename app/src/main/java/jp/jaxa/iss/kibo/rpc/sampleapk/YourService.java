package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.Bitmap;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.core.Mat;

import static jp.jaxa.iss.kibo.rpc.sampleapk.Movement.moveAstrobee;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {

    public static final int LOOP_MAX = 4;
    @Override
    protected void runPlan1(){
        // The mission starts.
        api.startMission();
        Movement.api = api;
        Vision v = new Vision(api, getApplicationContext());

        // Target 1
        Point point = new Point(10.9d, -9.5d, 5.19d); //5.0
        Quaternion quaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
        moveAstrobee(point, quaternion, 'A', true, "AaronDebug");

        api.flashlightControlFront(.01f);
        Mat image = api.getMatNavCam();
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front0.jpg");

        // Target 2
        point = new Point(10.925, -8.875, 4.9);//new Point(10.925, -8.35, 4.9);
        Quaternion quaternion1 = new Quaternion(0f, 0.707f, 0f, 0.707f);
        moveAstrobee(point, quaternion1, 'A', true, "AaronDebug");

        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front1.jpg");


        // Target 3
        point = new Point(10.7, -7.925, 4.8); //10.8, 4.9 = x, z
        moveAstrobee(point, quaternion1, 'A', true, "AaronDebug");

        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front2.jpg");

        // Target 4
        point = new Point(10.7, -6.8525, 4.8);
        quaternion1 = new Quaternion(-1f, 0f, 0f, 0f);
        moveAstrobee(point, quaternion1, 'A', true, "AaronDebug");

        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front3.jpg");
        image = api.getMatDockCam();
        image = Vision.undistort(image, false);
        Vision.findAruco(image);
        api.saveMatImage(Vision.undistort(image), "back3.jpg");

        // Astronaut
        point = new Point(11.143, -6.7607, 4.9654);
        quaternion1 = new Quaternion(0f, 0f, 0.707f, 0.707f);
        moveAstrobee(point, quaternion1, 'A', true, "AaronDebug");

        // Get a camera image.
        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        image = Vision.undistort(image);
//        Vision.findAruco(image);
        api.saveMatImage(image, "front4.jpg");
        image = api.getMatDockCam();
        image = Vision.undistort(image, false);
//        Vision.findAruco(image);
        api.saveMatImage(image, "back4.jpg");


        // When you move to the front of the astronaut, report the rounding completion.
        api.reportRoundingCompletion();

        Movement.wait(30);

        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        image = Vision.undistort(image);
//        Vision.findAruco(image);
        api.saveMatImage(image, "front5.jpg");
        image = api.getMatDockCam();
        image = Vision.undistort(image, false);
//        Vision.findAruco(image);
        api.saveMatImage(image, "back5.jpg");
        /* ********************************************************** */
        /* Write your code to recognize which item the astronaut has. */
        /* ********************************************************** */

        // Let's notify the astronaut when you recognize it.
        api.notifyRecognitionItem();

        /* ******************************************************************************************************* */
        /* Write your code to move Astrobee to the location of the target item (what the astronaut is looking for) */
        /* ******************************************************************************************************* */

        // Take a snapshot of the target item.
        api.takeTargetItemSnapshot();
    }

    @Override
    protected void runPlan2(){
       // write your plan 2 here.
    }

    @Override
    protected void runPlan3(){
        // write your plan 3 here.
    }

    public static String intify(float f) {
        return "" + (int) (f * 100);
    }
}
