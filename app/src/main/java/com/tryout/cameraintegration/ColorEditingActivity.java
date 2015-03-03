package com.tryout.cameraintegration;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ColorEditingActivity extends Activity {

    private Bitmap mBitmap;
    private ImageView iv;
    private File mediaFile;
    private String writeBmp;
    private String mFileName;
    private Scalar mRed = new Scalar(255, 0, 0);
    private Scalar mGreen = new Scalar(0, 255, 0);
    private Scalar mBlue = new Scalar(0, 0, 255);
    private int thickness = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_editing);

        // Retrieves saved file path/name
        Intent intent = getIntent();
        mFileName = intent.getStringExtra(MainActivity.EXTRA_FILE_NAME);
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "/CameraIntegration");
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                mFileName);
        Uri fileUri = Uri.fromFile(mediaFile);

        // Sets bitmap to be editable(mutable) and saves it in "mBitmap"
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        mBitmap = BitmapFactory.decodeFile(fileUri.getPath(), options);
        // Show bitmap on imageView2
        iv = (ImageView) findViewById(R.id.imageView2);
        iv.setImageBitmap(mBitmap);
        iv.postInvalidate();
    }

    // load more openCV stuff
    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    // Just to know which button was pressed based on their IDs, calls scanColors(int)
    public void decideColors(View v){
        switch(v.getId()){
            case(R.id.button_detection):
                detectShape(scaleDown(mBitmap, 0.3f, true));
                break;
        }
    }

    // Button scaled and original. Removes tinting.
    public void resetColor(View v){
        switch(v.getId()) {
            case (R.id.button_scaled):
                Bitmap scaledBitmap = scaleDown(mBitmap, 0.1f, true);
                iv.setImageBitmap(scaledBitmap);
                savePNG(1337, scaledBitmap);
                break;
            case (R.id.button_original):
                iv.setImageBitmap(mBitmap);
                iv.postInvalidate();
                break;
        }
    }

    // Saves a scaled down bitmap Scale should receive float bigger than 0.
    public static Bitmap scaleDown(Bitmap realImage, float scale, boolean filter) {
        int width = Math.round(scale * realImage.getWidth());
        int height = Math.round(scale * realImage.getHeight());
        //Log.d("NEW", "\nW "+width+"\nH "+height);
        return Bitmap.createScaledBitmap(realImage, width, height, filter);
    }

    // Saves as PNG, used to test the output. Variable "randomName" is just used in the filename, can be any int.
    public void savePNG(int randomName, Bitmap bmpToSave){
        FileOutputStream out = null;
        // Grabs original file name and appends things to it.
        if (mediaFile.getPath().contains(".")){
            writeBmp = mediaFile.getPath().substring(0, mediaFile.getPath().lastIndexOf('.'));
        }
        try {
            out = new FileOutputStream(writeBmp + "_" + Math.abs(randomName) + ".png");
            bmpToSave.compress(Bitmap.CompressFormat.PNG, 100, out);
            // PNG is a loss less format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // load openCV stuff *OpenCV manager must be installed*
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public void detectShape(Bitmap scaled) {
        Mat rgba = new Mat();
        Mat gray = new Mat();
        Mat hierarchy = new Mat();
        Mat circles = new Mat();
        Mat blur = new Mat();
        int cntTriangles = 0;
        int cntSquares = 0;
        int cntCircles = 0;
        int maxThreshold = 255;
        int threshold = 90;
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Utils.bitmapToMat(scaled, rgba);

        // convert image to grayscale
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY, 4);

        // Convert to binary image
        Imgproc.threshold(gray, gray, threshold, maxThreshold, Imgproc.THRESH_BINARY_INV);

        // find circles
        Imgproc.GaussianBlur(gray, blur, new Size(9, 9), 4, 4);
        Imgproc.HoughCircles(blur, circles, Imgproc.CV_HOUGH_GRADIENT, 2.0, 100);

        // iterate over circles
        for (int i = 0; i < Math.min(circles.cols(), 10); i++) {
            double vCircle[] = circles.get(0, i);
            Point pt = new Point();
            int radius;

            cntCircles++;

            if (vCircle == null) {
                break;
            }

            pt.x = Math.round(vCircle[0]);
            pt.y = Math.round(vCircle[1]);
            radius = (int) Math.round(vCircle[2]);

            // draw circle around detected circle
            Core.circle(rgba, pt, radius, mBlue, thickness);
        }

        // denoise the image
        Mat erode = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(9, 9));
        Imgproc.erode(gray, gray, erode);
        Mat dilate = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(9, 9));
        Imgproc.dilate(gray, gray, dilate);
        Imgproc.dilate(gray, gray, dilate);

        //find contours
        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // iterate over all the objects
        // todo remove close points
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        for (int i = 0; i < contours.size(); i++)
        {
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());

            // square
            if (points.total() == 4) {
                cntSquares++;
                Rect rect = Imgproc.boundingRect(points);
                Core.rectangle(rgba, new Point(rect.x, rect.y), new Point(rect.x+rect.width, rect.y+rect.height), mGreen, thickness);
            }
            // triangle
            else if (points.total() == 3) {
                cntTriangles++;
                Rect rect = Imgproc.boundingRect(points);
                Core.rectangle(rgba, new Point(rect.x, rect.y), new Point(rect.x+rect.width, rect.y+rect.height), mRed, thickness);
            }
        }

        // add number of squares, triangles and circles to the image
        Core.putText(rgba, ("squares: " + String.valueOf(cntSquares) +
                ", triangles: " + String.valueOf(cntTriangles) +
                ", circles: " + String.valueOf(cntCircles)), new Point(10, 30), 0, 1.2, mGreen);

        // show the image
        Utils.matToBitmap(rgba, scaled);
        scaled.prepareToDraw();
        iv.setImageBitmap(scaled);
        iv.postInvalidate();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_color_editing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
