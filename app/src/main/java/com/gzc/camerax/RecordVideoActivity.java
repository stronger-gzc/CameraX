package com.gzc.camerax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

@SuppressLint("RestrictedApi")
public class RecordVideoActivity extends AppCompatActivity {
    private static final String TAG = "RecordVideoActivity";
    private Button recordView;
    private PreviewView previewView;
    private boolean isRecord = false;

    private VideoCapture videoCapture;
    private Preview preview;
    private CameraSelector cameraSelector;


    private Executor executor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private CameraInfo cameraInfo;
    private CameraControl cameraControl;

    private int aspectRatioInt = AspectRatio.RATIO_16_9;
    private int cameraSelectorInt = CameraSelector.LENS_FACING_BACK;

    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_video);

        recordView = (Button) findViewById(R.id.record_view);
        previewView = (PreviewView) findViewById(R.id.preview_view);

        recordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecord){
                    recordView.setText("开始");
                    isRecord = false;
                    videoCapture.stopRecording();
                }else{
                    recordView.setText("暂停");
                    isRecord = true;
                    try {
                        record();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        initCamera();

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
                    //如果不需要预览的话，去掉preview即可
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());
                    Camera camera = cameraProvider.bindToLifecycle(RecordVideoActivity.this,cameraSelector,preview,videoCapture);
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
        initVideoCapture();
        initPreview();
        initCameraSelector();
    }

    @SuppressLint("RestrictedApi")
    private void initVideoCapture(){
        videoCapture = new VideoCapture
                .Builder()
                .setTargetAspectRatio(aspectRatioInt)
//                .setTargetRotation(previewView.getDisplay().getRotation())
                .build();
    }

    /**
     * 构建图像预览
     */
    private void initPreview(){
        preview = new Preview
                .Builder()
                .setTargetAspectRatio(aspectRatioInt)
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

    @SuppressLint("RestrictedApi")
    private void record() throws IOException {
        File file  = File.createTempFile("com.gzc.camerax-",".mp4",getExternalMediaDirs()[0]);
        VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture
                .OutputFileOptions
                .Builder(file)
                .build();
        videoCapture.startRecording(outputFileOptions, executor, new VideoCapture.OnVideoSavedCallback() {
            @Override
            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                Toast.makeText(RecordVideoActivity.this, "成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                Log.e(TAG,"失败："+message+"   "+cause.getMessage());
            }
        });
    }
}