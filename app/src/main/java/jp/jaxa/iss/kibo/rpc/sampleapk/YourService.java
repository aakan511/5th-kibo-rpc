package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.Bitmap;
import android.util.Log;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;
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
        takeSnapshot(api);

        // Target 2
        goToTarget(1, 2);
        takeSnapshot(api);

        if (api.getRobotKinematics().getPosition().getX() <= 10.7) {
            moveAstrobee(Movement.scanningPaths[1].points[Movement.scanningPaths[1].points.length - 1], Movement.scanningPaths[1].orientation, 'A', false, "readjusting");
            Log.i("Target2", "had to readjust");
        }


        // Target 3
        goToTarget(2, 3);
        takeSnapshot(api);

        // Target 4
        goToTarget(3, 4);
        takeSnapshot(api);

        // Astronaut
        goToTarget(4, 5);
        api.reportRoundingCompletion();

        Movement.wait(5);

        api.flashlightControlFront(.01f);
        Mat image = api.getMatNavCam();
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front5.jpg");

        // Target item
//        Vision.currTarget = 1; // use this to test return paths
        goToTarget(5, Vision.currTarget);

        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        Point adjustment = Vision.arucoOffset(image, Vision.currTarget);
        Log.i("adjustment", "(" + adjustment.getX() + ", " + adjustment.getY() + ", " + adjustment.getZ() + ")");
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

    public static void takeSnapshot(KiboRpcApi api) {
        //api.flashlightControlFront(.01f);
        Mat image = api.getMatNavCam();
        int target = Vision.currTarget;
        Point adj = Vision.arucoOffsetDebug(image, target);
        double distance = Vision.distance(adj);
        Log.i("distanceReports", "target " + target + " distance : " + distance);
        Log.i("distanceReportsPoint", "(" + adj.getX() + ", " + adj.getY() + ", " + adj.getZ() + ")");
        if (distance > .30) {
            Point currPos1 = api.getRobotKinematics().getPosition();
            Point absPos = new Point(currPos1.getX() + adj.getX(), currPos1.getY() + adj.getY(), currPos1.getZ() + adj.getZ());
            moveAstrobee(absPos, api.getRobotKinematics().getOrientation(), 'A', false, "TESTINKH");
            api.saveMatImage(image, "front" + target + "OG.jpg");
            image = api.getMatNavCam();
        }
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front" + target + ".jpg");
    }
}
