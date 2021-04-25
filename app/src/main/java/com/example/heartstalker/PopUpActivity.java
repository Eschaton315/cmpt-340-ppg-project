package com.example.heartstalker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class PopUpActivity extends AppCompatActivity {

    private TextView textView;
    private TextView textView2;
    private TextView textView3;
    private Button submit;
    private Button backBtn;
    private EditText timeIn;
    private int gender =0;
    private String genderStr;
    private float  time=0;
    private float heartRate;
    private float MaxHR=200;
    private float age;
    private float weight;
    private String mhrStr;
    private String weightStr;
    private String intensity;
    private String calorie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up);

        textView = findViewById(R.id.intensity);
        textView2 = findViewById(R.id.calories);
        textView3 = findViewById(R.id.HRDisplay);
        timeIn = findViewById(R.id.editTimeInput);
        backBtn =findViewById(R.id.back);
        submit = findViewById(R.id.submitButtonExercise);
        heartRate = Float.parseFloat(getHeartRateVal());
        textView3.setText(String.format("%.0f", heartRate)+" Bpm");
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timeIn.getText().toString().equals("")) {
                    time = 0;
                    textView.setText("No value inputted");
                } else{
                    time = Float.parseFloat(timeIn.getText().toString());
                    mhrStr = getMaxHRVal();
                    weightStr = getWeight();
                    genderStr = getGender();

                    if(!genderStr.equals("")){
                        gender = Integer.parseInt(genderStr);
                    }

                    if(mhrStr.equals("")||weightStr.equals("")){
                        textView.setText("Set age or weight in \"settings\" first");
                    }else{
                        MaxHR = Float.parseFloat(mhrStr);
                        age = Float.parseFloat(getAge());
                        weight = Float.parseFloat(weightStr);



                        intensity = CalculateExerciseIntensity(heartRate,MaxHR);
                        calorie = CaloriesBurn(heartRate,weight,time,age,gender);

                        textView.setText(intensity);
                        textView2.setText("Calories Burned: " + calorie);


                    }



                }


            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PopUpActivity.this, MainActivity.class));
            }

        });

    }

    public String getHeartRateVal(){
        String HRval;
        try{
            FileInputStream fileInputStream = openFileInput("Heart Rate.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();

            while((HRval = bufferedReader.readLine())!=null){
                stringBuffer.append(HRval);
            }
            return (stringBuffer.toString());
        }catch(FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        return (null);
    }

    public String getMaxHRVal(){
        String HRval;
        try{
            FileInputStream fileInputStream = openFileInput("maxHR.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();

            while((HRval = bufferedReader.readLine())!=null){
                stringBuffer.append(HRval);
            }
            return (stringBuffer.toString());
        }catch(FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        return (null);
    }

    public String getAge(){
        String Ageval;
        try{
            FileInputStream fileInputStream = openFileInput("Age.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();

            while((Ageval = bufferedReader.readLine())!=null){
                stringBuffer.append(Ageval);
            }
            return (stringBuffer.toString());
        }catch(FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        return (null);
    }

    public String getWeight(){
        String weightVal;
        try{
            FileInputStream fileInputStream = openFileInput("Weight.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();

            while((weightVal = bufferedReader.readLine())!=null){
                stringBuffer.append(weightVal);
            }
            return (stringBuffer.toString());
        }catch(FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        return (null);
    }


    public String getGender(){
        String genderVal;
        try{
            FileInputStream fileInputStream = openFileInput("Gender.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();

            while((genderVal = bufferedReader.readLine())!=null){
                stringBuffer.append(genderVal);
            }
            return (stringBuffer.toString());
        }catch(FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        return (null);
    }


    private String CalculateExerciseIntensity(float heartRate,float maxHR){

        float intensityPercent= 100*(heartRate/maxHR);
        String intensityPercentStr = String.format("Intensity\n%.0f",intensityPercent);
        intensityPercentStr = intensityPercentStr.concat("%");

        return intensityPercentStr;
    }


    private String CaloriesBurn(float heartRate,float weight,float time,float age,int gender){
        //requires gender,weight,and exercise duration
        //weight in kg
        //time in hours
        //genders will just be 0=male/1=female
        //if bpm is too low, it will just say 0

        float calBurn=-1;
        String calBurnStr;

        if(gender==0){
            calBurn = (float) (((-55.0969+(0.6309*heartRate)+(0.1988*weight)+(0.2017*age))/4.184)*60*time);
        }
        if(gender==1) {
            calBurn= (float) (((-20.4022+(0.4472*heartRate)-(0.1263*weight)+(0.074*age))/4.184)*60*time);
        }

        if(calBurn<=-1){
            return "0";
        }

        calBurnStr=String.format("%.0f",calBurn);

        return calBurnStr;

    }
}