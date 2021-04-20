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

import static java.lang.Math.sqrt;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textView;
    private float[] colourData = new float[201];
    private long[] timeData = new long[200];
    private float averageRed;
    private int arrIndex = 0;
    private boolean first = true;
    private long time;
    private int check;
    private float aveColour;


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


                time = System.currentTimeMillis();
                averageRed = averageCalculator(bitmap);


                timeData[arrIndex] = time;
                colourData[arrIndex] = averageRed;
                if(arrIndex < 199){
                    arrIndex++;
                    check = 0;
                }else{

                    //Use Normalized Data
                    colourData = NormalizeData(colourData);

                    //get average colour rating
                    aveColour = 0;
                    for(int i = 0; i < 200; i++){

                        aveColour += colourData[i];
                    }

                    //Save average Colour
                    aveColour = aveColour / 200;

                    if(first == true){
                        //Save as the first instance of average
                        colourData[200] = aveColour;
                        first = false;
                        System.out.println("aveColour1 = " + aveColour);

                    }else{
                        //Use old average and create a new average
                        colourData[200] = (aveColour + colourData[200]) / 2;
                        System.out.println("aveColour2 = " + aveColour);

                    }

                    arrIndex = 0;
                    check = 1;
                }


                if(check == 1){
                    System.out.println("Enter Calculations");
                    float heartRate = CalculateHeartRate(timeData, colourData);

                    //uncomment once it is ready to use

                    textView.setText(Float.toString(heartRate));
                    check = 0;
                }


                //textView.setText(Float.toString(averageCalculator(bitmap)));

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
    


    //method 1, uses uptrend and downtrend
    /*
    private float CalculateHeartRate(long[] timeArr, float[] colourArr){
        float heartRate;
        int[] peakArr = new int[200];
        int peakArrIndex = 0;
        float valueTrack;
        long time = 0;
        boolean upTrend = false;
        int upTrendCounter = 0;

        //Finds intervals
        valueTrack = colourArr[0];
        for(int i = 1; i < 200; i++){
            if(colourArr[i] > valueTrack && upTrend == false){
                //reached an upward trend
                upTrend = true;
                valueTrack = colourArr[i];
                upTrendCounter = 1;
            }else if(colourArr[i] < valueTrack && upTrend == true && upTrendCounter < 10){
                //peak reached
                upTrend = false;
                peakArr[peakArrIndex] = i - 1;
                peakArrIndex++;
                valueTrack = colourArr[i];
                upTrendCounter = 0;
            }else if (colourArr[i] > valueTrack && upTrend == true){
                upTrendCounter++;
                valueTrack = colourArr[i];
            }else{
                //Either upTrend == False and colour < valueTrack
                //or upTrend == True and colour > valueTrack
                //No need to change other than value Track
                valueTrack = colourArr[i];
            }
        }

        //System.out.println("check 0 " + peakArrIndex + " " + time);

        if(peakArrIndex < 2){
            return 0;
        }
        //Get the average time between intervals
        for(int i = 0; i < peakArrIndex; i ++){
            time = time + (timeArr[i + 1] - timeArr[i]);
        }

        System.out.println("check 1 " + peakArrIndex + " " + time);

        time = time/(peakArrIndex - 1);

        //bt/ms * ms/s * s/min = bt/min then inverse

        System.out.println("check 2 " + peakArrIndex + " " + time);

        float hold = time;
        heartRate = 60000/hold;

        System.out.println("check 3 " + peakArrIndex + " " + time + " " + hold);

        return heartRate;
    }*/

    //Method 2 Get average value for heartrate measurements, then use that average to find ups and downs
    /*
    private float CalculateHeartRate(long[] timeArr, float[] colourArr){
        float averageColour = 0;
        boolean beating = false;
        float heartRate = 0;

        //get average colour rating
        for(int i = 0; i < 300; i++){
            averageColour += colourArr[i];
        }
         averageColour = averageColour / 300;

        //Use average to find
        for(int i = 0; i < 300; i++){
            if(averageColour > colourArr[i] && beating == false){
                beating = true;
                heartRate++;
            }else if(averageColour < colourArr[i] && beating == true){
                beating = false;
            }
        }

        //Get the time interval (in ms)
        long timeTotal = timeArr[299] - timeArr[0];
        System.out.println("time total " + timeTotal + "  HeartRate " + heartRate);

        //in beats/ms
        heartRate = heartRate/timeTotal;
        System.out.println(heartRate);

        //conversion to min
        heartRate = heartRate * 60 * 1000;

        System.out.println(heartRate);

        return heartRate;
    }
  */

    //Method 3: Mix of 1 and 2
    private float CalculateHeartRate(long[] timeArr, float[] colourArr){
        float averageColour = colourArr[200];
        boolean beating = false;
        float heartRate = 0;
        long[] timeTrack = new long[30];
        int loopStart = 0;
        int prevPeak = -1;
        int currentPeak = -1;
        float peakValue = 0;
        long timeTotal = 0;

        //debug
        for(int i = 0; i < 100; i++){
            System.out.println("colourArr val = " + colourArr[i]);
        }

        //Use average to find heartbeats
        //find the peak in the sample and use it to discover the peak of the beat
        for(int i = 0; i < 200; i++){
            //value > average, and was not beating
            if(averageColour < colourArr[i] && beating == false){
                beating = true;
                loopStart = i;
                heartRate++;

            //value < average and it was beating
            }else if(averageColour > colourArr[i] && beating == true){
                beating = false;

                //Find Peak and remember
                for (int j = loopStart; j <= i; j++){
                    if(peakValue < colourArr[j]){
                        peakValue = colourArr[j];
                        currentPeak = j;
                    }
                }

                if(prevPeak != -1){
                    timeTrack[(int) heartRate] = timeArr[currentPeak] - timeArr[prevPeak];
                }

                prevPeak = currentPeak;
                peakValue = 0;
            }
        }

        //Get the time interval (in ms)
        //check if the recording ended mid beat
        if(beating == true){
            heartRate = heartRate - 1;
        }
        if(averageColour > colourArr[0]){

            for(int i = 1; i < heartRate; i++){
                timeTotal = timeTotal + timeTrack[i];
            }
            heartRate = heartRate - 1;

        }else{

            for(int i = 0; i < heartRate; i++){
                timeTotal = timeTotal + timeTrack[i];
            }
        }


        System.out.println("time total " + timeTotal + "  HeartRate " + heartRate);

        //in beats/ms
        heartRate = heartRate/timeTotal;

        System.out.println("heartRate" + heartRate);

        //conversion to min
        heartRate = heartRate * 60 * 1000;

        System.out.println("heartRate" + heartRate);

        //Irregular Value, should not be returned
        /*
        if(heartRate > 130 || heartRate < 40){
            return 666;
        }*/

        return heartRate;
    }


    private float[] NormalizeData(float[] colourArr){

        //Step 1: normalize the data Given
        float colourMean = 0;
        float[] colourStdCalc = new float[201];
        float[] normColourArr = new float[201];
        float colourStd;

        //Find Standard Deviation of colour
        for(int i = 0; i < 200; i++){
            colourMean =+ colourArr[i];
        }
        colourMean = colourMean/200;

        for(int i = 0; i < 200; i++){
            colourStdCalc[i] = (colourArr[i] - colourMean) * (colourArr[i] - colourMean);
        }
        float colourMean2 = 0;
        for(int i = 0; i < 200; i++){
            colourMean2 =+ colourStdCalc[i];
        }
        colourMean2 = colourMean2/200;

        colourStd = (float) sqrt((double) colourMean2);

        for(int i = 0; i < 200; i++){
            normColourArr[i] = (colourArr[i] - colourMean)/colourStd;
        }
        normColourArr[200] = colourArr[200];
        return normColourArr;
    }

}
