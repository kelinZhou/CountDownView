package com.kelin.countdownviewdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.kelin.countdownview.CountDownView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CountDownView cdView = findViewById(R.id.cd_view);
        cdView.setDuration(5000);
        cdView.start();
    }
}
