package com.alvinyho.topographapp;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Mat;
import org.w3c.dom.Text;

import java.io.File;

public class CaptureActivity extends AppCompatActivity {

    static { System.loadLibrary("opencv_java3"); }

    ImageView next_button;
    TextView next_text;

    ImageView left_foot_imageView;
    ImageView right_foot_imageView;

//    Boolean next_enabled = false;
    Boolean LEFT_EXIST = false;
    Boolean RIGHT_EXIST = false;

    Uri leftBitmapUri, rightBitmapUri;

    private Uri imageUri;
    private static int TAKE_PICTURE_LEFT = 4201;
    private static int TAKE_PICTURE_RIGHT = 4202;




    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        // Retrieve the buttons for next and set their opacity to .2
        next_button = (ImageView) findViewById(R.id.next_button);
        next_text = (TextView) findViewById(R.id.next_text);
        next_button.setAlpha(.2f);
        next_text.setAlpha(.2f);

        verifyStoragePermissions(CaptureActivity.this);


        left_foot_imageView = (ImageView) findViewById(R.id.left_foot);
        right_foot_imageView = (ImageView) findViewById(R.id.right_foot);

        left_foot_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto(v, "left");
            }
        });


        right_foot_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto(v, "right");
            }
        });

        ImageView resetButton = (ImageView) findViewById((R.id.closeButton));

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWarningDialog();
            }
        });



        next_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoNextPage();
            }
        });

        next_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoNextPage();
            }
        });



    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        if (w > h) {
            android.graphics.Matrix mtx = new android.graphics.Matrix();
            mtx.postRotate(degree);

            return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
        }
        return bitmap;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String logtag = "onActivityResult in CaptureActivity";
        switch (requestCode){
            case 4201:
                if (resultCode == Activity.RESULT_OK) {
                    Uri selectedImage = imageUri;
                    getContentResolver().notifyChange(selectedImage, null);
                    ImageView imageView = (ImageView) findViewById(R.id.left_foot);
                    ContentResolver cr = getContentResolver();
                    Bitmap bmap;

                    Log.d(logtag, "attempting to process bitmap");
                    try {
                        bmap = android.provider.MediaStore.Images.Media.getBitmap(cr, selectedImage);
                        bmap = rotate(bmap, 90);
                        imageView.setImageBitmap(bmap);

                        leftBitmapUri = selectedImage;

                        // notify the user
                        Toast.makeText(CaptureActivity.this, selectedImage.toString(), Toast.LENGTH_LONG).show();
                        LEFT_EXIST = true;
                        enable_next();

                    } catch (Exception e) {
                        Toast.makeText(CaptureActivity.this, "failed to load", Toast.LENGTH_LONG).show();
                        Log.e(logtag, e.toString());
                    }
                }


                break;
            case 4202:
                if (resultCode == Activity.RESULT_OK) {
                    Uri selectedImage = imageUri;
                    getContentResolver().notifyChange(selectedImage, null);
                    ImageView imageView = (ImageView) findViewById(R.id.right_foot);
                    ContentResolver cr = getContentResolver();
                    Bitmap bmap;

                    Log.d(logtag, "attempting to process bitmap");
                    try {
                        bmap = android.provider.MediaStore.Images.Media.getBitmap(cr, selectedImage);
                        bmap = rotate(bmap, 90);
                        imageView.setImageBitmap(bmap);

                        rightBitmapUri = selectedImage;
                        // notify the user
                        Toast.makeText(CaptureActivity.this, selectedImage.toString(), Toast.LENGTH_LONG).show();
                        RIGHT_EXIST = true;
                        enable_next();


                    } catch (Exception e) {
                        Toast.makeText(CaptureActivity.this, "failed to load", Toast.LENGTH_LONG).show();
                        Log.e(logtag, e.toString());
                    }
                }
                break;

        }
    }





    public void takePhoto(View v, String side){
        // tell the phone we want to use the camera
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        // create a new temp file called pic.jpg in the "pictures" storage area of the phone


        if (side == "left") {
            File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "temp_left_pic.jpg");
            // take the return data and store it in the temp file "pic.jpg"
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
            // stor the temp photo uri so we can find it later
            imageUri = Uri.fromFile(photo);
            // start the camera
            startActivityForResult(intent, TAKE_PICTURE_LEFT);
        }
        if (side == "right"){
            File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "temp_right_pic.jpg");
            // take the return data and store it in the temp file "pic.jpg"
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
            // stor the temp photo uri so we can find it later
            imageUri = Uri.fromFile(photo);
            // start the camera
            startActivityForResult(intent, TAKE_PICTURE_RIGHT);
        }

    }



    /*
     *  Enables the next button if next_enabled has been toggled
     */
    private void enable_next(){
        next_button.setAlpha(1.0f);
        next_text.setAlpha(1);
    }




    void showWarningDialog(){
        DialogFragment newFragment = WarningDialogFragment.newInstance(
                "Are you sure you want to reset pictures?");
        newFragment.show(getFragmentManager(), "dialog");
    }



    public void resetImages(){
        left_foot_imageView.setImageResource(R.drawable.left_foot);
        right_foot_imageView.setImageResource(R.drawable.right_foot);
        leftBitmapUri = null;
        rightBitmapUri = null;

        disable_next_txt_btn();
    }

    public void disable_next_txt_btn(){
        next_button.setAlpha(0.2f);
        next_text.setAlpha(0.2f);
        LEFT_EXIST = false;
        RIGHT_EXIST = false;
    }



    public void gotoNextPage(){
        if(LEFT_EXIST && RIGHT_EXIST){
            Intent intent = new Intent(this, ColorActivity.class);
            intent.putExtra("leftUri", leftBitmapUri.toString());
            intent.putExtra("rightUri", rightBitmapUri.toString());

            startActivity(intent);

        }
    }


}
