package com.bcilab.lonelyelderly;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class InfoActivity extends AppCompatActivity {
    public static final int REQUEST_CONTACTS = 8282;

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
            case R.id.button_load : {
                // 단말기 내장되어 있는 연락처 앱 호출하기
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                // 호출 후, 연락처앱에서 전달되는 결과물을 받기 위해 startActivityForResult로 실행
                startActivityForResult(intent, REQUEST_CONTACTS);
                break;
            }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_CONTACTS) {
            Cursor cursor = getContentResolver().query(data.getData(),
                    new String[] { ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER },
                    null, null, null);

            cursor.moveToFirst();
//            이름획득
//            receiveName = cursor.getString(0);

            //전화번호 획득
            editText.setText(cursor.getString(1));
//            uri_phoneNum = Uri.parse(cursor.getString(1));
            cursor.close();
        }
    }
}