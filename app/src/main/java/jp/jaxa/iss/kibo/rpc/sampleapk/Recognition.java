package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.content.Context;
import android.util.Log;

import com.google.common.io.ByteStreams;

import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Net;
import org.opencv.dnn.Dnn;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.ArucoDetector;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;

import static org.opencv.dnn.Dnn.blobFromImage;
import static org.opencv.objdetect.Objdetect.DICT_5X5_250;
import static org.opencv.objdetect.Objdetect.getPredefinedDictionary;

public class Recognition {
    public String[] classNames;
    public Net model;
    public Context context;
    public KiboRpcApi api;
    public String[] targets = new String[4];
    public ArucoDetector arucoDetector = new ArucoDetector(getPredefinedDictionary(DICT_5X5_250));
    public int finalTarget;

    public Recognition(Context c, int id, String[] names, KiboRpcApi api) {
        Log.i("RecognitionDebug", "Recognition constructor began");
        classNames = names;
        context = c;
        loadModel(id);
        this.api = api;
        finalTarget = 4;

    }

    public void loadModel(int id) {
        try {
            String mModelFile = Utils.exportResource(context, id);
//            MatOfByte buffer = loadFileFromResource(id);
//            model = Dnn.readNetFromONNX(buffer);
            model = Dnn.readNetFromONNX(mModelFile);
            Log.i("Recognition", "model loaded successfully");
        } catch (Exception e) {
            Log.i("ERROR", e.getMessage());
        }
    }



//    public MatOfByte loadFileFromResource(int id) {
////        byte[] buffer;
//        try {
////            // load cascade file from application resources
////            InputStream is = context.getResources().openRawResource(id);
////            Log.i("RecognitionDebug", "input stream acquired");
//////            int size = is.available();
//////            buffer = new byte[size];
//////            int bytesRead = is.read(buffer);
////            buffer = ByteStreams.toByteArray(is);
////            Log.i("RecognitionDebug", "length of buffer :" + buffer.length);
////            is.close();
//            Mat modelMat = Utils.loadResource(context, id);
//            return  (MatOfByte) modelMat; //((MatOfByte) (modelMat));
//        } catch (IOException e) {
//            //e.printStackTrace();
//            Log.e("ERROR", "Failed to load ONNX model from resources! Exception thrown: " + e);
//            return null;
//        }
//        //return new MatOfByte(buffer);
//    }

    public RecognitionResult findTarget(Mat img, int id) {
        if (model == null) {
            Log.i("ERROR", "tried to call findTarget without loading model first");
            return null;
        }
        int IN_WIDTH = 640;
        int IN_HEIGHT = 640;
        double IN_SCALE_FACTOR = 1.0 / 255;
        double MEAN_VAL = 0;

        double yScale = ((double) (img.size().height)) / IN_HEIGHT;
        double xScale = ((double) img.size().width) / IN_WIDTH;

        Log.i("RecognitionTesting", "scales created");
        Mat imgRGB = new Mat();
        Imgproc.cvtColor(img, imgRGB, Imgproc.COLOR_GRAY2RGB);

        Log.i("RecognitionTesting", "image converted to color " + img.channels() + ", " + img.type());
        Log.i("RecognitionTesting", "color image : " + imgRGB.channels() + ", " + imgRGB.type());
        Mat blob = blobFromImage(imgRGB, IN_SCALE_FACTOR, new Size(new Point(IN_WIDTH, IN_HEIGHT)), new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), true, false);

        Log.i("RecognitionTesting", "created blob");
        model.setInput(blob);
        Log.i("RecognitionTesting", "set blob as input");

        Mat outputs = model.forward();

        Log.i("RecognitionTesting", "Outputs shape : (" + outputs.size().width + " , " + outputs.size().height + ", "+ outputs.channels() + ", " + outputs.total() +")");
        Mat mask = outputs.reshape(0, 1).reshape(0, outputs.size(1));
        Rect2d[] rect2d = new Rect2d[mask.cols()];
        float[] scoref = new float[mask.cols()];
        int[] classid = new int[mask.cols()];

