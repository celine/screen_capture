package com.screencapture;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

/**
 * Created by wenchihhsieh on 2017/3/20.
 */

public class RecordScreenActivity extends AppCompatActivity implements Constants {
    private int REQUEST_CODE = 101;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection projection;
    private int densityDpi;
    private int DISPLAY_WIDTH = 1080;
    private int DISPLAY_HEIGHT = 960;
    private boolean mBound = false;
    private ServiceConnection mConnection;
    private VirtualDisplay mVirtualDisplay;
    private Surface mediaSureface;
    private Messenger mService;
    private Object connectLocker = new Object();
    private static final String LOG_TAG = RecordScreenActivity.class.getSimpleName();
    private View startRecord;
    private Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "receive " + msg.what);
            switch (msg.what) {
                case STOP_RECORD:
                    stopRecord();
                    break;
                case MSG_REGISTER_ACK_NOT_READY:
                    enableRecordingBtn(false);
                    break;
                case MSG_REGISTER_ACK_READY:
                    Surface mediaSureface = (Surface) msg.obj;
                    setSurface(mediaSureface);
                    enableRecordingBtn(true);
                    break;
            }
        }
    });

    private void enableRecordingBtn(boolean enable) {
        startRecord.setEnabled(enable);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        float factor = 0.4f;
        DISPLAY_WIDTH = (int) (getResources().getDisplayMetrics().widthPixels * factor);
        DISPLAY_HEIGHT = (int) (getResources().getDisplayMetrics().heightPixels * factor);
        Log.d(LOG_TAG, "width " + DISPLAY_WIDTH);
        Intent intent = new Intent();
        intent.putExtra(SCREEN_WIDTH, DISPLAY_WIDTH);
        intent.putExtra(SCREEN_HEIGHT, DISPLAY_HEIGHT);
        intent.setClass(this, RecordScreenService.class);
        startService(intent);
        mediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        densityDpi = getResources().getDisplayMetrics().densityDpi;
        startRecord = findViewById(R.id.startRecord);
        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupDisplay();
            }
        });
        doBindService();
    }

    private void setupDisplay() {

        Toast.makeText(getBaseContext(), R.string.start_recording, Toast.LENGTH_SHORT).show();
        if (projection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        }


    }


    private void doBindService() {
        Intent intent = new Intent(this, RecordScreenService.class);
        mConnection = new RecordServiceConnection();
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
    }

    private void doUnBindService() {
        if (mBound) {
            if (mService != null) {
                Message msg = Message.obtain(null, UNREGISTER_MESSENGER);
                msg.replyTo = mMessenger;
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "error", e);
                }
            }
            unbindService(mConnection);
            mBound = false;
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    public class RecordServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(LOG_TAG, "onServiceConnected");
            synchronized (connectLocker) {
                mService = new Messenger(iBinder);
                connectLocker.notify();
            }
            Message msg = Message.obtain(null, REGISTER_MESSENGER);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "error", e);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (connectLocker) {
                Log.d(LOG_TAG,"onServiceDisconnected");
                mService = null;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                projection = mediaProjectionManager.getMediaProjection(resultCode, data);
                projection.registerCallback(mMediaCallback, null);
                mVirtualDisplay = projection.createVirtualDisplay("virtualDisplay", DISPLAY_WIDTH, DISPLAY_HEIGHT, densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaSureface, null, null);
                try {
                    mService.send(Message.obtain(null, START_RECORD));
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "error", e);
                }
                Log.d(LOG_TAG, "start");
            }
        }
    }

    private void setSurface(Surface surface) {
        mediaSureface = surface;


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        doUnBindService();

    }

    MediaProjection.Callback mMediaCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            super.onStop();
            stopRecord();
        }
    };

    private void stopRecord() {
        Toast.makeText(getBaseContext(), R.string.stop_recording, Toast.LENGTH_SHORT).show();
        if (projection != null) {
            projection.unregisterCallback(mMediaCallback);
            projection.stop();

        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }

    }
}
