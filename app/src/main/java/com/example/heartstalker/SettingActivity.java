package com.example.heartstalker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener{
    BottomNavigationView bottomNavigationView;
    int age;
    EditText ageInput;
    int defaultAge = 35;

    Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        ageInput = findViewById(R.id.editTextAgeInput);
        submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ageInput.getText().toString().equals(""))
                    age = defaultAge;
                else
                    age = Integer.valueOf(ageInput.getText().toString());
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
}