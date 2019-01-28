package com.bcilab.lonelyelderly;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class InfoActivity extends AppCompatActivity {
    EditText editText;
    Uri uri_phoneNum;
    private static TextView number; //저장된 번호 띄우기

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        editText = (EditText) findViewById(R.id.editText);
        number =(TextView)findViewById(R.id.number); //저장된 번호 띄우기
    }

    public void OnClick(View v) {
        switch (v.getId()) {
            case R.id.button_save : {
                String phoneNum = editText.getText().toString();
                uri_phoneNum = Uri.parse("tel:" + phoneNum);
                Toast.makeText(InfoActivity.this, "긴급 연락처가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                number.setText(phoneNum); //저장된 번호 띄우기
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