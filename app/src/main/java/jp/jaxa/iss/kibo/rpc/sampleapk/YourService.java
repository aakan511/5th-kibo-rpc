package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.Bitmap;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.core.Mat;

import java.util.Random;

import static jp.jaxa.iss.kibo.rpc.sampleapk.Movement.moveAstrobee;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {

    public static final int LOOP_MAX = 4;
    @Override
    protected void runPlan1(){
        // The mission starts.
        Random rand = new Random();
        api.startMission();
        Movement.api = api;
        Vision.api = api;
        ProperReport pr = new ProperReport();
        // Set up image YOLO Model
        YOLO yolo = new YOLO();
        yolo.setModelFile("YOLOv8n.pt");
        // Move to a point.
        Point point = new Point(10.9d, -9.92284d, 5.195d);
        Quaternion quaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
        moveAstrobee(point, quaternion, 'A', true, "AaronDebug");

        point = new Point(10.925, -8.6, 5.195);
        Quaternion quaternion1 = new Quaternion(0f, 0.707f, 0f, 0.707f);

        moveAstrobee(point, quaternion1, 'A', true, "AaronDebug");

        Bitmap img = api.getBitmapNavCam();
        String a1report = "";
        for(Recognition r: yolo.detect(img, "JUSTIN")){
            if(r.getLabelName() != "objects")
                a1report=r.getLabelName();
        }


        /* *********************************************************************** */
        /* Write your code to recognize type and number of items in the each area! */
        /* *********************************************************************** */

        // When you recognize items, let’s set the type and number.
        api.setAreaInfo(1, pr.sex(a1report), 1);

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
