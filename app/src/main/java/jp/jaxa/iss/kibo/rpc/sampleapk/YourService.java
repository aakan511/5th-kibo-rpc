package jp.jaxa.iss.kibo.rpc.sampleapk;

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
        Vision.api = api;
        // Move to a point.
        Point point = new Point(10.9d, -9.92284d, 5.195d);
        Quaternion quaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
        moveAstrobee(point, quaternion, 'A', true, "AaronDebug");

        quaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
        point = new Point(10.925, -8.6, 5.195);
        //moveAstrobee(point, quaternion, 'A', true, "AaronDebug");


        // Get a camera image.
//        Mat image = api.getMatNavCam();
//        api.saveMatImage(image, "front1");
//        Mat target1 = Vision.cropPage(image);
//        api.saveMatImage(target1, "target1_crop");
//
//        image = api.getMatDockCam();
//        api.saveMatImage(image, "back1");
//        Mat target4 = Vision.cropPage(image);
//        api.saveMatImage(target4, "target4_crop");

        point = new Point(10.925, -8.6, 5.195);
        Quaternion quaternion1 = new Quaternion(0f, 0.707f, 0f, 0.707f);
        //moveAstrobee(point, quaternion, 'A', true, "AaronDebug");

        for(double x = 10.60; x <= 11.5; x+=.1) {
            point = new Point(x, -8.6, 5.195);
            moveAstrobee(point, quaternion, 'A', true, "AaronDebug");

//            for (float i = 0f; i < .4f; i+=.05f) {
//                api.flashlightControlFront(i);
//                api.flashlightControlBack(i);
//                api.saveMatImage(api.getMatNavCam(), "" + "front1_" + x +"_" + intify(i) + ".png");
//                api.saveMatImage(api.getMatDockCam(), "" + "back1_" + x +"_" + intify(i) + ".png");
//
//            }
            api.saveMatImage(api.getMatNavCam(), "" + "front1_" + x + ".png");
            api.saveMatImage(api.getMatDockCam(), "" + "back1_" + x + ".png");

            moveAstrobee(point, quaternion1, 'A', true, "AaronDebug");

//            for (float i = 0f; i < .4f; i+=.05f) {
//                api.flashlightControlFront(i);
//                api.flashlightControlBack(i);
//                api.saveMatImage(api.getMatNavCam(), "" + "front2_" + x +"_" + intify(i) + ".png");
//
//            }
            api.saveMatImage(api.getMatNavCam(), "" + "front2_" + x + ".png");
        }

        // Get a camera image.
//        image = api.getMatNavCam();
//        api.saveMatImage(image, "front2");
//        Mat target23 = Vision.cropPage(image);
//        api.saveMatImage(target23, "target23_crop");
//
//        image = api.getMatDockCam();
//        api.saveMatImage(image, "back2");



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
