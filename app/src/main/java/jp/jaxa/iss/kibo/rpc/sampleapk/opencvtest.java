package jp.jaxa.iss.kibo.rpc.sampleapk;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;
import java.util.List;

public class opencvtest {

    private static final int TARGET_IMG_WIDTH = 224;
    private static final int TARGET_IMG_HEIGHT = 224;
    private static final double SCALE_FACTOR = 1/255.0;
    private static Mat imageRead;

    public static void main(String[] args) {
        String imageLocation = "";
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // not sure if i need this

        Mat image = new Mat();

        Net dnnNet = Dnn.readNetFromTensorflow("");
        Mat blob = Dnn.blobFromImage(image, SCALE_FACTOR, new Size(300,300));

        dnnNet.setInput(blob);
        List<Mat> detections = new ArrayList<>();
        dnnNet.forward(detections);

        for (Mat detection : detections) {
            double confidence = detection.get(0, 2)[0];

            if (confidence > .5) {
                int x1 = (int) detection.get(0,3)[0];
                int y1 = (int) detection.get(0,4)[0];
                int x2 = (int) detection.get(0,5)[0];
                int y2 = (int) detection.get(0,6)[0];

                Imgproc.rectangle(image, new Point(x1, y1), new Point(x2,y2), new Scalar(0, 255, 0), 2);
            }
        }

//        Mat classification = dnnNet.forward();
//        DnnOpenCV.getPredictedClass(classification);
//
//        imageRead = Imgcodecs.imread(imageLocation);
//
//        Imgproc.resize(imageRead, image, new Size(256,256));
//
//        Mat imgFloat = new Mat(image.rows(), image.cols(), CvType.CV_32FC3);
//        image.convertTo(imgFloat, CvType.CV_32FC3, SCALE_FACTOR);

        Imgcodecs.ims
        Imgcodecs.waitKey(0);
    }
}
