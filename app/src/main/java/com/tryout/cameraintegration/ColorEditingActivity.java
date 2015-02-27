package com.tryout.cameraintegration;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ColorEditingActivity extends Activity {

    private Bitmap mBitmap;
    private ImageView iv;
    private File mediaFile;
    private String writeBmp;
    private Scalar color = new Scalar (100, 100, 255);

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

    // load more openCV stuff
    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_editing);

        // Retrieves saved file path/name
        Intent intent = getIntent();
        String mFileName = intent.getStringExtra(MainActivity.EXTRA_FILE_NAME);
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
        mBitmap.prepareToDraw();
        iv.setImageBitmap(mBitmap);
        iv.postInvalidate();
        //Log.d("BITMAP", "Is mutable: "+ mBitmap.isMutable());
    }

    // Just to know which button was pressed based on their IDs, calls scanColors(int)
    public void decideColors(View v){
        switch(v.getId()){
            case(R.id.button_detection):
                detectShape(scaleDown(mBitmap, 0.1f, true));
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

    public void detectShape(Bitmap scaled) {
        Mat rgba = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Utils.bitmapToMat(scaled, rgba);

        // convert image to grayscale
        Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGBA2GRAY, 4);

        // Convert to binary image
        int maxThreshold = 255;
        int threshold = 70;
        Imgproc.threshold(rgba, rgba, threshold, maxThreshold, Imgproc.THRESH_BINARY);

        // denoise the image
        Mat erode = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(9, 9));
        Imgproc.erode(rgba, rgba, erode);
        Mat dilate = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(9, 9));
        Imgproc.dilate(rgba, rgba, dilate);

        Imgproc.findContours(rgba, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        Button button = (Button)findViewById(R.id.button_detection);
        button.setText(String.valueOf(contours.size()));

        // iterate over all the objects
        for (int i = 0; i < contours.size(); i++)
        {
            // negative value will fill the shape, otherwise it will be the line thickness
            Imgproc.drawContours(rgba, contours, i, color, -1);
        }

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
