package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

public class KOZ{

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