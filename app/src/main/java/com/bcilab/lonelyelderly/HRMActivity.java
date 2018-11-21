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
import android.os.Vibrator;
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

    private static DetectHandler detectHandler;
    private static DetectThread detectThread;
    private static boolean isFirstZero = true;

    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrm);
        statusText = (TextView) findViewById(R.id.statusText);
        heartBPM = (TextView) findViewById(R.id.heartBPM);

        // Bind service
        mIsBound = bindService(new Intent(HRMActivity.this, ConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
        detectHandler = new DetectHandler();
//        detectThread = new DetectThread();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /*
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        uri = intent.getParcelableExtra("uri");

        뒤로 가기 버튼 누른 후, 연락처를 변경하고 들어왔을 때
        액티비티가 살아있다면 변경된 연락처를 받아오는 함수.
        (현재 코드는 뒤로 가기를 누르면 액티비티 소멸)
        문제점 : 여기서 가는 액티비티는 MainActivity 와 전화화면
        전화화면의 경우 전달하는 intent 가 없으므로 에러가 날 것이다.
        그래서 지금은 액티비티를 소멸시키는 방법을 사용
        (Main 으로 돌아갈 경우만 소멸, 전화화면 X)
    }
    */

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
                // if detectThread is working, kill the thread.
                if(detectThread.getState() == Thread.State.RUNNABLE) {
                    detectThread.interrupt();
                    detectThread.stop();
                }

                if (mIsBound == true && mConnectionService != null) {
                    if (mConnectionService.closeConnection() == false) {
                        updateStatus("Disconnected");
                        Toast.makeText(getApplicationContext(), R.string.ConnectionAlreadyDisconnected, Toast.LENGTH_LONG).show();
                    }
                }
                break;
            }
            case R.id.buttonBack: {
                // if detectThread is working, kill the thread.
                if(detectThread.getState() == Thread.State.RUNNABLE) {
                    detectThread.interrupt();
                    detectThread.stop();
                }

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

    // 모니터링 상태 표시
    public static void updateStatus(final String str) {
        statusText.setText(str);
    }

    // 심박수 화면에 띄우기 & 심정지 감지 시 스레드 실행
    public static void updateHeartBPM(final String str) {
        heartBPM.setText(str);

        // 측정된 심박수가 0이고, 스레드 생성 이후 0이 최초로 감지되었을 때 스레드 실행
        // UI가 아닌 실제 측정값으로 판단하므로 UI text 가 0이어도 실행 안 될 수 있음
        if(Integer.parseInt(str) == 0 && isFirstZero){
            isFirstZero = false;
            detectThread = new DetectThread();
            detectThread.start();
        }
    }

    static class DetectThread extends Thread {
        // 심정지 감지 시, 10초 동안 시간 메시지를 DetectHandler 에게 전달하고 소멸
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

        /* millisecond
         * vibe_pattern[odd] : waiting time
         * vibe_pattern[even] : vibrating time
         */
        long[] vibe_pattern = { 100, 100, 100, 100, 100, 100, 100, 100, 100, 100,
                                100, 100, 100, 100, 100, 100, 100, 100, 100, 100,
                                100, 100, 100, 100, 100, 100, 100, 100, 100, 100 };

        // 받은 메시지를 통해 경과시간 혹은 알림 상자 띄우기
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Bundle bundle = msg.getData();
            int elapsed = bundle.getInt("elapsed");
            statusText.setText("BPM = 0 최초 감지 후 " + elapsed + "초 경과");

            if (elapsed == 10) {
                // 0 : infinity, -1 : only once
                // 10초 간 진동-off 시작
                vibrator.vibrate(vibe_pattern, -1);
                makeEmergencyDialog();
            }
        }
    }

    private AlertDialog makeEmergencyDialog(){
        /* makeEmergencyDialog
         * 심정지 알림상자
         * 제목 : 긴급
         * 내용 : 심정지 감지 (무반응 시 10초 후 자동연락)
         * 연락 버튼 누를 시 - 긴급연락처로 전화 걸기
         * 취소 버튼 누를 시 - isFirstZero 초기화 (DetectThread 재활용을 위해)
         */
        AlertDialog.Builder emergencyDialog;
        final Uri uri = Uri.parse("tel:01012345678");

        emergencyDialog = new AlertDialog.Builder(this);
        emergencyDialog.setTitle("긴급");
        emergencyDialog.setMessage("심정지 감지 (무반응 시 10초 후 자동연락)");

        emergencyDialog.setPositiveButton("연락", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Hard Coded : should be modified later

                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(uri);
                startActivity(intent);
            }
        });

        emergencyDialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                vibrator.cancel();
                isFirstZero = true;
            }
        });

        // To do list
        // 타이머 여기서부터 20초 재기 - 20초 후 자동 연락
        // 알림 상자 팝업 시 진동과 벨소리 기능 추가 - 진동 추가됨
        return emergencyDialog.show();
    }
}