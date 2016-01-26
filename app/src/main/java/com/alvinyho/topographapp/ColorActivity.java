package com.alvinyho.topographapp;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.print.PrintHelper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ColorActivity extends AppCompatActivity {

    boolean leftDone = false,
            rightDone = false;

    boolean left_isError = false,
            right_isError = false;


    Uri left_bitmap_uri, right_bitmap_uri;
    ImageView left_imageView, right_imageView;

    Mat final_result_l, final_result_r;

    int originalCol;
    int originalRow;


    Bitmap left_result, right_result;
    ImageView saveImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        ImageView goBackButton = (ImageView) findViewById(R.id.backButton);
        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                v.setAlpha(0.2f);
                finish();
            }
        });



        saveImage = (ImageView) findViewById(R.id.save);
        saveImage.setVisibility(View.INVISIBLE);

        saveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String logtag = "SAVEIMAGE";
                Log.d(logtag, "SAVING IMAGE");

                v.setAlpha(0.2f);
                reset_icon_to_color(v);

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
                Date now = new Date();
                String fileName = formatter.format(now);
                saveImage(final_result_l, "leftFoot_" + fileName + ".png");
                saveImage(final_result_r, "rightFoot_" + fileName + ".png");
            }
        });


        final Button left_print_button = (Button) findViewById(R.id.left_foot_print_button);
        final Button right_print_button = (Button) findViewById(R.id.right_foot_print_button);

        left_print_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String logtag = "LEFTPRINT";
                Log.d(logtag, "LEFT PRINT IMAGE CLICKED");

                delayAfterClick(v);


                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                left_result.compress(Bitmap.CompressFormat.JPEG, 70, bos);
                byte[] bitmapdata = bos.toByteArray();
                ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);

                Bitmap resizedBitmap = createScaledBitmapFromStream(bs, 2125, 4000);  // TODO: Scale to multiplier of size of the foot

                doPhotoPrintSpecific(v, resizedBitmap);
                left_print_button.setBackgroundResource(R.drawable.selected_print);


            }
        });

        right_print_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String logtag = "RIGHTPRINT";
                Log.d(logtag, "RIGHT PRINT IMAGE CLICKED");

                delayAfterClick(v);


                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                right_result.compress(Bitmap.CompressFormat.JPEG, 70, bos);
                byte[] bitmapdata = bos.toByteArray();
                ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);

                Bitmap resizedBitmap = createScaledBitmapFromStream(bs, 2125, 4000);  // TODO: Scale to multiplier of size of the foot

                doPhotoPrintSpecific(v, resizedBitmap);
                right_print_button.setBackgroundResource(R.drawable.selected_print);

            }
        });


        TextView start_over_text = (TextView) findViewById(R.id.start_over_text);
        ImageView start_over_button = (ImageView) findViewById(R.id.start_over_button);

        start_over_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOver();
            }
        });

        start_over_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOver();
            }
        });

        final ViewStub loading = ((ViewStub) findViewById(R.id.stub_import));
        loading.setVisibility(View.VISIBLE);


        Intent intent = getIntent();
        left_bitmap_uri = Uri.parse(intent.getStringExtra("leftUri"));
        right_bitmap_uri = Uri.parse(intent.getStringExtra("rightUri"));
        left_imageView = (ImageView) findViewById(R.id.left_foot_color);
        right_imageView = (ImageView) findViewById(R.id.right_foot_color);



        new Thread(new Runnable() {
            public void run() {
                final Bitmap bitmap = runCanny(R.id.left_foot_color, left_bitmap_uri, 0);
                left_imageView.post(new Runnable(){
                    @Override
                    public void run() {
                        left_imageView.setImageBitmap(bitmap);
                        leftDone = true;
                        remove_loading(loading);
                        left_result = bitmap;
                    }
                });
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                final Bitmap bitmap = runCanny(R.id.right_foot_color, right_bitmap_uri, 1);
                right_imageView.post(new Runnable(){
                    @Override
                    public void run() {
                        right_imageView.setImageBitmap(bitmap);
                        rightDone = true;
                        remove_loading(loading);
                        right_result = bitmap;
                    }
                });
            }
        }).start();


        left_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(left_isError){
                    finish();
                }
                loadPhoto(left_imageView, left_imageView.getMaxWidth(), left_imageView.getMaxHeight());

            }
        });

        right_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(right_isError){
                    finish();
                }
                loadPhoto(right_imageView, right_imageView.getMaxWidth(), right_imageView.getMaxHeight());
            }
        });






    }

    private void doPhotoPrintSpecific(View v, Bitmap bm) {
//        createCombinedImage(v);
        PrintHelper photoPrinter = new PrintHelper(this);
        photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FILL);

