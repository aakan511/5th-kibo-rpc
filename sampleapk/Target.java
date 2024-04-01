package jp.jaxa.iss.kibo.rpc.sampleapk;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

public class Target {

    //orientations indexes also off by one because start point orientation is not needed
    private static Quaternion[] orientations = new Quaternion[]{ (new Quaternion(0f, 0f, -0.707f, 0.707f)), (new Quaternion(0.5f, 0.5f, -0.5f, 0.5f)), (new Quaternion(0f, 0.707f, 0f, 0.707f)),
            (new Quaternion(0, 0, -1, 0)), (new Quaternion(0f, 0.707f, 0f, 0.707f)), new Quaternion(0f, 0f, -0.707f, -0.707f)};

    private static Point[] reversePointHelper = {new Point(11.2146d, -9.92284, 5.47), new Point(10.45, -9.21, 4.51),
            new Point(10.71, -7.763, 4.75), new Point(10.51, -6.6115, 5.2074),
            new Point(11.047,-7.9156,5.4), new Point( 11.405, -9.05, 4.94),
            new Point(11.38d, -8.56d, 4.85)};

    private static Path path1 = new Path(46.0, new Point[]{ (new Point(10.6d, -10.0d, 5.2988d)), (new Point(11.2146d, -9.92284, 5.47))}, orientations[0]);

    private static Path[] target2 = {new Path(21.6, new Point[]{new Point(10.45, -9.21, 4.51)}, orientations[1]),
            new Path(43.8, new Point[]{new Point(10.5, -9.0709, 4.75), new Point(10.45, -9.21, 4.51)} , orientations[1])};

    private static Path[] target3 = {new Path(50.9, new Point[]{new Point(10.71, -8.5, 4.75), new Point(10.71, -7.763, 4.75)}, orientations[2]),
            new Path(53.9, new Point[]{new Point(10.71, -8.5, 4.75), new Point(10.71, -7.763, 4.75)}, orientations[2]),
            new Path(45.2, new Point[]{new Point(10.71, -8.5, 4.75), new Point(10.71, -7.763, 4.75)}, orientations[2])};

    private static Path[] target4 = {new Path(62.6, new Point[]{new Point(10.5, -9.4, 4.7), new Point(10.51, -6.6115, 5.2074)}, orientations[3]),
            new Path(45.1, new Point[]{new Point(10.51, -6.6115, 5.2074)}, orientations[3]),
            new Path(56.6, new Point[]{new Point(10.51, -8.4, 4.7), new Point(10.51, -6.6115, 5.2074)}, orientations[3]),
            new Path(29.6, new Point[]{new Point(10.51, -6.6115, 5.2074)}, orientations[3])};

    private static Path[] QR = {new Path(63.6, new Point[]{new Point(11.38, -8.56, 4.85)}, orientations[4]),
            new Path(72.9, new Point[]{new Point(10.7, -8.95, 4.8), new Point(11.38, -8.56, 4.85)}, orientations[4]),
            new Path(59.0, new Point[]{new Point(11.38, -8.56, 4.85)}, orientations[4]),
            new Path(68.4, new Point[]{new Point(11.38, -8.56, 4.85)}, orientations[4])};

    private static Path[] goal = {new Path(61.7, new Point[]{new Point(11.2, -8.5, 4.75), new Point(11.143d, -6.7607d, 4.9654d)}, orientations[5]),
            new Path(54.4, new Point[]{new Point(10.8, -8.5, 4.75), new Point(11.143d, -6.7607d, 4.9654d)}, orientations[5]),
            new Path(26.3, new Point[]{new Point(11.143d, -6.7607d, 4.9654d)}, orientations[5]),
            new Path(21.6, new Point[]{new Point(11.143d, -6.7607d, 4.9654d)}, orientations[5]),
            new Path(33.456, new Point[]{new Point(11.143d, -6.7607d, 4.9654d)}, orientations[5])};

    //QR = target 5
    //goal = target 6
    //QR & goal indexes are off by one since there is no reason to go straight to the qr code from the start
    private Target() {};

    public static Path getPath(int currTarget, int nextTarget){
        if(currTarget == nextTarget){
            return null;
        }

        if(currTarget > nextTarget){
            Path p = getPath(nextTarget, currTarget);//reversePath(getPath(nextTarget, currTarget));
            //p.setQuaternion(orientations[nextTarget-1]);
            Point[] pts = new Point[p.getPoints().length];


//            for(int i=0; i< pts.length-1; i++){
//                pts[i] = p.getPoints()[p.getPoints().length -1 - i];
//            }
            pts[pts.length-1] = reversePointHelper[nextTarget-1];
            if(pts.length == 2){
                pts[0] = p.getPoints()[0];
            }


            return (new Path(p.getDistance(), pts, orientations[nextTarget-1]));
        }
        switch(nextTarget){
            case 1:
                if(currTarget==0){return path1;}
                else{return null;}
                //break;
            case 2:
                return target2[currTarget];
            //break;
            case 3:
                return target3[currTarget];
            //break;
            case 4:
                return target4[currTarget];
            //break;
            case 5:
                return QR[currTarget-1];
            case 6:
                return goal[currTarget-1];
            default:
                return null;
            //break;
        }
    }

    public static List<Integer> planPath(List<Integer> targets, int currTarget){
        if(targets.size() == 1){
            return targets;
        }
        if(targets.size() == 2){
            Iterator<Integer> it = targets.iterator();
            int nextTarget = it.next();
            int nextNextTarget = it.next();
            double dist1 = getPath(currTarget, nextTarget).getDistance() + getPath(nextTarget, nextNextTarget).getDistance();
            double dist2 = getPath(currTarget, nextNextTarget).getDistance() + getPath(nextNextTarget, nextTarget).getDistance();

            if(dist1<=dist2){
                return targets;
            }if(dist1>dist2){
                Collections.reverse(targets);
                return targets;
            }
        }

        return targets;
    }

    public static Long nextTargetTime(int currTarget, int nextTarget){
        if(currTarget == nextTarget)
            return 0L;
        return (long)((getPath(currTarget, nextTarget)).getDistance() * 1000);
    }
    public static Long nextTargetTime(int currTarget, int nextTarget, boolean readQR){
        if(readQR){
            return (long)((getPath(currTarget, 6)).getDistance() * 1000);
        }
        if(currTarget == nextTarget)
            return 0L;
        return (long)((getPath(currTarget, nextTarget)).getDistance() * 1000);
    }
}