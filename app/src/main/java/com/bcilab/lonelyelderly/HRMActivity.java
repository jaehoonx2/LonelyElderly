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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
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
        /*
        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(10f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(false);
        */
        startPlot();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(graphThread != null){
            graphThread.interrupt();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        graphThread.interrupt();

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

    // 모니터링 상태 표시
    public static void updateStatus(final String str) {
        statusText.setText(str);
    }

    // 심박수 그래프 그리기 & 심정지 감지 시 스레드 실행
    public static void updateHeartBPM(final String str) {
        heartBPM.setText(str);

        // addTextChangedListener - if heartBPM text is changed, add an entry into graph
        heartBPM.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(plotData){
                    addEntry(Integer.parseInt(heartBPM.getText().toString()));
                    plotData = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

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
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            mChart.setMaxVisibleValueCount(220);
            mChart.moveViewToX(data.getEntryCount());
        }
    }

    // LineChart createSet()
    private static LineDataSet createSet(){
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.RED);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);

        return set;
    }
}