package edu.ilstu.cartelematics;


import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraModeVideoFragment extends Fragment implements View.OnClickListener {

    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    private CameraDevice cameraDevice;
    private AutoFitTextureView textureView;
    private CaptureRequest.Builder previewRequestBuilder;
    private Handler backgroundHandler;
    private CameraCaptureSession captureSession;
    private CaptureRequest previewRequest;
    private String cameraID;
    private Size videoSize;
    private Size previewSize;
    private MediaRecorder mediaRecorder;
    private HandlerThread backgroundThread;
    private Button record;
    private Button stop;
    private boolean isRecording = false;
    private String videoPath;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay vDisplay;
    private Surface surface;
    private SurfaceView surfaceView;
    private DisplayMetrics metrics;

    private int test;


    public static CameraModeVideoFragment newInstance(){
        return new CameraModeVideoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_camera_mode, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState){
        textureView = (AutoFitTextureView) view.findViewById(R.id.textureview);
        record = (Button) view.findViewById(R.id.recordButton);
        stop = (Button) view.findViewById(R.id.stopButton);
        stop.setVisibility(view.GONE);
        surfaceView = (SurfaceView) view.findViewById(R.id.surface);
        surface = surfaceView.getHolder().getSurface();
        textureView.setOnTouchListener(touchListener);
        record.setOnClickListener(this);
        stop.setOnClickListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        projectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaRecorder = new MediaRecorder();
    }

    @Override
    public void onResume(){
        super.onResume();
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        if(textureView.isAvailable()){
            openCamera(textureView.getWidth(), textureView.getHeight());
        }else{
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause(){
        closeCamera();
        backgroundThread.quitSafely();
        try{
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        super.onPause();
    }

    //textureView is the loaded when the activity is started.
    //this listens for that and starts the camera.
    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private OnTouchListener touchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getActionMasked() == 1) {
                if(isRecording == false) {
                    if (record.getVisibility() == View.VISIBLE) {
                        record.setVisibility(v.GONE);
                    } else {
                        record.setVisibility(v.VISIBLE);
                    }
                }else{
                    if (stop.getVisibility() == View.VISIBLE) {
                        stop.setVisibility(v.GONE);
                    } else {
                        stop.setVisibility(v.VISIBLE);
                    }
                }
            }
            return true;
        }
    };

    private void configureTransform(int width, int height) {
        if(null == textureView || null ==  previewSize){
            return;
        }
        Activity activity = getActivity();
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix =  new Matrix();
        RectF viewRect = new RectF(0, 0, width, height);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        float scale = Math.max((float) height / previewSize.getHeight(), (float) width / previewSize.getWidth());
        matrix.postScale(scale, scale, centerX, centerY);
        matrix.postRotate(90 * (Surface.ROTATION_90 - 2), centerX, centerY);
        textureView.setTransform(matrix);
    }

    private void openCamera(int width, int height) {
        final Activity activity = getActivity();
        if(ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            System.out.println("Permissions are not working");
            getPermissions();
            return;
        }
        setUpCameraOutputs(width, height);
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try{
            cameraID = manager.getCameraIdList()[0];
            if(!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)){
                throw new RuntimeException("Time out waiting to lock camera opening");
            }
            configureTransform(width, height);
            manager.openCamera(cameraID, stateCallback, backgroundHandler);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }catch (InterruptedException e){
            throw new RuntimeException("Interrupted");
        }
    }

    private void getPermissions() {
        requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 1);
    }

    private void closeCamera() {
        try{
            cameraOpenCloseLock.acquire();
            if(captureSession != null){
                captureSession.close();
                captureSession = null;
            }
            if(cameraDevice != null){
                cameraDevice.close();
                cameraDevice = null;
            }
            /**if(mediaRecorder != null){
                mediaRecorder.release();
                mediaRecorder = null;
            }**/
        }catch(InterruptedException e){
            throw new RuntimeException("Interrupted while closing");
        }finally{
            cameraOpenCloseLock.release();
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        final Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try{
            for(String cameraID : manager.getCameraIdList()){
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if(map == null){
                    continue;
                }
                videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, videoSize);

                int orientation = getResources().getConfiguration().orientation;
                if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                }else{
                    textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                }
            }
        }catch(CameraAccessException e){
            e.printStackTrace();
        }catch(NullPointerException e){
            e.printStackTrace();
        }
    }

    private Size chooseOptimalSize(Size[] outputSizes, int width, int height, Size videoSize) {
        List<Size> bigEnough = new ArrayList<>();
        int w = videoSize.getWidth();
        int h = videoSize.getHeight();
        for(Size option : outputSizes){
            if(option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width && option.getHeight() >= height){
                bigEnough.add(option);
            }
        }

        if(bigEnough.size() > 0){
            return Collections.min(bigEnough, new CompareSizesByArea());
        }else{
            return outputSizes[0];
        }
    }

    private Size chooseVideoSize(Size[] outputSizes) {
        for (Size size : outputSizes){

            if(size.getWidth() == size.getHeight() * 16/9 && size.getWidth() <= 1080){
                return size;
            }
        }

        return outputSizes[outputSizes.length - 1];
    }

    //controls opening, closing, and if the camera has and error
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraOpenCloseLock.release();
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
            Activity activity = getActivity();
            activity.finish();
        }
    };

    private void createCameraPreviewSession() {
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());

            Surface surface = new Surface(texture);

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if(cameraDevice == null){
                        return;
                    }

                    captureSession = session;
                    try{
                        previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        HandlerThread thread = new HandlerThread("CameraPreview");
                        thread.start();
                        previewRequest = previewRequestBuilder.build();
                        captureSession.setRepeatingRequest(previewRequest, null, backgroundHandler);
                    }catch(CameraAccessException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, backgroundHandler);

        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void closePreviewSession() {
        if(captureSession != null){
            captureSession.close();
            captureSession = null;
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.recordButton){
            startRecording();
        }
        else if(v.getId() == R.id.stopButton){
            stopRecording();
        }
    }

    private void stopRecording() {
        record.setVisibility(View.VISIBLE);
        stop.setVisibility(View.GONE);
        isRecording = false;

        mediaRecorder.stop();
        mediaRecorder.reset();
        projectionCallback.onStop();
        vDisplay.release();

        videoPath = null;
        createCameraPreviewSession();
    }

    /**
    private void stopRecording() {
        record.setVisibility(View.VISIBLE);
        stop.setVisibility(View.GONE);
        isRecording = false;

        mediaRecorder.stop();
        mediaRecorder.reset();

        videoPath = null;
        createCameraPreviewSession();
    }
     **/

    private void startRecording(){
        Activity activity = getActivity();
        if(cameraDevice == null || !textureView.isAvailable() || previewSize == null){
            return;
        }
        metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        try {
            setUpMediaRecorder();
        } catch (IOException e) {
            e.printStackTrace();
        }
        test = 1;
        if(mediaProjection == null) {
            startActivityForResult(projectionManager.createScreenCaptureIntent(), 1);
        }
        else {
            vDisplay = mediaProjection.createVirtualDisplay("CameraMode", videoSize.getWidth(), videoSize.getHeight(), metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null);
            getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        record.setVisibility(View.GONE);
                        stop.setVisibility(View.VISIBLE);
                        isRecording = true;
                    }
                });
            }
        mediaRecorder.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode != 1){
            System.out.println("request code error");
            return;
        }
        if(resultCode != Activity.RESULT_OK){
            System.out.println("result code error");
            return;
        }

        /**mediaRecorder = new MediaRecorder();
        try {
            setUpMediaRecorder();
        } catch (IOException e) {
            e.printStackTrace();
        }**/

        Activity activity = getActivity();
        metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        mediaProjection.registerCallback(projectionCallback, null);
        vDisplay = mediaProjection.createVirtualDisplay("CameraMode", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                System.out.println(test);
                record.setVisibility(View.GONE);
                stop.setVisibility(View.VISIBLE);
                isRecording = true;
            }
        });
    }

    MediaProjection.Callback projectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            System.out.println("here first");
            super.onStop();
        }
    };

    /**
    private void startRecording() {
        if(cameraDevice == null || !textureView.isAvailable() || previewSize == null){
            return;
        }
        try{
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            previewRequestBuilder.addTarget(previewSurface);
            Surface recorderSurface = mediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            previewRequestBuilder.addTarget(recorderSurface);

            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback(){

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session){
                    captureSession = session;
                    if(cameraDevice == null){
                        return;
                    }
                    try{
                        previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        HandlerThread thread = new HandlerThread("CameraPreview");
                        thread.start();
                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                    }catch (CameraAccessException e){
                        e.printStackTrace();
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            record.setVisibility(View.GONE);
                            stop.setVisibility(View.VISIBLE);
                            isRecording = true;
                            mediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    System.out.println("Failed");
                }
            }, backgroundHandler);
        }catch(CameraAccessException | IOException e){
            e.printStackTrace();
        }
    }
    **/

    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if(activity == null){
            return;
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if(videoPath == null || videoPath.isEmpty()){
            final File dir = getActivity().getExternalFilesDir(null);
            videoPath = (dir == null ? "" : (dir.getAbsolutePath() + "/")) + System.currentTimeMillis() + ".mp4";
        }
        mediaRecorder.setOutputFile(videoPath);
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        System.out.println("Recording Video to: " +  videoPath);
        mediaRecorder.setOrientationHint(0);
        mediaRecorder.prepare();
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long) o1.getWidth() * o1.getHeight() - (long) o2.getWidth() * o2.getHeight());
        }
    }


}
