package com.bcilab.lonelyelderly;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class InfoActivity extends AppCompatActivity {
    EditText editText;
    Uri uri_phoneNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        editText = (EditText) findViewById(R.id.editText);
    }

    public void OnClick(View v) {
        switch (v.getId()) {
            case R.id.button_save : {
                String phoneNum = editText.getText().toString();
                uri_phoneNum = Uri.parse("tel:" + phoneNum);
                Toast.makeText(InfoActivity.this, "긴급 연락처가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.button_back : {
                Intent intent = new Intent();
                intent.putExtra("uri_phoneNum", uri_phoneNum);
                setResult(RESULT_OK, intent);
                finish();
                break;
            }
            default :
        }
    }
}