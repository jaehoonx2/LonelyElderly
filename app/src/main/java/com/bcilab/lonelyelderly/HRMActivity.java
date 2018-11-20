package com.bcilab.lonelyelderly;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class HRMActivity extends AppCompatActivity {
    private static TextView statusText;
    private static TextView heartBPM;
    private boolean mIsBound = false;
    private ConnectionService mConnectionService = null;

    DetectHandler detectHandler;
    static DetectThread detectThread;
    static boolean isFirstZero = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrm);
        statusText = (TextView) findViewById(R.id.statusText);
        heartBPM = (TextView) findViewById(R.id.heartBPM);

        // Bind service
        mIsBound = bindService(new Intent(HRMActivity.this, ConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
        detectHandler = new DetectHandler();
        detectThread = new DetectThread();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up connections
        if (mIsBound == true && mConnectionService != null) {
            if (mConnectionService.closeConnection() == false) {
                updateStatus("Disconnected");
            }
        }
        // Un-bind service
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.buttonConnect: {
                if (mIsBound == true && mConnectionService != null) {
                    mConnectionService.findPeers();
                }
                break;
            }
            case R.id.buttonDisconnect: {
                if (mIsBound == true && mConnectionService != null) {
                    if (mConnectionService.closeConnection() == false) {
                        updateStatus("Disconnected");
                        Toast.makeText(getApplicationContext(), R.string.ConnectionAlreadyDisconnected, Toast.LENGTH_LONG).show();
                    }
                }
                break;
            }
            case R.id.buttonBack: {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
                break;
            }
            default:
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mConnectionService = ((ConnectionService.LocalBinder) service).getService();
            updateStatus("onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mConnectionService = null;
            mIsBound = false;
            updateStatus("onServiceDisconnected");
        }
    };

    private AlertDialog makeEmergencyDialog(){
        /* Dialog
         * 심정지 알림상자
         * 제목 : 긴급
         * 내용 : 심정지 감지 (무반응 시 20초 후 자동연락)
         * 알림상자 호출 시 스레드는 일시정지됨
         * 연락 버튼 누를 시 - 긴급연락 및 스레드 종료
         * 취소 버튼 누를 시 - 스레드 재개
         */
        AlertDialog.Builder emergencyDialog;

        emergencyDialog = new AlertDialog.Builder(this);
        emergencyDialog.setTitle("긴급");
        emergencyDialog.setMessage("심정지 감지 (무반응 시 20초 후 자동연락)");

        emergencyDialog.setPositiveButton("연락", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                if (MainActivity.uri != null && !MainActivity.uri.equals(Uri.EMPTY)) {
                    intent.setData(MainActivity.uri);
                }
                else {
                    MainActivity.uri.parse("tel:119");
                    intent.setData(MainActivity.uri);
                }
                startActivity(intent);
            }
        });

        emergencyDialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        return emergencyDialog.show();
    }

    // 상태 표시
    public static void updateStatus(final String str) {
        statusText.setText(str);
    }

    // 심박수 화면에 띄우기
    public static void updateHeartBPM(final String str) {
        heartBPM.setText(str);

        if(Integer.parseInt(str) == 0 && isFirstZero){
            isFirstZero = false;
            detectThread.start();
        }
    }

    class DetectThread extends Thread {
        /* DetectThread
         * 스레드 진행 과정
         * 1. 0이 감지되면 스레드 시작
         * 2. 최초 10초 동안 대기하면서 진행시간을 detectHandler 에게 알려줌
         * 3. 10초 후 스레드 종료
         */
        @Override
        public void run() {
            for(int elapsed = 0; elapsed < 11; elapsed++){
               Message message = detectHandler.obtainMessage();
               Bundle bundle = new Bundle();
               bundle.putInt("elapsed", elapsed);
               message.setData(bundle);

               detectHandler.sendMessage(message);

               try {
                   Thread.sleep(1000);
               } catch (Exception e) {
                   Log.e("DetectThread", "Exception in processing message.", e);
               }
           }
        }
    }

    class DetectHandler extends Handler {

        // 받은 메시지 처리, 여기서 UI 직접 접근 가능
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Bundle bundle = msg.getData();
            int elapsed = bundle.getInt("elapsed");
            statusText.setText("BPM = 0 최초 감지 후 " + elapsed + "초 경과");

            if (elapsed == 10) {
                makeEmergencyDialog();
            }
        }
    }
}