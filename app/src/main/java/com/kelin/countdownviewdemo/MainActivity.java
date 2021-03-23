package com.kelin.countdownviewdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.kelin.countdownview.CountDownView;

public class MainActivity extends AppCompatActivity {

    private CountDownView cdView1;
    private CountDownView cdView2;
    private CountDownView cdView3;
    private CountDownView cdView4;
    private CountDownView cdView5;
    private CountDownView cdView6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cdView1 = findViewById(R.id.cd_view1);
//        cdView.setOnFinishListener(new CountDownView.OnFinishListener() {
//            @Override
//            public void onFinish() {
//                Toast.makeText(getApplicationContext(), "倒计时完毕！", Toast.LENGTH_SHORT).show();
//            }
//        });
        cdView1.start();
        cdView2 = findViewById(R.id.cd_view2);
        cdView2.start();
        cdView3 = findViewById(R.id.cd_view3);
        cdView3.start();
        cdView4 = findViewById(R.id.cd_view4);
        cdView4.start();
        cdView5 = findViewById(R.id.cd_view5);
        cdView5.start();
        cdView6 = findViewById(R.id.cd_view6);
        cdView6.start();
    }

    public void onStartCountDown(View view) {
        if (!cdView1.isStarted()) {
            cdView1.start();
        }
        if (!cdView2.isStarted()) {
            cdView2.start();
        }
        if (!cdView3.isStarted()) {
            cdView3.start();
        }
        if (!cdView4.isStarted()) {
            cdView4.start();
        }
        if (!cdView5.isStarted()) {
            cdView5.start();
        }
        if (!cdView6.isStarted()) {
            cdView6.start();
        }
    }
}
