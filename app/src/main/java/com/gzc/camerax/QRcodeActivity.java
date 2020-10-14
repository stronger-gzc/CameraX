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
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.graphics.ImageFormat;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class QRcodeActivity extends AppCompatActivity {
        private ImageAnalysis imageAnalysis;
    private Preview preview;
    private CameraSelector cameraSelector;

    private Executor executor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;

    private CameraInfo cameraInfo;
    private CameraControl cameraControl;

    private int aspectRatioInt = AspectRatio.RATIO_16_9;
    private int cameraSelectorInt = CameraSelector.LENS_FACING_BACK;

    private ProcessCameraProvider cameraProvider;

    private MultiFormatReader reader = new MultiFormatReader();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_q_rcode);
        previewView = (PreviewView) findViewById(R.id.preview_view);

        Map<DecodeHintType, Collection<BarcodeFormat>>map = new HashMap<>();
        List<BarcodeFormat>barcodeFormatList = new ArrayList<>();
        barcodeFormatList.add(BarcodeFormat.QR_CODE);
        map.put(DecodeHintType.POSSIBLE_FORMATS,barcodeFormatList);
        reader.setHints(map);

        initCamera();

    }

    /**
     * 构建图像分析用例(可用于二维码识别等用途)
     * 注意：Analyzer回掉方法中如果不调用image.close()将不会获取到下一张图片
     */
    private void initImageAnalysis() {
        imageAnalysis = new ImageAnalysis.Builder()
//                .setTargetResolution(size)//分辨率
                .setTargetAspectRatio(aspectRatioInt)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)// 仅将最新图像传送到分析仪，并在到达图像时将其丢弃。
                .build();

        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
//                int rotationDegress = image.getImageInfo().getRotationDegrees();
//                ImageProxy.PlaneProxy[] planes = image.getPlanes();
//                ByteBuffer buffer = planes[0].getBuffer();
                if(ImageFormat.YUV_420_888!=image.getFormat()){
                    Log.e("guanzhenchuang","expect YUV_420_888, now = "+image.getFormat());
                    return;
                }
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bbs = new byte[buffer.remaining()];
                int height = image.getHeight();
                int width = image.getWidth();
                buffer.get(bbs);
                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(bbs, width, height, 0, 0, width, height, false);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    Result result = reader.decode(bitmap);
                    Toast.makeText(QRcodeActivity.this, result.toString(), Toast.LENGTH_SHORT).show();
                } catch (NotFoundException e) {
                    e.printStackTrace();
                    image.close();
                }
            }
        });
    }

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
                    Camera camera = cameraProvider.bindToLifecycle(QRcodeActivity.this,cameraSelector,preview,imageAnalysis);
                    cameraInfo = camera.getCameraInfo();
                    cameraControl = camera.getCameraControl();

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
        initImageAnalysis();
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

}