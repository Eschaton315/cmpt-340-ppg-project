package com.example.heartstalker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Size;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.heartstalker.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textView;
    private float[] colourData = new float[100];
    private long[] timeData = new long[100];
    private float highRed;
    private float lowRed;
    private int arrIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        textView = findViewById(R.id.heartRate);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        // set up Image Analysis
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(50, 50))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        // look at preview frames
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void analyze(@NonNull ImageProxy image) {
                // Get the YUV data
                ByteBuffer yuvBytes = imageToByteBuffer(image);

                // Convert YUV to RGB
                RenderScript rs = RenderScript.create(CameraActivity.this);
                Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
                Allocation allocationRgb = Allocation.createFromBitmap(rs, bitmap);

                Allocation allocationYuv = Allocation.createSized(rs, Element.U8(rs), yuvBytes.array().length);
                allocationYuv.copyFrom(yuvBytes.array());

                ScriptIntrinsicYuvToRGB scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
                scriptYuvToRgb.setInput(allocationYuv);
                scriptYuvToRgb.forEach(allocationRgb);

                allocationRgb.copyTo(bitmap);

                //pass time and arrayValue to function to fill array
                int check = AddValueToTracker(System.currentTimeMillis(), averageCalculator(bitmap), timeData, colourData, arrIndex);
                float heartRate = CalculateHeartRate(timeData, colourData);

                //uncomment once it is ready to use
                //textView.setText(CalculateHeartRate(timeData, colourData));

                textView.setText(Float.toString(averageCalculator(bitmap)));

                // Release
                bitmap.recycle();
                allocationYuv.destroy();
                allocationRgb.destroy();
                rs.destroy();
                image.close();

            }
        });

        // update number
        // might have to move this into analyze() so that it'll update
        // textView.setText("heart rate");

        // show preview in previewView
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());

        Camera cam = cameraProvider.bindToLifecycle((LifecycleOwner) this,
                cameraSelector, imageAnalysis, preview);

        // turn on flashlight
        if (cam.getCameraInfo().hasFlashUnit()) {
            cam.getCameraControl().enableTorch(true); // or false
        }
    }

    // this is inefficient?? and probably is subject to subject to cancellation error
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private float averageCalculator(Bitmap bitmap) {
        float sum = 0;
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                sum += bitmap.getColor(i,j).red();
            }
        }
        return sum / (bitmap.getHeight() * bitmap.getWidth());
    }

    private ByteBuffer imageToByteBuffer(final ImageProxy image) {
        final Rect crop = image.getCropRect();
        final int width = crop.width();
        final int height = crop.height();

        final ImageProxy.PlaneProxy[] planes = image.getPlanes();
        final byte[] rowData = new byte[planes[0].getRowStride()];
        final int bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
        final ByteBuffer output = ByteBuffer.allocateDirect(bufferSize);

        int channelOffset = 0;
        int outputStride = 0;

        for (int planeIndex = 0; planeIndex < 3; planeIndex++) {
            if (planeIndex == 0) {
                channelOffset = 0;
                outputStride = 1;
            } else if (planeIndex == 1) {
                channelOffset = width * height + 1;
                outputStride = 2;
            } else if (planeIndex == 2) {
                channelOffset = width * height;
                outputStride = 2;
            }

            final ByteBuffer buffer = planes[planeIndex].getBuffer();
            final int rowStride = planes[planeIndex].getRowStride();
            final int pixelStride = planes[planeIndex].getPixelStride();

            final int shift = (planeIndex == 0) ? 0 : 1;
            final int widthShifted = width >> shift;
            final int heightShifted = height >> shift;

            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));

            for (int row = 0; row < heightShifted; row++) {
                final int length;

                if (pixelStride == 1 && outputStride == 1) {
                    length = widthShifted;
                    buffer.get(output.array(), channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (widthShifted - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);

                    for (int col = 0; col < widthShifted; col++) {
                        output.array()[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }

                if (row < heightShifted - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }

        return output;
    }

    //function for recording averages in an array
    //Take the high and low
    //Normalize
    //investigate the amount of time required to do it
    //https://github.com/phishman3579/android-heart-rate-monitor
    
    private int AddValueToTracker(long time, float averageRed, long[] timeArr, float[] colourArr, int index){
        timeArr[index] = time;
        colourArr[index] = averageRed;
        if(index < 99){
            index++;
            return 0;
        }else{
            index = 0;
            return 1;
        }
    }


    private float CalculateHeartRate(long[] timeArr, float[] colourArr){
        long heartRate;
        int[] peakArr = new int[100];
        int peakArrIndex = 0;
        float valueTrack = 0;
        long time = 0;
        boolean upTrend = false;

        //Finds intervals
        for(int i = 0; i < 100; i++){
            if(colourArr[i] > valueTrack && upTrend == false){
                //reached an upward trend
                upTrend = true;
                valueTrack = colourArr[i];
            }else if(colourArr[i] < valueTrack && upTrend == true){
                //peak reached
                upTrend = false;
                peakArr[peakArrIndex] = i;
                peakArrIndex++;
                valueTrack = colourArr[i];
            }else{
                //Either upTrend == False and colour < valueTrack
                //or upTrend == True and colour > valueTrack
                //No need to change other than value Track
                valueTrack = colourArr[i];
            }
        }

        //Get the average time between intervals
        for(int i = 0; i < peakArrIndex - 1; i ++){
            time = time + timeArr[i + 1] - timeArr[i];
        }
        time = time/(peakArrIndex - 2);

        //1 over milliseconds per beat * 1000 * 60
        heartRate = 1/(time * 1000 * 60);

        return heartRate;
    }
  
}
