package com.google.android.cameraview.demo;


import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.CameraViewImpl;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpEntity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Callable;

import io.github.silvaren.easyrs.tools.BuildConfig;
import io.github.silvaren.easyrs.tools.Nv21Image;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class CameraActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE_STORAGE = 3001;
    private static final int PERMISSION_CODE_CAMERA = 3002;

    CameraView cameraView;

    View shutterEffect;
    View captureButton;
    ImageView outline_img;
    View turnButton;
    TextView retakeText;
    ImageView retakeYes;
    ImageView retakeNo;

    Uri captured_image;
    Uri input_image;
    Uri foreground_image;
    Uri outline_image;
    Bitmap bmp;
    ProgressBar pBar;

    int capturedY = -1;
    int capturedX = -1;

    ArrayList<TextDisplay> textDisplays;

    private RenderScript rs;

    private boolean frameIsProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.camera_view);
        shutterEffect = findViewById(R.id.shutter_effect);
        captureButton = findViewById(R.id.shutter);
        outline_img = findViewById(R.id.img_outline);

        turnButton = findViewById(R.id.turn);
        retakeYes = findViewById(R.id.retake_yes);
        retakeNo = findViewById(R.id.retake_no);
        retakeText = findViewById(R.id.retake_text);
        pBar = findViewById(R.id.progress_bar);

        retakeText.setVisibility(View.GONE);
        retakeYes.setVisibility(View.GONE);
        retakeNo.setVisibility(View.GONE);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.takePicture();
            }
        });

        Bundle data = getIntent().getExtras();
        textDisplays = (ArrayList<TextDisplay>) getIntent().getSerializableExtra("inputs");
        capturedX = getIntent().getIntExtra("capturedX", -1);
        capturedY = getIntent().getIntExtra("capturedY", -1);

        // get image data
        if (data != null) {
            ArrayList<Uri> picked_images = (ArrayList<Uri>)((data.get("picked_images")));

            input_image = picked_images.get(1);
            foreground_image = picked_images.get(2);
            outline_image = picked_images.get(0);

            if (outline_image != null) {
                outline_img.setImageURI(outline_image);
            } else {
                outline_img.setImageBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.outline_img)).getBitmap());
            }

        }

        // switch camera - front and back
        turnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.switchCamera();
            }
        });

        rs = RenderScript.create(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PermissionUtils.isStorageGranted(this) && PermissionUtils.isCameraGranted(this)) {
            cameraView.start();
            setupCameraCallbacks();
        } else {
            if (!PermissionUtils.isCameraGranted(this)) {
                PermissionUtils.checkPermission(this, Manifest.permission.CAMERA,
                        PERMISSION_CODE_CAMERA);
            } else {
                PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        PERMISSION_CODE_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE_STORAGE:
            case PERMISSION_CODE_CAMERA:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
        if (requestCode != PERMISSION_CODE_STORAGE && requestCode != PERMISSION_CODE_CAMERA) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }

    private void setupCameraCallbacks() {
        cameraView.setOnPictureTakenListener(new CameraViewImpl.OnPictureTakenListener() {
            @Override
            public void onPictureTaken(Bitmap bitmap, int rotationDegrees) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pBar.setVisibility(View.VISIBLE);
                        outline_img.setImageURI(null);
                        outline_img.setImageBitmap(null);
                        cameraView.setVisibility(View.INVISIBLE);
                        turnButton.setVisibility(View.INVISIBLE);
                        captureButton.setVisibility(View.INVISIBLE);
                    }
                });
                startSavingPhoto(bitmap, rotationDegrees);
            }
        });
        cameraView.setOnFocusLockedListener(new CameraViewImpl.OnFocusLockedListener() {
            @Override
            public void onFocusLocked() {
                playShutterAnimation();
            }
        });
        cameraView.setOnTurnCameraFailListener(new CameraViewImpl.OnTurnCameraFailListener() {
            @Override
            public void onTurnCameraFail(Exception e) {
                Toast.makeText(CameraActivity.this, "Switch Camera Failed. Does you device has a front camera?",
                        Toast.LENGTH_SHORT).show();
            }
        });
        cameraView.setOnCameraErrorListener(new CameraViewImpl.OnCameraErrorListener() {
            @Override
            public void onCameraError(Exception e) {
                Toast.makeText(CameraActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        cameraView.setOnFrameListener(new CameraViewImpl.OnFrameListener() {
            @Override
            public void onFrame(final byte[] data, final int width, final int height, int rotationDegrees) {
                if (frameIsProcessing) return;
                frameIsProcessing = true;
                Observable.fromCallable(new Callable<Bitmap>() {
                    @Override
                    public Bitmap call() throws Exception {
                        return Nv21Image.nv21ToBitmap(rs, data, width, height);
                    }
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Bitmap>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(Bitmap frameBitmap) {
                                if (frameBitmap != null) {
                                    Log.i("onFrame", frameBitmap.getWidth() + ", " + frameBitmap.getHeight());
                                }
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {
                                frameIsProcessing = false;
                            }
                        });
            }
        });
    }

    private void playShutterAnimation() {
        shutterEffect.setVisibility(View.VISIBLE);
        shutterEffect.animate().alpha(0f).setDuration(300).setListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        shutterEffect.setVisibility(View.GONE);
                        shutterEffect.setAlpha(0.8f);
                    }
                });
    }

    private String bitmapToFile(Bitmap bitmap) {
        //create a file to write bitmap data
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileName = getString(R.string.app_name) + sdf.format(currentTime) + ".png";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), fileName);
        try {
            file.createNewFile();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
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
        saveBackgroundRemovedImage(file); // save captured image

        return file.getAbsolutePath();
    }

    private void saveBackgroundRemovedImage(File file) {
        try {
            HttpClient httpclient = HttpClientBuilder.create().build();
            HttpPost httppost = new HttpPost("https://api.remove.bg/v1.0/removebg");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addPart("image_file", new FileBody(file));
            builder.addTextBody("crop", "true");
            builder.addTextBody("format", "png");
            HttpEntity entity = builder.build();
            httppost.setEntity(entity);
            httppost.addHeader("X-Api-Key", "ZjpMjvGkCqfgAFK6jQeWCZFE");

            CloseableHttpResponse response = (CloseableHttpResponse) httpclient.execute(httppost);
            HttpEntity entityOut = response.getEntity();
            if (entityOut != null) {
                try (FileOutputStream out = new FileOutputStream(file)) {
                    entityOut.writeTo(out);

                    captured_image = Uri.fromFile(file);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideCameraAndShowRetakeWindow(boolean isHide) {
        if (isHide) {
            cameraView.setVisibility(View.INVISIBLE);
            turnButton.setVisibility(View.INVISIBLE);
            captureButton.setVisibility(View.INVISIBLE);
            retakeText.setVisibility(View.VISIBLE);
            retakeYes.setVisibility(View.VISIBLE);
            retakeNo.setVisibility(View.VISIBLE);
        } else {
            cameraView.setVisibility(View.VISIBLE);
            turnButton.setVisibility(View.VISIBLE);
            captureButton.setVisibility(View.VISIBLE);
            retakeText.setVisibility(View.GONE);
            retakeYes.setVisibility(View.GONE);
            retakeNo.setVisibility(View.GONE);
        }
    }

    private void startSavingPhoto(final Bitmap bitmap, final int rotationDegrees) {
        Observable.fromCallable(new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                Matrix matrix = new Matrix();
                matrix.postRotate(-rotationDegrees);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
        }).map(new Function<Bitmap, String>() {
            @Override
            public String apply(Bitmap bitmap) throws Exception {
                return bitmapToFile(bitmap);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String filePath) throws Exception {
                        if (filePath.isEmpty()) {
                            Toast.makeText(CameraActivity.this, "Save image file failed :(", Toast.LENGTH_SHORT).show();
                        } else {
                            hideCameraAndShowRetakeWindow(true);
                            bmp = BitmapFactory.decodeFile(filePath);
                            outline_img.setImageBitmap(bmp);
                            notifyGallery(filePath);
                            pBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
    }

    private void notifyGallery(String filePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(filePath);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    public void proceedProcess(View view) {
        Intent intent = new Intent(CameraActivity.this, ImageActivity.class);
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(input_image);
        uris.add(captured_image);
        uris.add(foreground_image);
        intent.putParcelableArrayListExtra("images_to_merge", uris);

        intent.putExtra("inputs", textDisplays);
        intent.putExtra("capturedX", capturedX);
        intent.putExtra("capturedY", capturedY);
        startActivity(intent);
    }

    public void discardProcess(View view) {
        hideCameraAndShowRetakeWindow(false);

        if (outline_image != null) {
            outline_img.setImageURI(outline_image);
        } else {
            outline_img.setImageBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.outline_img)).getBitmap());
        }
    }
}
