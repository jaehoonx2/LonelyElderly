package com.bcilab.lonelyelderly;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
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

public class FallActivity extends AppCompatActivity {
    private static final String TAG = "FallActivity";

    private static TextView statusText;
    private boolean mIsBound = false;
    private ConnectionService mConnectionService = null;

    private static LineChart mChart;
    private Thread graphThread;
    private static boolean plotData = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fall);
        statusText = (TextView) findViewById(R.id.statusText);

        // Bind service
        mIsBound = bindService(new Intent(FallActivity.this, ConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);

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

        if (graphThread != null) {
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
                    updateStatus("Connected");
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

    // 모니터링 상태 표시
    public static void updateStatus(final String str) {
        statusText.setText(str);
    }

    // 가속도 데이터 받아오기 및 그래프 그리기
    public static void updateAccel(final String str) {
        final String[] accel_data = str.split("\\s");

        Log.d(TAG, "accel_data length : " + accel_data.length);

        if(plotData){
            for(int i = 1; i < 31; i++){
                if(i % 3 == 1)  // accel_x
                    addEntry(Float.parseFloat(accel_data[i]), 0);
                else if(i % 3 == 2) // accel_y
                    addEntry(Float.parseFloat(accel_data[i]), 1);
                else    // accel_z
                    addEntry(Float.parseFloat(accel_data[i]), 2);
            }

            plotData = false;
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
        LineDataSet set;
        int color = colors[order % colors.length];
        switch(order){
            case 0 :
                set = new LineDataSet(null, "x");
                set.setColor(color);
                break;
            case 1 :
                set = new LineDataSet(null, "y");
                set.setColor(color);
                break;
            default:
                set = new LineDataSet(null, "z");
                set.setColor(color);
                break;
        }

        set.setLineWidth(2.5f);
        set.setDrawCircles(false);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setMode(LineDataSet.Mode.LINEAR);
//        set.setCubicIntensity(0.2f);

        return set;
    }
}