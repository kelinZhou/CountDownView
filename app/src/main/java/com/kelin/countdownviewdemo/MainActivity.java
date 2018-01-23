package com.kelin.countdownviewdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.kelin.countdownview.CountDownView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CountDownView cdView = findViewById(R.id.cd_view);
        cdView.setOnFinishListener(new CountDownView.OnFinishListener() {
            @Override
            public void onFinish() {
                Toast.makeText(getApplicationContext(), "倒计时完毕！", Toast.LENGTH_SHORT).show();
            }
        });
        cdView.setDuration(10000);
        cdView.start();
    }
}
