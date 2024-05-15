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
        // Move to a point.
        Point point = new Point(10.9d, -9.5d, 5.195d); //-9.9
        Quaternion quaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
        moveAstrobee(point, quaternion, 'A', true, "AaronDebug");


        Mat image = api.getMatNavCam();
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front0.jpg");


        image = api.getMatDockCam();
        api.saveMatImage(Vision.undistort(image), "back0.jpg");


        point = new Point(10.925, -8.6, 4.9); //5.195
        Quaternion quaternion1 = new Quaternion(0f, 0.707f, 0f, 0.707f);
        moveAstrobee(point, quaternion1, 'A', true, "AaronDebug");



        // Get a camera image.
        image = api.getMatNavCam();
        image = Vision.undistort(image);
        Vision.findAruco(image);
        api.saveMatImage(image, "front2.jpg");


        image = api.getMatDockCam();
        api.saveMatImage(Vision.undistort(image), "back2.jpg");



        /* *********************************************************************** */
        /* Write your code to recognize type and number of items in the each area! */
        /* *********************************************************************** */

        // When you recognize items, let’s set the type and number.
        api.setAreaInfo(1, "item_name", 1);

        /* **************************************************** */
        /* Let's move to the each area and recognize the items. */
        /* **************************************************** */

        // When you move to the front of the astronaut, report the rounding completion.
        api.reportRoundingCompletion();

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