        for (int i = 0; i < mask.cols(); i++) {
            double[] x = mask.col(i).get(0, 0);
            double[] y = mask.col(i).get(1, 0);
            double[] w = mask.col(i).get(2, 0);
            double[] h = mask.col(i).get(3, 0);

            rect2d[i] = new Rect2d((x[0] - w[0]/2) * xScale, (y[0] - h[0]/2) * yScale, w[0] * xScale, h[0] * yScale);
            Mat score = mask.col(i).submat(4, outputs.size(1), 0, 1); // outputs.size(1) - 1
            Core.MinMaxLocResult mmr = Core.minMaxLoc(score);
            scoref[i] = (float) mmr.maxVal;
            classid[i] = (int) mmr.maxLoc.y;
        }
        MatOfRect2d bboxes = new MatOfRect2d(rect2d);
        MatOfFloat scores = new MatOfFloat(scoref);
        MatOfInt indeces = new MatOfInt();

        Dnn.NMSBoxes(bboxes, scores, .49999f, 0.7f, indeces); //nms = .45
        Log.i("RecognitionDebug", "indeces total : " + indeces.total());
        List<Integer> result = indeces.total() > 0 ? indeces.toList() : new ArrayList<Integer>();

        int category = 0;
        float maxConfidence = 0.0f;
        int cnt = result.size();
        for (Integer integer : result) {
            imgRGB = drawBoundingBox(imgRGB, classid[integer], scoref[integer], rect2d[integer]);

            if (scoref[integer] > maxConfidence) {
                category = classid[integer];
                maxConfidence = scoref[integer];
            }
        }
        Log.i("YOLOResults", "category : " + classNames[category] + ", count : " + cnt);
        api.saveMatImage(imgRGB, "recognitionTesting" + id + ".jpg");
        return new RecognitionResult(imgRGB, cnt, classNames[category]);
    }

    public Mat drawBoundingBox(Mat in, int classId, double confidence, int x, int y, int x1, int y1) {
        String label = String.format("%s, %.2f", classNames[classId], confidence);
        Log.i("RecognitionDrawingBoundingBox", String.format("Label: %s, x: %d, y: %d", label, x, y));
        Rect box = new Rect(new Point(x, y), new Point(x1, y1));
        Imgproc.rectangle(in, box, new Scalar(255, 0, 0));
        Imgproc.putText(in, label, new Point(x - 10, y - 10), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 0, 0), 2);

        return in;
    }

    public Mat drawBoundingBox(Mat in, int classId, double confidence, Rect2d rect) {

        return drawBoundingBox(in, classId, confidence, (int) rect.x, (int) rect.y, (int) (rect.x + rect.width), (int)(rect.y + rect.height));
    }

    public void identify (Mat in){
        List<Mat> corners = new ArrayList<>();
        Mat ids = new Mat();

        arucoDetector.detectMarkers(in, corners, ids);

        for (int i = 0; i < corners.size(); i++) {
            int currTarget = (int) ids.get(i, 0)[0] - 100;
            if (currTarget <= 4) {
                Mat clean = new Mat();
                in.copyTo(clean);
                clean = Vision.arucoCrop(clean, corners.get(i));
                api.saveMatImage(clean, "target_" + ((int) (ids.get(i, 0)[0] - 100)) + "_" + Vision.randName() + "_cropped.png");
                RecognitionResult result = findTarget(clean, currTarget);
                //Log.i ("DebuggingTarg4 glitch", "targets[currTarget - 1] : " + currTarget + ", " + finalTarget + ", " + (currTarget == finalTarget));
                if (currTarget != 0 && targets[currTarget - 1] == null) {
                    api.setAreaInfo(currTarget, result.category, result.numObjects);
                    targets[currTarget - 1] = result.category;
                } else if (currTarget == 0){
                    api.reportRoundingCompletion();
                    for (int j = 0; j < targets.length; j++) {
                        if (result.category.equals(targets[j])) {
                            finalTarget = j + 1;
                            return;
                        }
                    }
                }
            }



        }
    }

}

class RecognitionResult {
    Mat img;
    int numObjects;
    String category;

    public RecognitionResult (Mat in, int num, String c) {
        img = in;
        numObjects = num;
        category = c;
    }
}