//        Bitmap l_bitmap = left_result;
//        Bitmap r_bitmap = right_result;
        Bitmap bitmap = bm;
        photoPrinter.printBitmap("single.jpg - test print", bitmap);



    }


    /**
     * Read the image from the stream and create a bitmap scaled to the desired
     * size.  Resulting bitmap will be at least as large as the
     * desired minimum specified dimensions and will keep the image proportions
     * correct during scaling.
     */
    protected Bitmap createScaledBitmapFromStream(InputStream s, int minimumDesiredBitmapWidth, int minimumDesiredBitmapHeight ) {

        final BufferedInputStream is = new BufferedInputStream(s, 32*1024);
        try {
            final BitmapFactory.Options decodeBitmapOptions = new BitmapFactory.Options();
            // For further memory savings, you may want to consider using this option
            // decodeBitmapOptions.inPreferredConfig = Config.RGB_565; // Uses 2-bytes instead of default 4 per pixel

            if( minimumDesiredBitmapWidth >0 && minimumDesiredBitmapHeight >0 ) {
                final BitmapFactory.Options decodeBoundsOptions = new BitmapFactory.Options();
                decodeBoundsOptions.inJustDecodeBounds = true;
                is.mark(32*1024); // 32k is probably overkill, but 8k is insufficient for some jpgs
                BitmapFactory.decodeStream(is,null,decodeBoundsOptions);
                is.reset();

                final int originalWidth = decodeBoundsOptions.outWidth;
                final int originalHeight = decodeBoundsOptions.outHeight;

                // inSampleSize prefers multiples of 2, but we prefer to prioritize memory savings
                decodeBitmapOptions.inSampleSize= Math.max(1,Math.min(originalWidth / minimumDesiredBitmapWidth, originalHeight / minimumDesiredBitmapHeight));

            }

            return BitmapFactory.decodeStream(is,null,decodeBitmapOptions);

        } catch( IOException e ) {
            throw new RuntimeException(e); // this shouldn't happen
        } finally {
            try {
                is.close();
            } catch( IOException ignored ) {}
        }

    }

    private void remove_loading(ViewStub viewstub){
        if(leftDone && rightDone) {
            viewstub.setVisibility(View.INVISIBLE);
            enable_save();
        }
    }

    private void enable_save(){
        saveImage.setVisibility(View.VISIBLE);
    }


    private void loadPhoto(ImageView imageView, int width, int height) {

        ImageView tempImageView = imageView;


        AlertDialog.Builder imageDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.screen_drawing_room,
                (ViewGroup) findViewById(R.id.rlid));
        ImageView bg_image = (ImageView) layout.findViewById(R.id.drawing_target_image);
        bg_image.setImageDrawable(tempImageView.getDrawable());


        // DRAWING PAD VIEW
        DrawingView mDrawingView=new DrawingView(this);

        Log.d("created", "drawingview");


        imageDialog.setView(layout);

        LinearLayout mDrawingPad=(LinearLayout) layout.findViewById(R.id.view_drawing_pad);
        mDrawingPad.addView(mDrawingView);


        imageDialog.setPositiveButton("OK", new DialogInterface.OnClickListener(){

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });


        imageDialog.create();
        imageDialog.show();
    }


    private Mat rotateMat(Mat mat){

        if (mat.width() > mat.height()) {
            Core.transpose(mat, mat);
            Core.flip(mat, mat, 1);
            return mat;
        } else
            return mat;
    }

    private void saveImage (Mat mat, String file_name){
        String logtag = "SAVE IMAGE METHOD";
        Mat temp_mat = new Mat();

        // rotateMat if needed
        Mat rotated = rotateMat(mat);

        Imgproc.cvtColor(rotated, temp_mat, Imgproc.COLOR_RGBA2BGR);

        // Create the file in sd card
        File path = new File(Environment.getExternalStorageDirectory() + "/HarrisImages/");
        path.mkdirs();
        File file = new File(path, file_name);
        String full_path_filename = file.toString();
        Boolean bool;
        Log.d(logtag, "WROTE FILE TO " + full_path_filename);
        bool = Imgcodecs.imwrite(full_path_filename, temp_mat, new MatOfInt(Imgcodecs.CV_IMWRITE_JPEG_QUALITY));

        if (bool == true){
            Context context = getApplicationContext();
            CharSequence text = "Saved Image";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        } else {
            Context context = getApplicationContext();
            CharSequence text = "Could not save Image";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }



    }

    private Mat cannyMat(int view_id, Uri uri){
        Mat imageMat = retrieveFootBitmap(uri);

        //Initialize Mats
        Mat original = imageMat.clone();
        Mat edges = new Mat();
        Mat gray = new Mat();
        Mat med_blurred = new Mat();

        //RGB to Gray
        Imgproc.cvtColor(imageMat, gray, Imgproc.COLOR_RGB2GRAY);
//        Imgproc.GaussianBlur(gray, med_blurred, new Size(5.0, 3.0), 0);

        //Blur Image
        Imgproc.medianBlur(gray, med_blurred, 9);

        //Adaptive Thresh
        Imgproc.adaptiveThreshold(med_blurred, med_blurred, 255,
                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 18);

        //Equalize Histogram
        Imgproc.equalizeHist(med_blurred, med_blurred);

        // Edge Detection
        Imgproc.Canny(med_blurred, edges, 100, 300);

        //Dialate Edges
        Imgproc.dilate(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(19, 19)));

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges.clone(), contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                double lhs_contourArea = Imgproc.contourArea(lhs);
                double rhs_contourArea = Imgproc.contourArea(rhs);
                return Double.compare(lhs_contourArea, rhs_contourArea);
            }
        });

        Collections.reverse(contours);

        List<MatOfPoint> foundRect = new ArrayList<MatOfPoint>();

        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint2f maxCurve = new MatOfPoint2f();

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint temp = contours.get(i);
            MatOfPoint2f myPt = new MatOfPoint2f();
            temp.convertTo(myPt, CvType.CV_32FC2);
            double peri = Imgproc.arcLength(myPt, true);
            MatOfPoint2f approxContour2f = new MatOfPoint2f();
            MatOfPoint approxContour = new MatOfPoint();
            Imgproc.approxPolyDP(myPt, approxContour2f, 0.02 * peri, true);
            approxContour2f.convertTo(approxContour, CvType.CV_32S);
            if (approxContour.size().height == 4) {
                foundRect.add(approxContour);
                maxCurve = approxContour2f;
                Rect rect = Imgproc.boundingRect(approxContour);
//                    MatOfPoint = new MatOfRect(ret);
//                    foundRect.add(new MatOfRect(ret));
//                    numberRectFound++;
//                    Core.rectangle(imageMat, rect.tl(), rect.br(), new Scalar(255, 0, 0), 1, 8, 0);
                break;
            }
        }


        Imgproc.drawContours(imageMat, foundRect, -1, new Scalar(255, 0, 0), 5);
        Mat new_image = new Mat(original.size(), CvType.CV_8U);
        Imgproc.cvtColor(new_image, new_image, Imgproc.COLOR_BayerBG2RGB);
        Imgproc.drawContours(new_image, foundRect, -1, new Scalar(255, 255, 255), 5); //will draw the largest square/rectangle

        try {
            double temp_double[] = maxCurve.get(0, 0);
            Point p1 = new Point(temp_double[0], temp_double[1]);
            Imgproc.circle(new_image, new Point(p1.x, p1.y), 20, new Scalar(255, 0, 0), 5); //p1 is colored red
            String temp_string = "Point 1: (" + p1.x + ", " + p1.y + ")";

            temp_double = maxCurve.get(1, 0);
            Point p2 = new Point(temp_double[0], temp_double[1]);
            Imgproc.circle(new_image, new Point(p2.x, p2.y), 20, new Scalar(0, 255, 0), 5); //p2 is colored green
            temp_string += "\nPoint 2: (" + p2.x + ", " + p2.y + ")";

            temp_double = maxCurve.get(2, 0);
            Point p3 = new Point(temp_double[0], temp_double[1]);
            Imgproc.circle(new_image, new Point(p3.x, p3.y), 20, new Scalar(0, 255, 0), 5); //p2 is colored green
            temp_string += "\nPoint 3: (" + p3.x + ", " + p3.y + ")";


            temp_double = maxCurve.get(3, 0);
            Point p4 = new Point(temp_double[0], temp_double[1]);
            Imgproc.circle(new_image, new Point(p4.x, p4.y), 20, new Scalar(0, 255, 0), 5); //p2 is colored green
            temp_string += "\nPoint 4: (" + p4.x + ", " + p4.y + ")";


            ArrayList<org.opencv.core.Point> corners = new
                    ArrayList<org.opencv.core.Point>();
            ArrayList<org.opencv.core.Point> top = new
                    ArrayList<org.opencv.core.Point>();
            ArrayList<org.opencv.core.Point> bottom = new
                    ArrayList<org.opencv.core.Point>();

            corners.add(p1);
            corners.add(p2);
            corners.add(p3);
            corners.add(p4);

            Point centeroid = new Point(0, 0);
            for (Point point : corners) {
                centeroid.x += point.x;
                centeroid.y += point.y;
            }
            centeroid.x /= corners.size();
            centeroid.y /= corners.size();


            for (int i = 0; i < corners.size(); i++) {
                if (corners.get(i).y < centeroid.y)
                    top.add(corners.get(i));
                else
                    bottom.add(corners.get(i));
            }

            org.opencv.core.Point topLeft = top.get(0).x > top.get(1).x ?
                    top.get(1) : top.get(0);
            org.opencv.core.Point topRight = top.get(0).x > top.get(1).x ?
                    top.get(0) : top.get(1);
            org.opencv.core.Point bottomLeft = bottom.get(0).x > bottom.get(1).x ?
                    bottom.get(1) : bottom.get(0);
            org.opencv.core.Point bottomRight = bottom.get(0).x > bottom.get(1).x ?
                    bottom.get(0) : bottom.get(1);
            corners.clear();
            corners.add(topLeft);
            corners.add(topRight);
            corners.add(bottomRight);
            corners.add(bottomLeft);


            Mat correctedImage = new Mat(1440, 2560, original.type());

            originalCol = original.cols();
            originalRow = original.rows();
            //2125 4000

            Mat srcPoints = Converters.vector_Point2f_to_Mat(corners);
            Mat destPoints = Converters.vector_Point2f_to_Mat(Arrays.asList(new Point[]{
                    new Point(0, 0),
                    new Point(correctedImage.cols(), 0),
                    new Point(correctedImage.cols(), correctedImage.rows()),
                    new Point(0, correctedImage.rows())
            }));

            Mat transformation = Imgproc.getPerspectiveTransform(srcPoints, destPoints);
            Imgproc.warpPerspective(original, correctedImage, transformation,
                    correctedImage.size());

            Mat border_removed = removeBorder(correctedImage, 80);
            Mat result = border_removed.clone();

            Mat grayed = new Mat();
            Imgproc.cvtColor(result, grayed, Imgproc.COLOR_RGB2GRAY);
            Imgproc.adaptiveThreshold(grayed, result, 255,
                    Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY,17,20);


            Imgproc.GaussianBlur(result, result, new Size(7.0, 5.0), 0);
            Imgproc.equalizeHist(result, result);

            result = conv2(result, 50);

                 /*Color Map*/

            int imgHeight = result.height();
            int imgWidth = result.width();
            imgHeight = imgHeight / 5;
            imgWidth = imgWidth / 5;

            //DownScale
            Size smaller = new Size(imgWidth, imgHeight);
            int ze = 0;
            Imgproc.resize(result, result, smaller, ze, ze, Imgproc.INTER_LINEAR);

            //Sort grid with index
            Mat sorted = sortIntensity(result);
            Mat sortedinv = new Mat();
            Core.bitwise_not(result, result);

            //Find number of zeros
            Mat result2 = result;
            int numnonZeros = Core.countNonZero(result);
            int numZeros = imgHeight * imgWidth - numnonZeros;
            String numz = String.format("%d", numnonZeros);
            Log.d("numnonzeros", numz);


            Mat colorImg = new Mat(imgHeight, imgWidth, CvType.CV_8UC3, new Scalar(255, 255, 255));

            int counter = numZeros;
            int currInd = 0;
            double[] curr;
            int currCol = 0;
            int currRow = 0;
            double[] bgr = {255, 255, 255};
            double steps = (imgWidth * imgHeight - numZeros);
            double steps5 = steps / 6;
            double[] Blue = {69, 129, 174};
            double[] Cyan = {47, 163, 208};
            double[] Green = {116, 185, 68};
            double[] Yellow = {251, 218, 30};
            double[] Orange = {223, 134, 44};
            double[] Red = {215, 37, 51};
            double[] White = {255,255,255};
            double currBGRArr;

            IMCcolors WhiteBlue = new IMCcolors(White,Blue,steps/6);
            IMCcolors BlueCyan = new IMCcolors(Blue, Cyan, steps / 6);
            IMCcolors CyanGreen = new IMCcolors(Cyan, Green, steps / 6);
            IMCcolors GreenYellow = new IMCcolors(Green, Yellow, steps / 6);
            IMCcolors YellowOrange = new IMCcolors(Yellow, Orange, steps / 6);
            IMCcolors OrangeRed = new IMCcolors(Orange, Red, steps / 6);


            double[] currBGR;
            Log.d("Time", "before loop");
            while (counter < imgWidth * imgHeight) {
                curr = sorted.get(0, counter);
                currInd = (int) curr[0];
                currCol = currInd / imgWidth;
                currRow = currInd % imgWidth;

                if (counter < (numZeros+steps5)) {
                    currBGR = WhiteBlue.getNextFloat();
                    colorImg.put(currCol,currRow,currBGR);
                } else if (counter < (numZeros + 2*steps5)) {
                    currBGR = BlueCyan.getNextFloat();
                    colorImg.put(currCol, currRow, currBGR);
                } else if (counter < (numZeros + 3 * steps5)) {
                    currBGR = CyanGreen.getNextFloat();
                    colorImg.put(currCol, currRow, currBGR);
                    ;
                } else if (counter < (numZeros + 4 * steps5)) {
                    currBGR = GreenYellow.getNextFloat();
                    colorImg.put(currCol, currRow, currBGR);
                } else if (counter < (numZeros + 5 * steps5)) {
                    currBGR = YellowOrange.getNextFloat();
                    colorImg.put(currCol, currRow, currBGR);
                } else if (counter < (numZeros + 6 * steps5)) {
                    currBGR = OrangeRed.getNextFloat();
                    colorImg.put(currCol, currRow, currBGR);
                }
                counter++;


            }

            String counterout = String.format("%d", counter);
            Log.d("counter", counterout);

            //Interpolate
            Size bigger = new Size(imgWidth * 5, imgHeight * 5);
            int interp = 0;
            double fx = 0;
            double fy = 0;
            Imgproc.resize(colorImg, result, bigger, fx, fy, Imgproc.INTER_CUBIC);
            return result;
        } catch(Exception e) {
            return null;
        }

    }

    private Mat removeBorder(Mat original, int amount){
        Mat roi = original.submat(amount, original.height()-amount, 100, original.width()-amount);
        return roi;

    }

    public Mat conv2(Mat inputMat, int kernel_size){
        Mat dst = new Mat();
        //double kern = (double)1/Math.pow(kernel_size, 2);
        Mat kernel = new Mat(kernel_size, kernel_size, CvType.CV_32F, new Scalar((double)1/Math.pow(kernel_size, 2)));
        //Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(kern,kern));
//        Imgproc.filter2D(inputMat, dst, -1, kernel, new Point(-1, -1), 0, -1 );

        Imgproc.filter2D(inputMat, dst, -1, kernel);
        return dst;

    }

    public Mat sortIntensity(Mat inputMat) {

        //Initialize Vars
        Mat dst = new Mat();
        Mat reshMat = inputMat.reshape(0, 1);
        Core.sortIdx(reshMat, dst, 16);

        return dst;
    }


    private Mat retrieveFootBitmap(Uri uri){
        return uriToMat(uri);
    }

    private Mat uriToMat(Uri uri){

        try {
            Bitmap bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            Mat imageMat = new Mat (bm.getHeight(), bm.getWidth(), CvType.CV_8UC1, new Scalar(4));

            Utils.bitmapToMat(bm, imageMat);
            return imageMat;

        } catch (Exception e) {
            String logtag = "URI TO MAT ERR";
            Log.d(logtag, "Result Mat not found");
        }

        return null;
    }

    public Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();


        if (w > h) {

            android.graphics.Matrix mtx = new android.graphics.Matrix();
            mtx.postScale(0.5f, 0.5f);
            mtx.postRotate(degree);

            return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
        }
        return bitmap;
    }

    private Bitmap runCanny(int view_id, Uri uri, int side) {
        String logtag = "RUN CANNY";
        Log.d(logtag, "RUNNING CANNY");


        Mat result = cannyMat(view_id, uri);

        Log.d(logtag, "ORIGINAL COL" + String.valueOf(originalCol));
        Log.d(logtag, "ORIGINAL ROW" + String.valueOf(originalRow));

        if (side == 0) {
            final_result_l = result;
        }
        if (side == 1){
            final_result_r = result;
        }
        Bitmap cropped_bm = null;
        try{
            cropped_bm = Bitmap.createBitmap(result.cols(), result.rows(),
                    Bitmap.Config.ARGB_8888);
        }
        catch (NullPointerException e){
            if (side == 0){
                left_isError = true;
            }
            if (side == 1){
                right_isError = true;
            }
            return BitmapFactory.decodeResource(getResources(), R.drawable.error_message);
        }

        Utils.matToBitmap(result, cropped_bm);
        cropped_bm = rotate(cropped_bm, 90);
        return cropped_bm;



//        try{
//            Mat result = cannyMat(view_id, uri);
//
//            if (side == 0) {
//                final_result_l = result;
//            }
//            if (side == 1){
//                final_result_r = result;
//            }
//            Bitmap cropped_bm = Bitmap.createBitmap(result.cols(), result.rows(),
//                    Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(result, cropped_bm);
//
//            cropped_bm = rotate(cropped_bm, 90);
//            return cropped_bm;
//        } catch (Exception e) {
//            Bitmap error_bm = BitmapFactory.decodeResource(getResources(),
//                    R.drawable.error_message);
//
//            if (side == 0){
//                left_isError = true;
//                left_imageView.setImageBitmap(error_bm);
//
//                // TODO: Change on_click handling
//
//
//
//            }
//            if (side == 1){
//                right_isError = true;
//                right_imageView.setImageBitmap(error_bm);
//                // TODO: Chane on_click handling
//            }
//
//
//            Log.d (logtag, "Null exception error found. Image could not be created.");
//            e.printStackTrace();
//            return error_bm;
//        }
//        return BitmapFactory.decodeResource(getResources(), R.drawable.good_feet_logo);

    }

    private void delayAfterClick(View v){
        final Button printButton = (Button) v;
        printButton.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                ColorActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        printButton.setEnabled(true);

                    }
                });
            }
        }).start();
    }



    private void reset_icon_to_color(View v){
        final Button icon = (Button) v;
        icon.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                ColorActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        icon.setEnabled(true);

                    }
                });
            }
        }).start();
    }




    public void goToMain() {
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }


    private void startOver(){
        //finish();
        DialogFragment newFragment = ResetDialogFragment.newInstance(
                "Are you sure you want to go back to the beginning?");
        newFragment.show(getFragmentManager(), "dialog");

    }

}
