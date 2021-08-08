package com.google.android.cameraview.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import io.github.silvaren.easyrs.tools.BuildConfig;

public class ImageActivity extends AppCompatActivity {

    private static final String DEBUG_TAG = "ImageActivity";
    private Bitmap capturedImg;
    private Bitmap bitmapInput;
    private Bitmap bitmapForeground;
    private PointF transparentCenter;
    PointF mid = new PointF();
    Bitmap pictureF;
    Matrix translateF;
    TextDisplay nameText1 = new TextDisplay();
    TextDisplay nameText2 = new TextDisplay();
    ArrayList<TextDisplay> textDisplays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        Bundle data = getIntent().getExtras();
        textDisplays = (ArrayList<TextDisplay>) getIntent().getSerializableExtra("inputs");
        nameText1 = textDisplays.get(0);
        nameText2 = textDisplays.get(1);

        int capturedX = getIntent().getIntExtra("capturedX", -1);
        int capturedY = getIntent().getIntExtra("capturedY", -1);

        if (data != null) {
            ArrayList<Uri> picked_images = (ArrayList<Uri>) ((data.get("images_to_merge")));
            try {
                capturedImg = MediaStore.Images.Media.getBitmap(this.getContentResolver(),picked_images.get(1));

                if (picked_images.get(0) != null) {
                    bitmapInput = MediaStore.Images.Media.getBitmap(this.getContentResolver(), picked_images.get(0));
                } else {
                    bitmapInput = BitmapFactory.decodeResource(this.getResources(), R.drawable.backg);
                }

                if (picked_images.get(2) != null) {
                    bitmapForeground = MediaStore.Images.Media.getBitmap(this.getContentResolver(), picked_images.get(2));
                } else {
                    bitmapForeground = BitmapFactory.decodeResource(this.getResources(), R.drawable.person);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        transparentCenter = new PointF(capturedX != -1 ? capturedX : 720, capturedY != -1 ? capturedY : 160);
        Log.d("transparentCenter", transparentCenter.x + "  " + transparentCenter.y);

        FrameLayout frame = findViewById(R.id.graphics_holder);
        PlayAreaView image = new PlayAreaView(this);
        frame.addView(image);
        image.translate.setTranslate(transparentCenter.x, transparentCenter.y);
        image.invalidate();
    }

    public String bitmapToFile(Bitmap bitmap){
        //create a file to write bitmap data
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileName = "merged" + sdf.format(currentTime) + ".png";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), fileName);
        try {
            file.createNewFile();
        } catch (Exception e) {
            if (io.github.silvaren.easyrs.tools.BuildConfig.DEBUG) e.printStackTrace();
            return "";
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bitmapData = bos.toByteArray();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bitmapData);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            return "";
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return file.getAbsolutePath();
    }

    public void saveImage(View view) {
        Bitmap newb = Bitmap.createBitmap(bitmapInput.getWidth(), bitmapInput.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newb);

        // draw src into canvas
        cv.drawBitmap(bitmapInput, null, new Rect(0,0, bitmapInput.getWidth(), bitmapInput.getHeight()), null);

        cv.drawBitmap(pictureF, translateF, null);
        cv.drawBitmap(bitmapForeground, 0, 0, null);

        Paint paint1 = new Paint();
        paint1.setColor(nameText1.getColor() > -1 ? nameText1.getColor():Color.BLUE);
        paint1.setTextSize(nameText1.getFontSize() > -1 ? nameText1.getFontSize() : 12);
        Typeface tf1 = Typeface.create(nameText1.getFontName() == null ? "Helvetica": nameText1.getFontName(), Typeface.NORMAL);
        paint1.setTypeface(tf1);
        paint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        cv.drawText(nameText1.getName() == null ? "Pallavi Sharma" : nameText1.getName(),
                nameText1.getLocationX() > -1 ? nameText1.getLocationX() : 562, nameText1.getLocationY() > -1 ? nameText1.getLocationY() : 353, paint1);

        Paint paint2 = new Paint();
        paint2.setColor(nameText2.getColor() > -1 ? nameText2.getColor() : Color.BLACK);
        paint2.setTextSize(nameText2.getFontSize() > -1 ? nameText2.getFontSize() : 8);
        Typeface tf2 = Typeface.create(nameText2.getFontName() == null ? "monospace": nameText2.getFontName(), Typeface.NORMAL);
        paint2.setTypeface(tf2);
        paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        cv.drawText(nameText2.getName() == null ? "DAV Public School" : nameText2.getName(),
                nameText2.getLocationX() > -1 ? nameText2.getLocationX() : 562, nameText2.getLocationY() > -1 ? nameText2.getLocationY() : 365, paint2);

        // save all clip
        cv.save();
        // store
        cv.restore();

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(bitmapToFile(newb));
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);

