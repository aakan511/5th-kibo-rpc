package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;

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
        Vision v = new Vision(api);

        Recognition r = new Recognition(getApplicationContext(), R.raw.armstrong, new String[]{"beaker", "pipette", "goggle", "hammer", "kapton_tape", "screwdriver", "thermometer",
                "top", "watch", "wrench"}, api);

        (new Thread(r)).start();
        TargetSnapshot snap = new TargetSnapshot(api, r);

        // Target 1
        goToTarget(0, 1);
        snap.takeImage(true);
        snap.start();

        // Target 2 & 3
        goToTarget(1, 2);
//        snap.switchToDockCam();
        //snap.takeImage(false);
        snap.start();

        // Target 4
        goToTarget(3, 4);
        snap.takeImage(true);
        snap.start();

        // Astronaut
        goToTarget(4, 5);
        api.reportRoundingCompletion();

        Movement.wait(2);

        api.flashlightControlFront(.01f);
        Mat image = api.getMatNavCam();
        image = Vision.undistort(image);
//        Vision.findAruco(image);
//        api.saveMatImage(image, "front5.jpg");
        r.identify(image);

        // Target item
//        r.finalTarget = 3;
        goToTarget(5, r.finalTarget);

        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();
        Point[] distanceTargetItem = Vision.arucoOffsetCenter(image, r.finalTarget);
        if (Vision.targetItemReadjust(distanceTargetItem[1])) {
            Point adjustment = distanceTargetItem[0];
            Log.i("adjustment", "(" + adjustment.getX() + ", " + adjustment.getY() + ", " + adjustment.getZ() + ")");
            Point currPos = api.getRobotKinematics().getPosition();
            Point absPos = new Point(currPos.getX() + adjustment.getX(), currPos.getY() + adjustment.getY(), currPos.getZ() + adjustment.getZ());
//        image = Vision.undistort(image);
//        api.saveMatImage(image, "front6.jpg");
            moveAstrobee(absPos, api.getRobotKinematics().getOrientation(), 'A', false, "finalAdjustment");
        }

//        api.flashlightControlFront(.01f);
        image = api.getMatNavCam();

        //FOR DEBUG ONLY REMOVE LATER
        distanceTargetItem = Vision.arucoOffsetCenter(image, r.finalTarget);
        Vision.targetItemReadjust(distanceTargetItem[1]);

        image = Vision.undistort(image);
        api.saveMatImage(image, "front7_" + Vision.randName() + "_.jpg");

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
        //api.flashlightControlFront(.01f);
//        Mat image = snapshotFront ? api.getMatNavCam() : api.getMatDockCam();
        int target = Vision.currTarget;

        image = snapshotFront ? Vision.undistort(image) : Vision.undistortRear(image);
        api.saveMatImage(image, "front" + target + "_" + Vision.randName() + ".jpg");

        r.identify(image);

        snapshotFront = true; //for next image
        Vision.currTarget++;
    }

    public void takeImage(boolean takeWithFront) {
        snapshotFront = takeWithFront;
        image = snapshotFront ? api.getMatNavCam() : api.getMatDockCam();
    }

//    public void switchToDockCam() {
//        snapshotFront = false;
//    }
}

class ReadjustSnapshot extends TargetSnapshot {

    public ReadjustSnapshot(KiboRpcApi api, Recognition r) {
        super(api, r);
    }

    @Override
    public void run() {
        Mat image = Vision.undistort(api.getMatNavCam());
        r.identify(image);
        api.saveMatImage(image, "front1_adjusted_" + Vision.randName() + "_.jpg");
    }
}
