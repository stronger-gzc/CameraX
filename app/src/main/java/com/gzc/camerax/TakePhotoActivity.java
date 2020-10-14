package com.gzc.camerax;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class TakePhotoActivity extends AppCompatActivity {
    private ImageView imageView;

    private ImageCapture imageCapture;
//    private ImageAnalysis imageAnalysis;
    private Preview preview;
    private CameraSelector cameraSelector;


    private Executor executor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private View takePhotoView;

    private CameraInfo cameraInfo;
    private CameraControl cameraControl;

    private int aspectRatioInt = AspectRatio.RATIO_16_9;
    private int cameraSelectorInt = CameraSelector.LENS_FACING_BACK;
    private Size size = new Size(1280,720);

    private ProcessCameraProvider cameraProvider;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        imageView = (ImageView) findViewById(R.id.image_view);
        previewView = (PreviewView) findViewById(R.id.preview_view);
        takePhotoView = (View) findViewById(R.id.take_photo_view);
        takePhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    saveImage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setVisibility(View.GONE);
            }
        });
        initCamera();
    }

    /**
     * 构建图像捕获用例
     */
    private void initImageCapture() {
        imageCapture = new ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .setTargetAspectRatio(aspectRatioInt)
//                .setTargetResolution(size)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation;
                if (orientation >= 45 && orientation < 135) {
                    rotation = Surface.ROTATION_270;
                } else if (orientation >= 135 && orientation < 225) {
                    rotation = Surface.ROTATION_180;
                } else if (orientation >= 225 && orientation < 315) {
                    rotation = Surface.ROTATION_90;
                } else {
                    rotation = Surface.ROTATION_0;
                }
                imageCapture.setTargetRotation(rotation);
            }
        };

        orientationEventListener.enable();
    }

    /**
     * 构建图像分析用例(可用于二维码识别等用途)
     * 注意：Analyzer回掉方法中如果不调用image.close()将不会获取到下一张图片
     */
//    private void initImageAnalysis() {
//        imageAnalysis = new ImageAnalysis.Builder()
////                .setTargetResolution(size)//分辨率
//                .setTargetAspectRatio(aspectRatioInt)
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)// 仅将最新图像传送到分析仪，并在到达图像时将其丢弃。
//                .build();
//
//        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
//            @Override
//            public void analyze(@NonNull ImageProxy image) {
//                int rotationDegress = image.getImageInfo().getRotationDegrees();
//                ImageProxy.PlaneProxy[] planes = image.getPlanes();
//                ByteBuffer buffer = planes[0].getBuffer();
//            }
//        });
//    }

    /**
     * 初始化相机
     */
    private void initCamera(){
        executor = ContextCompat.getMainExecutor(this);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        initUseCases();
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {

                    cameraProvider = cameraProviderFuture.get();
                    cameraProvider.unbindAll();
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());
                    Camera camera = cameraProvider.bindToLifecycle(TakePhotoActivity.this,cameraSelector,preview,imageCapture);
                    cameraInfo = camera.getCameraInfo();
                    cameraControl = camera.getCameraControl();

                    initCameraListener();

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        },executor);
    }

    /**
     * 初始化配置信息
     */
    private void initUseCases(){
//        initImageAnalysis();
        initImageCapture();
        initPreview();
        initCameraSelector();
    }

    /**
     * 构建图像预览
     */
    private void initPreview(){
        preview = new Preview
                .Builder()
                .setTargetAspectRatio(aspectRatioInt)
//                .setTargetResolution(size)
                .build();
    }

    /**
     * 选择摄像头
     */
    private void initCameraSelector(){
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraSelectorInt)
                .build();
    }

    private void saveImage() throws IOException {
//        File file = new File(getExternalMediaDirs()[0],System.currentTimeMillis()+".jpg");
        File file  = File.createTempFile("com.gzc.camerax-",".jpg",getExternalMediaDirs()[0]);

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture
                .OutputFileOptions
                .Builder(file)
                .build();
        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                imageView.setVisibility(View.VISIBLE);
                Glide.with(TakePhotoActivity.this)
                        .load(file)
                        .into(imageView);
//                Log.e("guanzhenchuang","图片保存成功："+file.getAbsolutePath());
//                Uri contentUri = Uri.fromFile(new File(file.getAbsolutePath()));
//                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,contentUri);
//                sendBroadcast(mediaScanIntent);
//                savedUri = outputFileResults.getSavedUri();
//                Log.e("guanzhenchuang","uri:"+savedUri.toString());
//                new Image
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
            }
        });
    }

    private void initCameraListener() {

    }
}