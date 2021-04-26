package com.example.heartstalker;

import android.content.Context;
import android.content.Intent;
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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.sql.SQLOutput;
import java.util.concurrent.ExecutionException;

import static java.lang.Math.sqrt;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textView;
    private ImageView progressBar;
    private Button exerciseButton;
    private float[] colourData = new float[201];
    private long[] timeData = new long[200];
    private float averageRed;
    private int arrIndex = 0;
    private boolean first = true;
    private long time;
    private int check;
    private float aveColour;
    private float maxHR=200;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);


        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        textView = findViewById(R.id.heartRate);
        progressBar = findViewById(R.id.progressBar);
        exerciseButton = findViewById(R.id.exercise);
        exerciseButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));


        exerciseButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent exerciseIntent = new Intent(CameraActivity.this, PopUpActivity.class);
                startActivity(exerciseIntent);
            }
        });



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
                //slots 0-199 are used for data,
                //slot 200 is to store average over every single run of the algorithm

                if(arrIndex < 199){
                    arrIndex++;
                    if(arrIndex==150){
                        progressBar.setVisibility(View.VISIBLE);
                    }
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
                    //calculate once all slots are filled
                    System.out.println("Enter Calculations");
                    float heartRate = CalculateHeartRate(timeData, colourData);


                    //get max HR
                    //Any heart rate that are considered irregular will be omitted i.e. anything over max heart rate or 40bpm. maxHR default is 200.
                    if(heartRate<=maxHR&&heartRate>=40) {
                        String heartRate_Str = String.format("%.0f", heartRate);
                        heartRate_Str = heartRate_Str.concat("\nBpm");
                        saveHeartRate(heartRate);
                        //save heart rate value for other activities
                       progressBar.setVisibility(View.GONE);
                        textView.setText(heartRate_Str);

                        //make button visible
                        if(exerciseButton.getVisibility()==View.GONE){
                            exerciseButton.setVisibility(View.VISIBLE);
                        }


                    }

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

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private float averageCalculator(Bitmap bitmap) {
        float sum = 0;
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                sum += bitmap.getColor(i,j).red();
            }
        }
        return sum / (50 * 50);
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


    //Calculate Heart Rate
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


        //in beats/ms
        heartRate = heartRate/timeTotal;

        //conversion to min
        heartRate = heartRate * 60 * 1000;

        return heartRate;
    }


    private float[] NormalizeData(float[] colourArr){


        float colourMean = 0;
        float[] colourStdCalc = new float[201];
        float[] normColourArr = new float[201];
        float colourStd;

        //Step 1: Get Mean for the data given
        for(int i = 0; i < 200; i++){
            colourMean =+ colourArr[i];
        }
        colourMean = colourMean/200;

        //Step 2: Find Standard Deviation of colour
        for(int i = 0; i < 200; i++){
            colourStdCalc[i] = (colourArr[i] - colourMean) * (colourArr[i] - colourMean);
        }
        float colourMean2 = 0;
        for(int i = 0; i < 200; i++){
            colourMean2 =+ colourStdCalc[i];
        }
        colourMean2 = colourMean2/200;

        colourStd = (float) sqrt((double) colourMean2);

        //Step 3: Normalize every value in the array
        for(int i = 0; i < 200; i++){
            normColourArr[i] = (colourArr[i] - colourMean)/colourStd;
        }
        normColourArr[200] = colourArr[200];
        return normColourArr;
    }



    //save heart rate to a internal txt file so it can be pulled from other activities
    public void saveHeartRate(float heartRate) {
        String heartRateStr = String.format("%.2f", heartRate);
        try {
            FileOutputStream fileOutputStream = openFileOutput("Heart Rate.txt", MODE_PRIVATE);
            fileOutputStream.write(heartRateStr.getBytes());
            System.out.println("saving: "+heartRateStr);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    }



