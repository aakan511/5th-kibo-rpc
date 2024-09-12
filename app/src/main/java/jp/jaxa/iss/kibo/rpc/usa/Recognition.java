package jp.jaxa.iss.kibo.rpc.usa;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;


import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
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
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.util.ArrayList;
import java.util.List;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;

import static org.opencv.dnn.Dnn.blobFromImage;
import static org.opencv.objdetect.Objdetect.DICT_5X5_250;
import static org.opencv.objdetect.Objdetect.getPredefinedDictionary;


//import org.jboss.netty.buffer.ChannelBuffer;
//import org.ros.message.MessageListener;
//import org.ros.namespace.GraphName;
//import org.ros.node.AbstractNodeMain;
//import org.ros.node.ConnectedNode;
//import org.ros.node.Node;
//import org.ros.node.topic.Subscriber;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

public class Recognition implements Runnable{
    public String[] classNames;
    public Net model;
    public Context context;
    public KiboRpcApi api;
    public RecognitionResult[] targets = new RecognitionResult[4];
    public ArucoDetector arucoDetector = new ArucoDetector(getPredefinedDictionary(DICT_5X5_250));
    public int finalTarget;
    public int id;

    public Paint paint;
    public ObjectDetector objectDetector;
    public ImageProcessor imageProcessor;
    public boolean saveImages;
    public boolean processImages;


    public Recognition(Context c, int id, String[] names, KiboRpcApi api) {
        Log.i("RecognitionDebug", "Recognition constructor began");
        classNames = names;
        context = c;
        this.id = id;
        this.api = api;
        finalTarget = 4;

        //new stuff
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);

    }

    public void run() {
        loadModel(id);
    }

    public void loadModel(int id) {
//        try {
//            String mModelFile = Utils.exportResource(context, id);
//            model = Dnn.readNetFromONNX(mModelFile);
//            Log.i("Recognition", "model loaded successfully");
//        } catch (Exception e) {
//            Log.i("ERROR", e.getMessage());
//        }
        BaseOptions baseOptionsBuilder = BaseOptions.builder().setNumThreads(4).build();
        ObjectDetector.ObjectDetectorOptions optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(0.5f)
                .setMaxResults(3).setBaseOptions(baseOptionsBuilder).build();

        try {
            String mModelFile = Utils.exportResource(context, id);
            objectDetector = ObjectDetector.createFromFileAndOptions(context, mModelFile, optionsBuilder);
        } catch (Exception e) {
            e.printStackTrace();
        }

        imageProcessor = new ImageProcessor.Builder().build();
    }

    public RecognitionResult detect(Mat img, int id) {
        if (objectDetector == null) {
            Log.i("ERROR", "objectDetector is null");
            return null;
        }
        Bitmap bmp = Bitmap.createBitmap(img.width(), img.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bmp);

        TensorImage tensorImage = imageProcessor.process(TensorImage.fromBitmap(bmp));

        List<Detection> results = objectDetector.detect(tensorImage);


        return null;
    }

    public RecognitionResult findTarget(Mat img, int id) {
        if (model == null) {
            Log.i("ERROR", "tried to call findTarget without loading model first");
            return null;
        }
        int IN_WIDTH = 1280;
        int IN_HEIGHT = 1280;
        double IN_SCALE_FACTOR = 1.0 / 255;
        double MEAN_VAL = 0;

        double yScale = ((double) (img.size().height)) / IN_HEIGHT;
        double xScale = ((double) img.size().width) / IN_WIDTH;

        Mat imgRGB = new Mat();
        Imgproc.cvtColor(img, imgRGB, Imgproc.COLOR_GRAY2RGB);

        Mat blob = blobFromImage(imgRGB, IN_SCALE_FACTOR, new Size(new Point(IN_WIDTH, IN_HEIGHT)), new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), true, false);

        model.setInput(blob);

        Mat outputs = model.forward();

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
            Mat score = mask.col(i).submat(4, outputs.size(1), 0, 1);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(score);
            scoref[i] = (float) mmr.maxVal;
            classid[i] = (int) mmr.maxLoc.y;
        }
        MatOfRect2d bboxes = new MatOfRect2d(rect2d);
        MatOfFloat scores = new MatOfFloat(scoref);
        MatOfInt indeces = new MatOfInt();

        Dnn.NMSBoxes(bboxes, scores, .4f, 0.6f, indeces);
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
        if (in == null) return;
        List<Mat> corners = new ArrayList<>();
        Mat ids = new Mat();

        arucoDetector.detectMarkers(in, corners, ids);

        identify(corners, ids, in);
    }

    public void identify (List<Mat> corners, Mat ids, Mat in){
        for (int i = 0; i < corners.size(); i++) {
            int currTarget = (int) ids.get(i, 0)[0] - 100;
            if (currTarget <= 4) {
                Mat clean = new Mat();
                in.copyTo(clean);
                clean = Vision.arucoCrop(clean, corners.get(i));
                api.saveMatImage(clean, "target_" + ((int) (ids.get(i, 0)[0] - 100)) + "_cropped.png");
                RecognitionResult result = findTarget(clean, currTarget);
                if (currTarget != 0 && targets[currTarget - 1] == null) {
                    api.setAreaInfo(currTarget, result.category, result.numObjects);
                    targets[currTarget - 1] = result;
                } else if (currTarget == 0){
                    for (int j = 0; j < targets.length; j++) {
                        if (targets[j] == null) {
                            targets[j] = new RecognitionResult(new Mat(), 2, "hammer", true);
                        }
                        if (result.category.equals(targets[j].category)) {
                            finalTarget = j + 1;
                            return;
                        }
                        if (finalTarget == 4 && targets[j].errorDetected) {
                            finalTarget = j + 1;
                            Log.i("RECOGNITION_DEBUG", "defaulted to unknown target");
                        }
                    }
                }
            }
        }
    }

    public void fillBlanks() {
        for (int i = 0; i < targets.length; i++) {
            if (targets[i] == null) {
                targets[i] = new RecognitionResult(new Mat(), 2, "hammer", true);
                api.setAreaInfo(i + 1, targets[i].category, targets[i].numObjects);
            }
        }
    }
 }

class RecognitionResult {
    Mat img;
    int numObjects;
    String category;
    boolean errorDetected;

    public RecognitionResult (Mat in, int num, String c) {
        img = in;
        numObjects = num;
        category = c;
        errorDetected = false;
    }

    public RecognitionResult (Mat in, int num, String c, boolean errorDetected) {
        this(in, num, c);
        this.errorDetected = errorDetected;
    }

}
