package com.bcilab.lonelyelderly;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class HRMActivity extends AppCompatActivity {
    private static final String TAG = "HRMActivity";

    private static TextView statusText;
    private static TextView heartBPM;
    private boolean mIsBound = false;
    private ConnectionService mConnectionService = null;

    private static LineChart mChart;
    private Thread graphThread;
    private static boolean plotData = true;

    private static DetectHandler detectHandler;
    private static Thread detectThread;

    private static String[] SVM;

    static boolean isHeartAttack = false;
    Vibrator vibrator;
    static long start, end;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrm);
        statusText = (TextView) findViewById(R.id.statusText);
        heartBPM = (TextView) findViewById(R.id.heartBPM);
        SVM = new String[20];

        // Bind service
        mIsBound = bindService(new Intent(HRMActivity.this, ConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
        detectHandler = new DetectHandler();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Real-time Line Chart
        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.getDescription().setEnabled(true);
        mChart.getDescription().setText("Real Time Heart Rate Monitoring");
        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(true);
        mChart.setPinchZoom(false);
        mChart.setBackgroundColor(Color.WHITE);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);

        startPlot();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(graphThread != null){
            graphThread.interrupt();
        }

        if(detectThread != null){
            detectThread.interrupt();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(graphThread != null)
            graphThread.interrupt();

        if(detectThread != null)
            detectThread.interrupt();

        // Clean up connections
        if (mIsBound == true && mConnectionService != null) {
            if (mConnectionService.closeConnection() == false) {
//                updateStatus("Disconnected");
            }
        }
        // Un-bind service
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mConnectionService = ((ConnectionService.LocalBinder) service).getService();
//            updateStatus("onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mConnectionService = null;
            mIsBound = false;
//            updateStatus("onServiceDisconnected");
        }
    };

    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.buttonConnect: {
                if (mIsBound == true && mConnectionService != null) {
                    mConnectionService.findPeers();
                    start = System.currentTimeMillis();
                }
                break;
            }
            case R.id.buttonDisconnect: {
                if(graphThread != null)
                    graphThread.interrupt();

                if(detectThread != null)
                    detectThread.interrupt();

                if (mIsBound == true && mConnectionService != null) {
                    if (mConnectionService.closeConnection() == false) {
                        start = end = 0;
//                        updateStatus("Disconnected");
                        Toast.makeText(getApplicationContext(), R.string.ConnectionAlreadyDisconnected, Toast.LENGTH_LONG).show();
                    }
                }
                break;
            }
            case R.id.buttonBack: {
                if(graphThread != null)
                    graphThread.interrupt();

                if(detectThread != null)
                    detectThread.interrupt();

                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
                break;
            }
            default:
        }
    }

    // 모니터링 상태 표시
    public static void updateStatus(final String str) {
        statusText.setText(str);
    }

    // 심박수 그래프 그리기 & 수치 나타내기
    public static void updateHeartBPM(final String str) {
        end = System.currentTimeMillis();
        if(end - start < 5000) {
            updateStatus("밴드를 착용해주세요");
            heartBPM.setText("수신 대기중");
            return;
        }

        String[] data = str.split("\\s");

        for(int i = 0; i < SVM.length; i++)
            SVM[i] = data[i+1];

        if(plotData){
            switch (data[0]){
                case "-3" : {
                                isHeartAttack = false;                  // 탈착 상황
                                heartBPM.setText("미착용");
                                updateStatus("밴드 탈착");
//                                if (detectThread != null)
//                                    detectThread.interrupt();

                                break;
                           }
                case "0" : {
//                                if(isHeartAttack){
//                                    heartBPM.setText(data[0]);
//                                    addEntry(Integer.parseInt(data[0]));
//                                    break;
//                                }

                                if (checkMove()) {                      // 센서 에러 의심 - bpm - 0 이지만 움직임 감지
                                    isHeartAttack = false;

                                    heartBPM.setText(data[0]);
                                    updateStatus("센서 이상 의심");
//                                    if (detectThread != null)
//                                        detectThread.interrupt();
                                } else {                                // 심정지 의심 - bpm 0이면서 부동 자세
                                    isHeartAttack = true;

                                    heartBPM.setText(data[0]);
                                    addEntry(Integer.parseInt(data[0]));
                                    updateStatus("심정지 의심");
                                    if (detectThread == null)
                                        startDetect();
                                }
                                break;
                          }
                default : {
                                isHeartAttack = false;                  // 일반 상황
                                updateStatus("심박수 모니터링중");
                                heartBPM.setText(data[0]);
                                addEntry(Integer.parseInt(data[0]));
                          }
                }
            plotData = false;
        }
    }

    private static boolean checkMove(){
        // 착용자의 움직임이 있는지 확인
        // 움직임이 있으면 true 반환
        // 탈착상황이 의심되면 false 반환
        float isum = 0.0f;
        for (int i = 0; i < SVM.length - 1; i++)
            isum += Math.abs(Float.parseFloat(SVM[i + 1]) - Float.parseFloat(SVM[i]));
        float iavg = isum / (SVM.length - 1);

        if(iavg > 0.05)
            return true;
        else
            return false;
    }

    // LineChart startDetect()
    private static void startDetect(){

        if(detectThread != null){
            // detectThread.interrupt();
            return;
        }

        detectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int result = 0;

                for(int i = 0; i < 10; i++){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(isHeartAttack) {
                        result = -1;
                        continue;
                    } else {
                        result = 0;
                        break;
                    }
                }

                if(result == -1) {
                    Message message = detectHandler.obtainMessage();
                    Bundle bundle = new Bundle();

                    bundle.putInt("emergency", -1);
                    message.setData(bundle);
                    detectHandler.sendMessage(message);
                }
            }
        });

        detectThread.start();
    }

    class DetectHandler extends Handler {

        /* millisecond
         * vibe_pattern[odd] : waiting time
         * vibe_pattern[even] : vibrating time
         */
        long[] vibe_pattern = { 100, 100, 100, 100, 100, 100, 100, 100, 100, 100 };

        // 받은 메시지를 통해 경과시간 혹은 알림 상자 띄우기
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Bundle bundle = msg.getData();
            int elapsed = bundle.getInt("emergency");

            if(elapsed != -1)
                statusText.setText("심정지 상황 의심");

            // 0 : infinity, -1 : only once
            // 10초 간 진동-off 시작
            makeEmergencyDialog();
            vibrator.vibrate(vibe_pattern, -1);

            long mstart, mend;
            mstart = System.currentTimeMillis();
            do{
                mend = System.currentTimeMillis();
            } while(mend - mstart < 10000);

            String str = getIntent().getStringExtra("phoneNum");
            final Uri uri = Uri.parse("tel:" + str);
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(uri);
            startActivity(intent);

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
        String str = getIntent().getStringExtra("phoneNum");
        final Uri uri = Uri.parse("tel:" + str);

        emergencyDialog = new AlertDialog.Builder(this);
        emergencyDialog.setTitle("긴급");
        emergencyDialog.setMessage("심정지 감지");

        emergencyDialog.setPositiveButton("연락", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Hard Coded : should be modified later

                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(uri);
                startActivity(intent);
            }
        });

        emergencyDialog.setNegativeButton("취소(앱 종료)", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 앱 종료
                moveTaskToBack(true);
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });

        // To do list
        // 알림 상자 팝업 시 진동과 벨소리 기능 추가 - 진동 추가됨
        return emergencyDialog.show();
    }

    // LineChart startPlot()
    private void startPlot(){

        if(graphThread != null){
            graphThread.interrupt();
        }

        graphThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    plotData = true;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        graphThread.start();
    }

    // LineChart addEntry()
    private static void addEntry(int idx){
        LineData data = mChart.getData();

        if(data != null){
            ILineDataSet set = data.getDataSetByIndex(0);

            if(set  == null){
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), idx), 0);
            data.setDrawValues(false);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            mChart.setMaxVisibleValueCount(220);
            mChart.moveViewToX(data.getEntryCount());
        }
    }

    // LineChart createSet()
    private static LineDataSet createSet(){
        LineDataSet set = new LineDataSet(null, "Beat per Minute");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(2f);
        set.setColor(Color.RED);
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setDrawCircles(false);
        return set;
    }
}