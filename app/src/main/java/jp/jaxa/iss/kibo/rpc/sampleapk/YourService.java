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

        Recognition r = new Recognition(getApplicationContext(), R.raw.falcon, new String[]{"beaker", "pipette", "goggle", "hammer", "kapton_tape", "screwdriver", "thermometer",
                "top", "watch", "wrench"}, api);

        // Target 1
        (new Thread(r)).start();
        goToTarget(0, 1);


        TargetSnapshot snap = new TargetSnapshot(api, r);
        snap.run();

        if (r.targets[0] == null) {
            Point og = Movement.scanningPaths[0].points[0];
            Point adj = new Point(og.getX(), og.getY() - .2, og.getZ());
            Log.i("Target1", "had to readjust");
            moveAstrobee(adj, Movement.scanningPaths[0].orientation, 'A', false, "readjusting");

            (new ReadjustSnapshot(api, r)).start();
         }

        // Target 2 & 3
        goToTarget(1, 2);
        snap.start();

        // Target 4
        goToTarget(3, 4);
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
//        r.finalTarget = 4;
        goToTarget(5, r.finalTarget);
//        goToTarget(5, 4);

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

//    public static void takeSnapshot(KiboRpcApi api, Recognition r) {
//        //api.flashlightControlFront(.01f);
//        Mat image = api.getMatNavCam();
//        int target = Vision.currTarget;
//
//        image = Vision.undistort(image);
//        api.saveMatImage(image, "front" + target + "_" + Vision.randName() + ".jpg");
//
//        r.identify(image);
//
//        Vision.currTarget++;
//    }
}

class TargetSnapshot extends Thread{
    KiboRpcApi api;
    Recognition r;

    public TargetSnapshot (KiboRpcApi api, Recognition r) {
        this.api = api;
        this.r = r;
    }

    public void run() {
        //api.flashlightControlFront(.01f);
        Mat image = api.getMatNavCam();
        int target = Vision.currTarget;

        image = Vision.undistort(image);
        api.saveMatImage(image, "front" + target + "_" + Vision.randName() + ".jpg");

        r.identify(image);

        Vision.currTarget++;
    }
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
