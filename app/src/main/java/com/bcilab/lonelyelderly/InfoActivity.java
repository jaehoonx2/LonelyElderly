package com.bcilab.lonelyelderly;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class InfoActivity extends AppCompatActivity {
    public static final int REQUEST_CONTACTS = 8282;

    EditText editText;
    Uri uri_phoneNum;
    private static TextView number;                 // 저장된 번호 띄우기

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        editText = (EditText) findViewById(R.id.editText);
        number = (TextView)findViewById(R.id.number);
        number.setText(phoneNumLoad());             // 저장된 번호 띄우기
        uri_phoneNum = Uri.parse("tel:" + number.getText().toString());
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
                number.setText(phoneNum);           // 저장된 번호 띄우기
                phoneNumSave(phoneNum);
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

    public void phoneNumSave(String phoneNum){
        String state = Environment.getExternalStorageState();   // 외부저장소(SDcard)의 상태 얻어오기
        File path;                                              // 저장 데이터가 존재하는 디렉토리경로
        File file;                                              // 파일명까지 포함한 경로

        if(!state.equals(Environment.MEDIA_MOUNTED)){           // SDcard의 상태가 쓰기 가능한 상태로 마운트되었는지 확인
            return;
        }

        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        file = new File(path, "phoneNum.txt");              // 파일명까지 포함함 경로의 File 객체 생성
        try {                                                     // 데이터 추가가 가능한 파일 작성자 (FileWriter 객체생성)
            BufferedWriter buf = new BufferedWriter(new FileWriter(file, false));
            buf.append(phoneNum);                                 // 번호 쓰기
            buf.newLine();                                        // 개행
            buf.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // 경로의 텍스트 파일읽기
    public static String phoneNumLoad(){
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/phoneNum.txt";

        StringBuffer strBuffer = new StringBuffer();

        try {
            InputStream is = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = "";

            while((line = reader.readLine()) != null)
                strBuffer.append(line + "\n");

            reader.close();
            is.close();
        } catch (IOException e){
            e.printStackTrace();
            return "";
        }
        
        return strBuffer.toString();
    }
}