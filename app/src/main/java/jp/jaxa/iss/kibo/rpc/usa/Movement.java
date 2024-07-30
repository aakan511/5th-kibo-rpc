package jp.jaxa.iss.kibo.rpc.usa;

import android.util.Log;

import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;

import static jp.jaxa.iss.kibo.rpc.usa.YourService.LOOP_MAX;


public final class Movement {
    public static KiboRpcApi api;

    //Path for going to target
    public static Path[] scanningPaths = {
            new Path(new Quaternion(-.123f, -0.123f, -.696f, .696f), new Point(10.7d, -9.6d, 4.85d)  ), // new Point(10.7d, -9.6d, 4.8d)
            new Path(new Quaternion(0f, 0.707f, 0f, 0.707f), new Point(10.75, -8.3, 4.7)), //new Point(10.75, -8.4, 4.7))
            null, //new Path(new Quaternion(0f, 0.707f, 0f, 0.707f), new Point(10.925d, -7.99, 4.56))
            new Path(new Quaternion(0f, .707f, .707f, 0f), new Point(10.75, -7, 4.75)),
            new Path(new Quaternion(0, 0, .707f, .707f), new Point(11, -6.8525, 4.75))}; //11.143
    public static Path[] returnPaths = {
            new Path(new Quaternion(0f, 0f, -.707f, .707f), new Point(10.75, -7.33, 4.82),  new Point(10.75d, -9.76d, 4.85d)),
            new Path(new Quaternion(0f, 0.707f, 0f, 0.707f), new Point(10.74, -7.33, 4.6), new Point(10.925, -8.875, 4.56)),
            new Path(new Quaternion(0f, 0.707f, 0f, 0.707f), new Point(10.74, -7.33, 4.6),  new Point(10.925d, -7.99, 4.56)), //new Point(10.74, -7.33, 4.6),  new Point(10.925d, -7.99, 4.56))
            new Path(new Quaternion(0f, .707f, -.707f, 0f), new Point(10.6, -6.8525, 4.8))
    };

    private Movement() {}

    public static Kinematics.Confidence moveAstrobee(Point point, Quaternion quaternion, char moveType, Boolean printRobotPosition,
                                                 String TAG)
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

    public static void wait(double sec)
    {
        int ms = (int)(sec* 1000);

        try
        {
            Thread.sleep(ms);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }

    public static void goToTarget(int start, int end) {
        end--;
        if (start != 5) {
            for (int i = 0; i < scanningPaths[end].length; i++) {
                moveAstrobee(scanningPaths[end].points[i], scanningPaths[end].orientation, 'A', false, "goingToTarget");
            }
        } else {
            for (int i = 0; i < returnPaths[end].length; i++) {
//                if (i == returnPaths[end].length - 1) {
//                    moveAstrobee(returnPaths[end].points[i], returnPaths[end].orientation, 'A', false, "goingToTarget");
//                } else {
//                    moveAstrobee(returnPaths[end].points[i], api.getRobotKinematics().getOrientation(), 'A', false, "goingToTarget");
//                }
                moveAstrobee(returnPaths[end].points[i], returnPaths[end].orientation, 'A', false, "goingToTarget");
            }
        }
    }
}


class KOZ{

    // coordinates of KOZ
    protected Point kOZ1_P1_min_data = new Point(10.87, -9.5, 4.27);
    protected Point kOZ1_P1_max_data = new Point(11.6, -9.45, 4.97);
    protected Point kOZ1_P2_min_data = new Point(10.25, -9.5, 4.97);
    protected Point kOZ1_P2_max_data = new Point(10.87, -9.45, 5.62);
    protected Point kOZ2_P1_min_data = new Point(10.87, -8.5, 4.97);
    protected Point kOZ2_P1_max_data = new Point(11.6, -8.45, 5.62);
    protected Point kOZ2_P2_min_data = new Point(10.25, -8.5, 4.27);
    protected Point kOZ2_P2_max_data = new Point(10.7, -8.45, 4.97);
    protected Point kOZ3_P1_min_data = new Point(10.87, -7.40, 4.27);
    protected Point kOZ3_P1_max_data = new Point(11.6, -7.35, 4.97);
    protected Point kOZ3_P2_min_data = new Point(10.25, -7.40, 4.97);
    protected Point kOZ3_P2_max_data = new Point(10.87, -7.35, 5.62);

    protected Point [] kOZ1_P1 = {kOZ1_P1_min_data, kOZ1_P1_max_data};
    protected Point [] kOZ1_P2 = {kOZ1_P2_min_data, kOZ1_P2_max_data};
    protected Point [] kOZ2_P1 = {kOZ2_P1_min_data, kOZ2_P1_max_data};
    protected Point [] kOZ2_P2 = {kOZ2_P2_min_data, kOZ2_P2_max_data};
    protected Point [] kOZ3_P1 = {kOZ3_P1_min_data, kOZ3_P1_max_data};
    protected Point [] kOZ3_P2 = {kOZ3_P2_min_data, kOZ3_P2_max_data};
}

class Path {
    public Point[] points;
    public Quaternion orientation;
    public int length;

    public Path(Quaternion q, Point... p) {
        orientation = q;
        points = p;
        length = p.length;
    }
}
