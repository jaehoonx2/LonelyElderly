package com.bcilab.lonelyelderly;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_HRM = 101;
    public static final int REQUEST_CODE_INFO = 102;
//    public static final int REQUEST_CODE_CALL = 103;
//    public static final int REQUEST_CODE_EXIT = 104;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void OnClick(View v){
        switch (v.getId()){
            case R.id.button_hrm : {
               Intent intent = new Intent(getApplicationContext(), HRMActivity.class);
               startActivityForResult(intent, REQUEST_CODE_HRM);
               break;
            }
            case R.id.button_info : {
//                Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
//                startActivityForResult(intent, REQUEST_CODE_INFO);
                break;
            }
            case R.id.button_call : {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:01065775125"));
                try{
                    startActivity(intent);
                } catch (Exception e){
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_HRM){
            // Response from HRMActivity
        }

        if(resultCode == RESULT_OK){
            // Normal response from HRMActivity
        }
    }
}
