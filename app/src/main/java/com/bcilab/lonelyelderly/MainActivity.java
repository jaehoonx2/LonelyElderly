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

    //Using the Accelometer & Gyroscoper
    private SensorManager mSensorManager = null;
    //Using the Accelometer
    private SensorEventListener mAccLis;
    private Sensor accel_data = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Using the Gyroscope & Accelometer
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //Using the Accelometer
        accel_data = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccLis = new AccelometerListener();

        findViewById(R.id.button_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSensorManager.registerListener(mAccLis, accel_data, SensorManager.SENSOR_DELAY_UI);
            }
        });

        findViewById(R.id.button_disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSensorManager.unregisterListener(mAccLis);
            }
        });

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
        //mSensorManager.unregisterListener(mAccLis);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.e("LOG", "onDestroy()");
        mSensorManager.unregisterListener(mAccLis);
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

    private class AccelometerListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            double accX = event.values[0];
            double accY = event.values[1];
            double accZ = event.values[2];

            String a =String.format("%f" , accX);
            String b =String.format("%f" , accY);
            String c =String.format("%f" , accZ);

            //Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            SimpleDateFormat time1 = new SimpleDateFormat("HH:mm:ss.SSS"); //timestamp
            String time_data1 = time1.format(new Date()); //timestamp

            //String message1 = a + " " + b + " " + c + " " + timestamp.getTime();
            String message1 = a + " " + b + " " + c + " " + time_data1;

            String state = Environment.getExternalStorageState(); //외부저장소(SDcard)의 상태 얻어오기
            File path;    //저장 데이터가 존재하는 디렉토리경로
            File file;     //파일명까지 포함한 경로

            if(!state.equals(Environment.MEDIA_MOUNTED)){ //SDcard 의 상태가 쓰기 가능한 상태로 마운트되었는지 확인
                return;
            }

            path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            file= new File(path, "PhoneData.txt"); //파일명까지 포함함 경로의 File 객체 생성
            try { //데이터 추가가 가능한 파일 작성자(FileWriter 객체생성)
                FileWriter wr= new FileWriter(file,true); //두번째 파라미터 true: 기존파일에 내용 이어쓰기
                PrintWriter writer= new PrintWriter(wr);
                writer.println(message1);
                writer.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Log.e("LOG", "X:" + a + " Y:" + b + " Z:" + c + " " + time_data1);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}