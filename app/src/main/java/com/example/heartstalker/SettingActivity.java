package com.example.heartstalker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener{
    BottomNavigationView bottomNavigationView;
    int age;
    String weight;
    EditText ageInput;
    EditText weightInput;
    int defaultAge = 35;
    int genderBool=0;
    String defaultWeight = "60";
    private Spinner spnGen;

    Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        ageInput = findViewById(R.id.editTimeInput);
        weightInput = findViewById(R.id.editWeightInput);
        spnGen = findViewById(R.id.editGender);

        List<String> gender = new ArrayList<>();
        gender.add("Male");
        gender.add("Female");
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, R.layout.spinner_color,gender);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnGen.setAdapter(genderAdapter);

        spnGen.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String genderStr = spnGen.getSelectedItem().toString();
                if(genderStr.equals("Male")){
                    genderBool=0;
                }else{
                    genderBool=1;
                }

                saveGender(genderBool);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        submitButton = findViewById(R.id.submitButtonExercise);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ageInput.getText().toString().equals("")){
                    age = defaultAge;
                } else{
                    age = Integer.valueOf(ageInput.getText().toString());
                    float maxHR = (float) (206.9-(0.67*age));
                    saveAge(age);
                    saveMaxHR(maxHR);
                }

                if(weightInput.getText().toString().equals("")){
                    weight = defaultWeight;
                } else{
                    weight = weightInput.getText().toString();

                    saveWeight(weight);

                }
            }
        });
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.ic_home:
                Intent intent = new Intent(this, MainActivity.class);
                if(age == 0){
                    age = defaultAge;
                }
                intent.putExtra("Age",age);
                startActivity(intent);
                finish();
            case R.id.ic_settings:
                return true;
        }
        return false;
    }

    public void saveAge(int age) {
        String ageStr = String.format("%d", age);
        try {
            FileOutputStream fileOutputStream = openFileOutput("Age.txt", MODE_PRIVATE);
            fileOutputStream.write(ageStr.getBytes());
            System.out.println("saving: "+ ageStr);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void saveWeight(String  weightStr) {
        try {
            FileOutputStream fileOutputStream = openFileOutput("Weight.txt", MODE_PRIVATE);
            fileOutputStream.write(weightStr.getBytes());
            System.out.println("saving: "+ weightStr);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public void saveMaxHR(float maxHR) {
        String maxHRStr = String.format("%.0f", maxHR);
        try {
            FileOutputStream fileOutputStream = openFileOutput("maxHR.txt", MODE_PRIVATE);
            fileOutputStream.write(maxHRStr.getBytes());
            System.out.println("saving: "+ maxHRStr);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void saveGender(int gender) {
        String genderStr = String.format("%d", gender);
        try {
            FileOutputStream fileOutputStream = openFileOutput("Gender.txt", MODE_PRIVATE);
            fileOutputStream.write(genderStr.getBytes());
            System.out.println("saving: "+ genderStr);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}