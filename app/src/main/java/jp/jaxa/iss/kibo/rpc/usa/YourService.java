package jp.jaxa.iss.kibo.rpc.usa;

import android.util.Log;

import gov.nasa.arc.astrobee.Result;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import static jp.jaxa.iss.kibo.rpc.usa.Movement.goToTarget;
import static jp.jaxa.iss.kibo.rpc.usa.Movement.moveAstrobee;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {

    public static final int LOOP_MAX = 6;
    @Override
    protected void runPlan1(){
        // Start Mission
        api.startMission();
        Movement.api = api;
        Vision v = new Vision(api);

        Recognition r = new Recognition(getApplicationContext(), R.raw.blinker, new String[]{"beaker", "pipette", "goggle", "hammer", "kapton_tape", "screwdriver", "thermometer",
                "top", "watch", "wrench"}, api);

        (new Thread(r)).start();
        TargetSnapshot snap = new TargetSnapshot(api, r);

        // Target 1
        goToTarget(0, 1);
        snap.takeImage(true);
        snap.start();

        // Target 2 & 3
        goToTarget(1, 2);
        snap.takeImage(true);
        snap.start();

        // Target 4
        goToTarget(3, 4);
        snap.takeImage(true);
        snap.start();

        // Astronaut
        goToTarget(4, 5);
        r.fillBlanks();
        api.reportRoundingCompletion();

        ArucoDetection arucoDetection = v.waitForTarget(30);
        if (arucoDetection != null) {
            r.identify(arucoDetection.corners, arucoDetection.ids, arucoDetection.image);
        } else {
            Log.i("ASTRONAUTERROR", "Astronaut Aruco not found");
        }

        api.notifyRecognitionItem();

        goToTarget(5, r.finalTarget);

        api.flashlightControlFront(.05f);
        Mat image = api.getMatNavCam();
        Point[] distanceTargetItem = Vision.arucoOffsetCenter(image, r.finalTarget);
        if (distanceTargetItem != null && Vision.targetItemReadjust(distanceTargetItem[1])) {
            Point adjustment = distanceTargetItem[0];
            Log.i("adjustment", "(" + adjustment.getX() + ", " + adjustment.getY() + ", " + adjustment.getZ() + ")");
            Point currPos = api.getRobotKinematics().getPosition();
            Point absPos = new Point(currPos.getX() + adjustment.getX(), currPos.getY() + adjustment.getY(), currPos.getZ() + adjustment.getZ());
            moveAstrobee(absPos, api.getRobotKinematics().getOrientation(), 'A', false, "finalAdjustment");
        }

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

}

class TargetSnapshot extends Thread{
    KiboRpcApi api;
    Recognition r;
    boolean snapshotFront;
    Mat image;

    public TargetSnapshot (KiboRpcApi api, Recognition r) {
        this.api = api;
        this.r = r;
        snapshotFront = true;
        image = new Mat();
    }

    public void run() {
        int target = Vision.currTarget;

        image = snapshotFront ? Vision.undistort(image) : Vision.undistortRear(image);
        api.saveMatImage(image, "front" + target + "_" + ".jpg");

        r.identify(image);

        snapshotFront = true; //for next image
        Vision.currTarget++;
    }

    public void takeImage(boolean takeWithFront) {
        snapshotFront = takeWithFront;
        Result result = snapshotFront ? api.flashlightControlFront(.05f) : api.flashlightControlBack(.05f);
        Log.i("flashlightControlResultOn", result.toString());
        image = snapshotFront ? api.getMatNavCam() : api.getMatDockCam();
        result = snapshotFront ? api.flashlightControlFront(.00f) : api.flashlightControlBack(.00f);
        Log.i("flashlightControlResultOff", result.toString());
    }
}