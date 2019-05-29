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
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FallActivity extends AppCompatActivity {
    private static final String TAG = "FallActivity";
    private boolean mIsBound = false;
    private ConnectionService mConnectionService = null;

    private static TextView statusText;
    private static TextView accel;
    private static LineChart mChart;
    private Thread graphThread;
    private static boolean plotData = true;

    private static String[] SVM;
    private static Kalman mKalmanPrev;      // Kalman filter t value
    private static Kalman mKalmanNext;      // Karlam filter t+1 value
    private static float mPrev, mNext;      // Variable to save previous Kalman filter value

    public static Queue queue;
    private static DetectHandler detectHandler;
    private static Thread detectThread;


    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fall);
        statusText = (TextView) findViewById(R.id.statusText);
        accel = (TextView) findViewById(R.id.accel);

        // Bind service
        mIsBound = bindService(new Intent(FallActivity.this, ConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
        detectHandler = new DetectHandler();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Kalman Filter Initialize
        mKalmanPrev = new Kalman(0.0f);
        mKalmanNext = new Kalman(0.0f);
        SVM = new String[20];
        queue = new com.bcilab.lonelyelderly.Queue(SVM.length, 6.0f);   // LENGTH : SVM.length, TRESHOLD : Ihigh - Ilow (estimated val)

        // Real-time Line Chart
        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.getDescription().setEnabled(true);
        mChart.getDescription().setText("Real Time Falling Detection");
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

        if(graphThread != null){
            graphThread.interrupt();
        }

        if(detectThread != null){
            detectThread.interrupt();
        }

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
                    accel.setText("측정중");
//                    updateStatus("Connected");
                    //startDetect();
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
//                        updateStatus("Disconnected");
                        Toast.makeText(getApplicationContext(), R.string.ConnectionAlreadyDisconnected, Toast.LENGTH_LONG).show();
                        accel.setText("미측정");
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
        }
    }

    // 모니터링 상태 표시
    public static void updateStatus(final String str) {
        statusText.setText(str);
    }

    // 가속도 데이터 받아오기 및 그래프 그리기
    public static void updateAccel(final String str) {
        String[] data = str.split("\\s");
//        int det = 0;

        int timestamp = Integer.parseInt(data[21]);         // timestamp - for saveFile
        for(int i = 0; i < SVM.length; i++)                   // SVM[20]
            SVM[i] = data[i+1];

        float Ivalue;                                      // next - prev
        float prev = 0.0f;                                 // variable to apply Kalman filter
        float next = 0.0f;

        if(plotData){
            for(int i = 0; i < SVM.length - 1; i++) {
                prev = (float) mKalmanPrev.update(Float.parseFloat(SVM[i]));
                next = (float) mKalmanNext.update(Float.parseFloat(SVM[i+1]));
                Ivalue = next - prev;

                if(queue.isFull()) {
                    queue.Dequeue();
                    queue.Enqueue(Ivalue);
                } else
                    queue.Enqueue(Ivalue);

                addEntry(Ivalue, 0);
                saveFile(Ivalue, timestamp + (49*i));

                mPrev = prev;
                mNext = next;
            }
            plotData = false;
        }

//        det = queue.getDiffNum();
//
//        if(det == 1){
//            if(queue.getAbsSum() > 15.0f)
//                updateStatus("낙상 의심");
//            else
//                updateStatus("오인 행동");
//        } else
//            updateStatus("비낙상");
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
                final double th = 6.0f;       // 임계값
                int detection = 0;            // 임계값 초과 감지 횟수

                boolean monitoring = true;    // default : true, 낙상 감지시 : false (모니터링을 해제하고 연락기능으로 들어가므로)

                long start = 0;               // 최초 감지 시 타임스탬프
                long end;

                while(monitoring) {


                    /*
                    // 큐가 꽉 안찼으면 찰 때까지 대기함
                    while (!queue.isFull()) {
                        try {
                            Thread.sleep(1000);                 // 밴드와 스마트폰 간 수신 간격은 약 1100ms
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    // 임계점 넘어가면 detection++
                    if (queue.getAbsSum() > th) {
                        detection++;

                        queue.setEmpty();

                        // 최초 감지면 타임스탬프 마크
                        if (detection == 1)
                            start = System.currentTimeMillis();
//                        try {
//                            Thread.sleep(1000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                    }

                    // 타임스탬프가 존재하면 경과 시간 확인
                    if(start != 0) {
                        end = System.currentTimeMillis();

                        if (end - start < 10000)
                            continue;

                        if (detection > 3){
                            // detection 초기화
                            // start 초기화
                            updateStatus("격렬한 활동");
                            detection = 0;
                            start = 0;
                        } else {
                            // 낙상 의심
                            updateStatus("낙상 의심");
                            monitoring = false;
                        }
                    }
*/
                    /*int a=0;
                    end = System.currentTimeMillis();
                    if(start != 0) {
                        if (end - start < 10000) {
                            if (detection == 1 || detection == 2) {
                                a = 2;
                            } else if (detection == 3) {
                                a = 3;
                            }
                        }
                    }

                    if(end- start >=1000 && a ==2){
                        // 낙상 의심
                        updateStatus("낙상 의심");
                        monitoring = false;
                    }
                    else if(end- start >=1000 && a ==3){
                        updateStatus("격렬한 활동");
                        detection = 0;
                        start = 0;
                    }*/

                }

                Message message = detectHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putInt("emergency", -1);
                message.setData(bundle);
                detectHandler.sendMessage(message);
            }
        });

        detectThread.start();
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
            int elapsed = bundle.getInt("emergency");

            if(elapsed != -1)
                statusText.setText("낙상 의심");

            // 0 : infinity, -1 : only once
            // 10초 간 진동-off 시작
            makeEmergencyDialog();

            vibrator.vibrate(vibe_pattern, -1);

            try {
                Thread.sleep(10000);

                String str = getIntent().getStringExtra("phoneNum");
                final Uri uri = Uri.parse("tel:" + str);
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(uri);
                startActivity(intent);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private AlertDialog makeEmergencyDialog(){
        /* makeEmergencyDialog
         * 낙상 감지 알림상자
         * 제목 : 긴급
         * 내용 : 낙상 의심
         * 연락 버튼 누를 시 - 긴급연락처로 전화 걸기
         * 취소 버튼 누를 시 - 앱 종료
         */
        AlertDialog.Builder emergencyDialog;
        String str = getIntent().getStringExtra("phoneNum");
        final Uri uri = Uri.parse("tel:" + str);

        emergencyDialog = new AlertDialog.Builder(this);
        emergencyDialog.setTitle("긴급");
        emergencyDialog.setMessage("낙상 의심");

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

    public static void saveFile(float Ivalue, int timestamp){
        String state = Environment.getExternalStorageState();   // 외부저장소(SDcard)의 상태 얻어오기
        File path;                                              // 저장 데이터가 존재하는 디렉토리경로
        File file;                                              // 파일명까지 포함한 경로

        if(!state.equals(Environment.MEDIA_MOUNTED)){           // SDcard 의 상태가 쓰기 가능한 상태로 마운트되었는지 확인
            return;
        }

        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        file = new File(path, "BandData.txt");            // 파일명까지 포함함 경로의 File 객체 생성
        try {                                                  // 데이터 추가가 가능한 파일 작성자(FileWriter 객체생성)
            FileWriter wr = new FileWriter(file,true);  // 두번째 파라미터 true: 기존파일에 내용 이어쓰기
            PrintWriter writer = new PrintWriter(wr);
            writer.println(Ivalue + " " + timestamp);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
    private static void addEntry(float idx, int order){
        LineData data = mChart.getData();

        if (data != null){
            ILineDataSet set = data.getDataSetByIndex(order);

            if(set == null){
                set = createSet(order);
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), idx), order);
            data.setDrawValues(false);
            data.notifyDataChanged();
        }

        // let the chart know it's data has changed
        mChart.notifyDataSetChanged();

        mChart.setMaxVisibleValueCount(220);
        mChart.moveViewToX(data.getEntryCount());
    }

    private final static int[] colors = new int[] {
            ColorTemplate.VORDIPLOM_COLORS[0],
            ColorTemplate.VORDIPLOM_COLORS[1],
            ColorTemplate.VORDIPLOM_COLORS[4]
    };

    // LineChart createSet()
    private static LineDataSet createSet(int order){
        // order 필요 없어짐 - 수정해야 함
        LineDataSet set;
        set = new LineDataSet(null, "SVM Variation");
        set.setColor(colors[1]);
        set.setLineWidth(2.5f);
        set.setDrawCircles(false);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setMode(LineDataSet.Mode.LINEAR);
//        set.setCubicIntensity(0.2f);

        return set;
    }
}