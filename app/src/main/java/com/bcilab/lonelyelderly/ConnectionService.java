package com.bcilab.lonelyelderly;

import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ConnectionService extends SAAgent {
    private static final String TAG = "LonelyElderly";
    private static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
    private final IBinder mBinder = new LocalBinder();
    private ServiceConnection mConnectionHandler = null;
    Handler statusHandler = new Handler();
    Handler HRHandler = new Handler();
    Handler accHandler = new Handler();

    public ConnectionService() { super(TAG, SASOCKET_CLASS); }

    @Override
    public void onCreate() {
        super.onCreate();
        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
        } catch (SsdkUnsupportedException e) {
            // try to handle SsdkUnsupportedException
            if (processUnsupportedException(e) == true) {
                return;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            /*
             * Your application can not use Samsung Accessory SDK. Your application should work smoothly
             * without using this SDK, or you may want to notify user and close your application gracefully
             * (release resources, stop Service threads, close UI th, etc.)
             */
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        if ((result == SAAgent.PEER_AGENT_FOUND) && (peerAgents != null)) {
            for(SAPeerAgent peerAgent:peerAgents)
                requestServiceConnection(peerAgent);
        } else if (result == SAAgent.FINDPEER_DEVICE_NOT_CONNECTED) {
            Toast.makeText(getApplicationContext(), "FINDPEER_DEVICE_NOT_CONNECTED", Toast.LENGTH_LONG).show();
            updateStatus("Disconnected");
        } else if (result == SAAgent.FINDPEER_SERVICE_NOT_FOUND) {
            Toast.makeText(getApplicationContext(), "FINDPEER_SERVICE_NOT_FOUND", Toast.LENGTH_LONG).show();
            updateStatus("Disconnected");
        } else {
            Toast.makeText(getApplicationContext(), R.string.NoPeersFound, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {
        if (peerAgent != null) {
            acceptServiceConnectionRequest(peerAgent);
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        if (result == SAAgent.CONNECTION_SUCCESS) {
            this.mConnectionHandler = (ServiceConnection) socket;
            updateStatus("Connected");
        } else if (result == SAAgent.CONNECTION_ALREADY_EXIST) {
            updateStatus("Connected");
            Toast.makeText(getBaseContext(), "CONNECTION_ALREADY_EXIST", Toast.LENGTH_LONG).show();
        } else if (result == SAAgent.CONNECTION_DUPLICATE_REQUEST) {
            Toast.makeText(getBaseContext(), "CONNECTION_DUPLICATE_REQUEST", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getBaseContext(), R.string.ConnectionFailure, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {
        super.onError(peerAgent, errorMessage, errorCode);
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
        final SAPeerAgent[] peers = peerAgents;
        final int status = result;
        statusHandler.post(new Runnable() {
            @Override
            public void run() {
                if (peers != null) {
                    if (status == SAAgent.PEER_AGENT_AVAILABLE) {
                        Toast.makeText(getApplicationContext(), "PEER_AGENT_AVAILABLE", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "PEER_AGENT_UNAVAILABLE", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    public class ServiceConnection extends SASocket {
        public ServiceConnection() {
            super(ServiceConnection.class.getName());
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
            final String message =new String(data);
            String[] sensors = message.split("\\s");
            FileSave(sensors);
            Log.i(TAG, "bpm "+ sensors[0] + " " +
                    "acc "  + sensors[1] + " " + sensors[2] + " " + sensors[3] + " "
                    + sensors[4] + " " + sensors[5] + " " + sensors[6] + " "
                    + sensors[7] + " " + sensors[8] + " " + sensors[9] + " "
                    + sensors[10] + " " + sensors[11]);
            updateHeartBPM(sensors[0]);
            updateAcc(message);
        }

        public void FileSave(String[] sensors){
            int[] timestamp = new int[9];

            int original = Integer.parseInt(sensors[11]);

            for(int i=0 ; i<9 ; i++){
                timestamp[i] = original + 24*(i+1);
            }

            String a0 = sensors[0];
            String a1 = sensors[1]+ " " + sensors[11];
            String a2 = sensors[2]+ " " + timestamp[0];
            String a3 = sensors[3]+ " " + timestamp[1];
            String a4 = sensors[4]+ " " + timestamp[2];
            String a5 = sensors[5]+ " " + timestamp[3];
            String a6 = sensors[6]+ " " +  timestamp[4];
            String a7 = sensors[7]+ " " + timestamp[5];
            String a8 = sensors[8]+ " " + timestamp[6];
            String a9 = sensors[9]+ " " + timestamp[7];
            String a10 = sensors[10]+ " " + timestamp[8];

            String message0= a1 + "\n" + a2 + "\n" + a3 + "\n" + a4 + "\n" + a5 + "\n"
                    + a6 + "\n" + a7 + "\n" + a8 +"\n" + a9 + "\n" + a10;

            String state= Environment.getExternalStorageState(); //외부저장소(SDcard)의 상태 얻어오기
            File path;    //저장 데이터가 존재하는 디렉토리경로
            File file;     //파일명까지 포함한 경로

            if(!state.equals(Environment.MEDIA_MOUNTED)){ //SDcard 의 상태가 쓰기 가능한 상태로 마운트되었는지 확인
                return;
            }

            path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            file= new File(path, "BandData.txt"); //파일명까지 포함함 경로의 File 객체 생성
            try { //데이터 추가가 가능한 파일 작성자(FileWriter 객체생성)
                FileWriter wr= new FileWriter(file,true); //두번째 파라미터 true: 기존파일에 내용 이어쓰기
                PrintWriter writer= new PrintWriter(wr);
                writer.println(message0);
                writer.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        protected void onServiceConnectionLost(int reason) {
            updateStatus("Disconnected");
            closeConnection();
        }
    }

    public class LocalBinder extends Binder {
        public ConnectionService getService() {
            return ConnectionService.this;
        }
    }

    public void findPeers() {
        findPeerAgents();
    }

    public boolean closeConnection() {
        if (mConnectionHandler != null) {
            mConnectionHandler.close();
            mConnectionHandler = null;
            return true;
        } else {
            return false;
        }
    }

    private boolean processUnsupportedException(SsdkUnsupportedException e) {
        e.printStackTrace();
        int errType = e.getType();
        if (errType == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED
                || errType == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
            /*
             * Your application can not use Samsung Accessory SDK. You application should work smoothly
             * without using this SDK, or you may want to notify user and close your app gracefully (release
             * resources, stop Service threads, close UI th, etc.)
             */
            stopSelf();
        } else if (errType == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
            Log.e(TAG, "You need to install Samsung Accessory SDK to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED) {
            Log.e(TAG, "You need to update Samsung Accessory SDK to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED) {
            Log.e(TAG, "We recommend that you update your Samsung Accessory SDK before using this application.");
            return false;
        }
        return true;
    }

    private void updateStatus(final String str) {
        statusHandler.post(new Runnable() {
            @Override
            public void run() {
                try{
                HRMActivity.updateStatus(str);
                } catch (Exception e) {}
            }
        });
    }

    private void updateAcc(final String data) {
        accHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    FallActivity.updateAccel(data);
                } catch (Exception e) {}

            }
        });
    }

    private void updateHeartBPM(final String data) {
        HRHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    HRMActivity.updateHeartBPM(data);
                } catch (Exception e) {}
            }
        });
    }
}