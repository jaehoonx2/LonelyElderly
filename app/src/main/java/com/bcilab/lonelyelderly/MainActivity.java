package com.bcilab.lonelyelderly;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_HRM = 101;
    public static final int REQUEST_CODE_INFO = 102;
    public static Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uri = Uri.EMPTY;
    }

    public void OnClick(View v){
        switch (v.getId()){
            case R.id.button_hrm : {
                Intent intent = new Intent(getApplicationContext(), HRMActivity.class);

                // 심정지 시 긴급연락처 전송 (미지정 시 119)
                if (uri != null && !uri.equals(Uri.EMPTY)) {
                    intent.putExtra("uri", uri);
                } else {
                    intent.putExtra("uri", "119");
                }
                startActivityForResult(intent, REQUEST_CODE_HRM);
                break;
            }
            case R.id.button_info : {
                Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
                startActivityForResult(intent, REQUEST_CODE_INFO);
                break;
            }
            case R.id.button_call : {
                Intent intent = new Intent(Intent.ACTION_CALL);
                if (uri != null && !uri.equals(Uri.EMPTY)) {
                    intent.setData(uri);
                } else {
                    Toast.makeText(getApplicationContext(), "긴급 연락처를 저장하십시오", Toast.LENGTH_SHORT).show();
                }

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
