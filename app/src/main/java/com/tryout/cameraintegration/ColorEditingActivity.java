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
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class ColorEditingActivity extends Activity {

    private Bitmap mBitmap;
    private ImageView iv;
    private File mediaFile;
    private String writeBmp;

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
        iv.setImageBitmap(mBitmap);
        //Log.d("BITMAP", "Is mutable: "+ mBitmap.isMutable());
    }

    // Just to know which button was pressed based on their IDs, calls scanColors(int)
    public void decideColors(View v){
        switch(v.getId()){
            case(R.id.button_red):
                    scanColors(Color.RED);
                break;
            case(R.id.button_green):
                    scanColors(Color.GREEN);
                break;
            case(R.id.button_blue):
                    scanColors(Color.BLUE);
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

    //Process colors, the int parameter is used to decide which button was pressed based on its color.
    private void scanColors(int mColor) {
        // To turn off, change to 0
        float sat = 0.1f;
        // To turn off, change to 1
        float val = 0.9f;

        Bitmap scaledBitmap = scaleDown(mBitmap, 0.1f, true);

        // Iterates over every pixel on scaledBitmap
        for (int y = 0; y < scaledBitmap.getHeight(); y++) {
            for (int x = 0; x < scaledBitmap.getWidth(); x++) {
                // Gets pixel[x,y] color and turns it into HSV, which is better to compare colors.
                int clr = scaledBitmap.getPixel(x,y);
                float[] hsv = new float[3];
                Color.colorToHSV(clr, hsv);
                switch(mColor){
                    /*
                    COLOR HUE CODES
                    red >= 0 && <=10
                        =>340 && <=359
                    green = >=70 && <= 155
                    blue = >= 180 && <= 260
                     */
                    case(Color.RED):
                        if (hsv[0] >= 0 && hsv[0] <=20 && hsv[1] >= sat && hsv[2] <= val){
                            clr = Color.rgb(255, 0, 0);
                        }else if(hsv[0] >= 340 && hsv[0] <= 359 && hsv[1] >= sat && hsv[2] <= val){
                            clr = Color.rgb(255, 0, 0);
                        }else{
                            clr = Color.rgb(0, 0, 0);
                        }
                        scaledBitmap.setPixel(x, y, clr);
                        break;
                    case(Color.GREEN):
                        if (hsv[0] >= 70 && hsv[0] <=155 && hsv[1] >= sat && hsv[2] <= val){

                            clr = Color.rgb(0, 255, 0);
                        }else{
                            clr = Color.rgb(0, 0, 0);
                        }
                        scaledBitmap.setPixel(x, y, clr);
                        break;
                    case(Color.BLUE):
                        //Log.d("COLORS", "H: ""S: ""V: ")
                        if (hsv[0] >= 180 && hsv[0] <=260 && hsv[1] >= sat && hsv[2] <= val){

                            clr = Color.rgb(0, 0, 255);
                        }else{
                            clr = Color.rgb(0, 0, 0);
                        }
                        scaledBitmap.setPixel(x, y, clr);
                        break;
                }
            }
        }
        scaledBitmap.prepareToDraw();
        iv.setImageBitmap(scaledBitmap);
        iv.postInvalidate();
        savePNG(mColor, scaledBitmap);
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
