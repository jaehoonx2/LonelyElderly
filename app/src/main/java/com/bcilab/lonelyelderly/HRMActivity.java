package com.bcilab.lonelyelderly;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class HRMActivity extends AppCompatActivity {
    private static TextView mTextView;
    private static TextView heartBPM;
    private boolean mIsBound = false;
    private ConnectionService mConnectionService = null;

    public static Thread thread;
    public AlertDialog.Builder emergencyDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrm);
        mTextView = (TextView) findViewById(R.id.tvStatus);
        heartBPM = (TextView) findViewById(R.id.heartBPM);

        // Bind service
        mIsBound = bindService(new Intent(HRMActivity.this, ConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);

        // Dialog
        /*
        * 심정지 알림상자
        * 제목 : 긴급
        * 내용 : 심정지 감지 (무반응 시 20초 후 자동연락)
        * 연락 버튼 누를 시 - 긴급연락 및 스레드 종료
        * 취소 버튼 누를 시 - 스레드 종료
        */
        emergencyDialog = new AlertDialog.Builder(this);
        emergencyDialog.setTitle("긴급");
        emergencyDialog.setMessage("심정지 감지 (무반응 시 20초 후 자동연락)");
        emergencyDialog.setPositiveButton("연락", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!thread.isInterrupted())
                    thread.interrupt();

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
            public void onClick(DialogInterface dialog, int which) {
                if(!thread.isInterrupted())
                    thread.interrupt();
            }
        });

        // Thread
        /*
         * 스레드 진행 과정
         * 1. 스레드가 시작되면 먼저 10초 간 대기
         * 2-1. 10초 후에도 심박수가 0일 경우, 알림상자를 띄우고 20초 재대기
         * 2-2. 10초 후에 심박수가 정상으로 돌아온 경우, 스레드 종료
         * 3. 20초 동안 알림상자에 대한 피드백이 없을 경우 자동연락
         */
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    for(int i = 0; i<10 && getTextView() == 0 ; i++) {
                        updateTextView("BPM = 0 최초감지 후 " + i + "초 경과");
                        Thread.sleep(1000);
                    }

                    if(getTextView() != 0){
                        thread.interrupt();
                    } else {
                        emergencyDialog.show();
                        Thread.sleep(20000);
                    }

                    Intent intent = new Intent(Intent.ACTION_CALL);
                    if (MainActivity.uri != null && !MainActivity.uri.equals(Uri.EMPTY)) {
                        intent.setData(MainActivity.uri);
                    } else {
                        MainActivity.uri.parse("tel:119");
                        intent.setData(MainActivity.uri);
                    }
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Clean up connections
        if (mIsBound == true && mConnectionService != null) {
            if (mConnectionService.closeConnection() == false) {
                updateTextView("Disconnected");
            }
        }
        // Un-bind service
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        super.onDestroy();
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
                        updateTextView("Disconnected");
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
            updateTextView("onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mConnectionService = null;
            mIsBound = false;
            updateTextView("onServiceDisconnected");
        }
    };

    public static int getTextView() {
        String data = mTextView.getText().toString();
        return Integer.parseInt(data);
    }

    public static void updateTextView(final String str) {
        mTextView.setText(str);
    }

    public static void updateHR(final String str) {
        heartBPM.setText(str);

        /*
         * BPM = 0 값이 들어왔을 때, 기존의 thread 죽어있으면 살리기
         * BPM != 0 값이 들어왔을 때, 기존의 thread 살아있으면 죽이기
         */
        if(Integer.parseInt(str) == 0) {
            if(!thread.isAlive())
                thread.start();
        } else {
            if(!thread.isInterrupted())
                thread.interrupt();
        }
    }
}