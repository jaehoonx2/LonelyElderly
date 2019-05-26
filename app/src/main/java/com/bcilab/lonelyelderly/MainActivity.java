package com.bcilab.lonelyelderly;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.text.IDNA;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_HRM = 101;
    public static final int REQUEST_CODE_INFO = 102;
    public static final int REQUEST_CODE_FALL = 103;
    public static Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 긴급연락처 세팅
        if(InfoActivity.phoneNumLoad() != "")
            uri = Uri.parse("tel:" + InfoActivity.phoneNumLoad());
        else
            uri = Uri.parse("tel:119");
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.e("LOG", "onPause()");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.e("LOG", "onDestroy()");
    }

    public void OnClick(View v){
        switch (v.getId()){
            case R.id.button_hrm : {
                Intent intent = new Intent(getApplicationContext(), HRMActivity.class);
                intent.putExtra("phoneNum", InfoActivity.phoneNumLoad());    // 연락처 정보 전달

                startActivityForResult(intent, REQUEST_CODE_HRM);
                break;
            }
            case R.id.button_info : {
                Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
                startActivityForResult(intent, REQUEST_CODE_INFO);
                break;
            }
            case R.id.button_fall : {
                Intent intent = new Intent(getApplicationContext(), FallActivity.class);
                startActivityForResult(intent, REQUEST_CODE_FALL);
                break;
            }
            case R.id.button_call : {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(uri);

                try {
                    Toast.makeText(getApplicationContext(), "긴급 연락 기능 작동! 보호자에게 전화를 발신합니다.", Toast.LENGTH_LONG).show();
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            }
            case R.id.button_exit : {
                moveTaskToBack(true);
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
            default :
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode){
            case REQUEST_CODE_HRM :
                break;
            case REQUEST_CODE_FALL :
                break;
            case REQUEST_CODE_INFO :
                uri = intent.getParcelableExtra("uri_phoneNum");
                break;
            default :
        }

        if(resultCode == RESULT_OK){
            // Normal response from Activities
            if(requestCode == REQUEST_CODE_HRM)
                Toast.makeText(getApplicationContext(), "return from HRMActivity", Toast.LENGTH_SHORT).show();
            else if (requestCode == REQUEST_CODE_INFO)
                Toast.makeText(getApplicationContext(), "return from InfoActivity", Toast.LENGTH_SHORT).show();
        }
    }
}