        Toast.makeText(this, "Image Saved!", Toast.LENGTH_SHORT).show();
    }


    private class PlayAreaView extends View {

        private GestureDetector gestures;
        private ScaleGestureDetector scaleGestureDetector;
        private Matrix translate;
        private Bitmap picture;
        private Matrix animateStart;
        private Interpolator animateInterpolator;
        private long startTime;
        private long endTime;
        private float totalAnimDx;
        private float totalAnimDy;

        public void onAnimateMove(float dx, float dy, long duration) {
            animateStart = new Matrix(translate);
            animateInterpolator = new OvershootInterpolator();
            startTime = System.currentTimeMillis();
            endTime = startTime + duration;
            totalAnimDx = dx;
            totalAnimDy = dy;
            post(new Runnable() {

                public void run() {
                    onAnimateStep();
                }
            });
        }

        private void onAnimateStep() {
            long curTime = System.currentTimeMillis();
            float percentTime = (float) (curTime - startTime) / (float) (endTime - startTime);
            float percentDistance = animateInterpolator.getInterpolation(percentTime);
            float curDx = percentDistance * totalAnimDx;
            float curDy = percentDistance * totalAnimDy;
            translate.set(animateStart);
            onMove(curDx, curDy);

            Log.v(DEBUG_TAG, "We're " + percentDistance + " of the way there!");
            if (percentTime < 1.0f) {
                post(new Runnable() {

                    public void run() {
                        onAnimateStep();
                    }
                });
            }
        }

        public void onMove(float dx, float dy) {
            translate.postTranslate(dx, dy);
            invalidate();
        }

        public void onResetLocation() {
            translate.reset();
            invalidate();
        }

        public void onSetLocation(float dx, float dy) {
            translate.postTranslate(dx, dy);
        }

        public PlayAreaView(final Context context) {
            super(context);
            translate = new Matrix();
            scaleGestureDetector = new ScaleGestureDetector(ImageActivity.this, new ScaleGestureDetector.OnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    Log.v(DEBUG_TAG, "onScale");
                    onScaling(detector.getScaleFactor());
                    return true;
                }

                @Override
                public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                    return true;
                }

                @Override
                public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

                }
            });
            gestures = new GestureDetector(ImageActivity.this, new GestureListener(this));
            picture = capturedImg;
        }


        protected void onDraw(Canvas canvas) {
            Log.v(DEBUG_TAG, "onDraw");

            canvas.drawBitmap(bitmapInput, 0, 0, null);
            canvas.drawBitmap(picture, translate, null);
            canvas.drawBitmap(bitmapForeground, 0, 0, null);

            Paint paint1 = new Paint();
            paint1.setColor(nameText1.getColor() != -1 ? nameText1.getColor():Color.BLUE);
            paint1.setTextSize(nameText1.getFontSize() > -1 ? nameText1.getFontSize() : 12);
            Typeface tf1 = Typeface.create(nameText1.getFontName() == null ? "Helvetica": nameText1.getFontName(), Typeface.NORMAL);
            paint1.setTypeface(tf1);
            paint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
            canvas.drawText(nameText1.getName() == null ? "Pallavi Sharma" : nameText1.getName(),
                    nameText1.getLocationX() > -1 ? nameText1.getLocationX() : 562, nameText1.getLocationY() > -1 ? nameText1.getLocationY() : 353, paint1);

            Paint paint2 = new Paint();
            paint2.setColor(nameText2.getColor() != -1 ? nameText2.getColor() : Color.BLACK);
            paint2.setTextSize(nameText2.getFontSize() > -1 ? nameText2.getFontSize() : 8);
            Typeface tf2 = Typeface.create(nameText2.getFontName() == null ? "monospace": nameText2.getFontName(), Typeface.NORMAL);
            paint2.setTypeface(tf2);
            paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
            canvas.drawText(nameText2.getName() == null ? "DAV Public School" : nameText2.getName(),
                    nameText2.getLocationX() > -1 ? nameText2.getLocationX() : 562, nameText2.getLocationY() > -1 ? nameText2.getLocationY() : 365, paint2);

            Matrix m = canvas.getMatrix();
            Log.d(DEBUG_TAG, "Matrix: " + translate.toShortString());
            Log.d(DEBUG_TAG, "Canvas: " + m.toShortString());
            pictureF = picture;
            translateF = translate;
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (event.getPointerCount() == 1) {
                gestures.onTouchEvent(event);
            } else if (event.getPointerCount() == 2) {
                midPoint(mid, event);
                scaleGestureDetector.onTouchEvent(event);
            }
            return true;
        }

        public void onScaling(float mScale) {
            mScale = Math.max(0.1f, Math.min(mScale, 5.0f));
            translate.postScale(mScale, mScale, mid.x, mid.y);
            invalidate();
        }
    }

    /** Calculate the mid point of the first two fingers */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private class GestureListener implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

        PlayAreaView view;

        public GestureListener(PlayAreaView view) {
            this.view = view;
        }

        public boolean onDown(MotionEvent e) {
            Log.v(DEBUG_TAG, "onDown");
            return true;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, final float velocityX, final float velocityY) {

            Log.v(DEBUG_TAG, "onFling");
//            final float distanceTimeFactor = 0.4f;
//            final float totalDx = (distanceTimeFactor * velocityX / 2);
//            final float totalDy = (distanceTimeFactor * velocityY / 2);
//
//            view.onAnimateMove(totalDx, totalDy, (long) (1000 * distanceTimeFactor));

            return false;
        }

        public boolean onDoubleTap(MotionEvent e) {
            Log.v(DEBUG_TAG, "onDoubleTap");
//            view.onResetLocation();
            return false;
        }

        public void onLongPress(MotionEvent e) {
            Log.v(DEBUG_TAG, "onLongPress");
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.v(DEBUG_TAG, "onScroll");
            view.onMove(-distanceX, -distanceY);
            return true;
        }

        public void onShowPress(MotionEvent e) {
            Log.v(DEBUG_TAG, "onShowPress");
        }

        public boolean onSingleTapUp(MotionEvent e) {
            Log.v(DEBUG_TAG, "onSingleTapUp");
            return false;
        }

        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.v(DEBUG_TAG, "onDoubleTapEvent");
            return false;
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.v(DEBUG_TAG, "onSingleTapConfirmed");
            return false;
        }
    }
}