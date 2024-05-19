package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.Bitmap;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.core.Mat;

import static jp.jaxa.iss.kibo.rpc.sampleapk.Movement.goToTarget;
import static jp.jaxa.iss.kibo.rpc.sampleapk.Movement.moveAstrobee;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {

    public static final int LOOP_MAX = 4;
    @Override
    protected void runPlan1(){
        // Start Mission
        api.startMission();
        Movement.api = api;
        Vision v = new Vision(api, getApplicationContext());

        // Target 1
        goToTarget(0, 1);

        api.flashlightControlFront(.01f);
        Mat image = api.getMatNavCam();
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front0.jpg");

        // Target 2
        goToTarget(1, 2);

        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front1.jpg");


        // Target 3
        goToTarget(2, 3);

        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front2.jpg");

        // Target 4
        goToTarget(3, 4);

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
        goToTarget(4, 5);
        api.reportRoundingCompletion();

        Movement.wait(5);

        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front5.jpg");

        // Target item
        goToTarget(5, Vision.currTarget);

        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        Point adjustment = Vision.arucoOffset(image, Vision.currTarget);
        Point currPos = api.getRobotKinematics().getPosition();
        Point absPos = new Point(currPos.getX() + adjustment.getX(), currPos.getY() + adjustment.getY(), currPos.getZ() + adjustment.getZ());
        image = Vision.undistort(image);
        api.saveMatImage(image, "front6.jpg");

        moveAstrobee(absPos, api.getRobotKinematics().getOrientation(), 'A', false, "finalAdjustment");

        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        image = Vision.undistort(image);
        api.saveMatImage(image, "front7.jpg");

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
