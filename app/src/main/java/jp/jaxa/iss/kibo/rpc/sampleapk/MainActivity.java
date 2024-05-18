import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
//    private Net net;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        try {
//            // Load the network
//            net = Dnn.readNetFromCaffe(getAssets().open("deploy.prototxt"), getAssets().open("model.caffemodel"));
//        } catch (IOException | IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void processImage(String imagePath) {
//        Mat image = Imgcodecs.imread(imagePath);
//        Mat blob = Dnn.blobFromImage(image, 1.0, new Size(224, 224), new Scalar(104, 117, 123), false, false);
//        net.setInput(blob);
//        Mat result = net.forward();
//        // Process the result
//    }
}