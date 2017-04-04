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
import android.widget.Toast;

/**
 * Created by wenchihhsieh on 2017/3/20.
 */

public class RecordScreenActivity extends AppCompatActivity implements Constants {
    int REQUEST_CODE = 101;
    MediaProjectionManager mediaProjectionManager;
    MediaProjection projection;
    int densityDpi;
    int DISPLAY_WIDTH = 1080;
    int DISPLAY_HEIGHT = 720;
    boolean mBound = false;
    ServiceConnection mConnection;
    VirtualDisplay mVirtualDisplay;
    Surface mediaSureface;
    private static final String LOG_TAG = RecordScreenActivity.class.getSimpleName();
    Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "receive " + msg.what);
            switch (msg.what) {
                case SET_SURFACE:
                    Surface mediaSureface = (Surface) msg.obj;
                    startRecord(mediaSureface);
                    break;
                case STOP_RECORD:
                    stopRecord();
                    break;
            }
        }
    });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float factor = 0.5f;
        DISPLAY_WIDTH = (int) (getResources().getDisplayMetrics().widthPixels * factor);
        DISPLAY_HEIGHT = (int) (getResources().getDisplayMetrics().heightPixels * factor);
        Intent intent = new Intent();
        intent.putExtra(SCREEN_WIDTH, DISPLAY_WIDTH);
        intent.putExtra(SCREEN_HEIGHT, DISPLAY_HEIGHT);
        intent.setClass(this, RecordScreenService.class);
        startService(intent);
        mediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        densityDpi = getResources().getDisplayMetrics().densityDpi;

        doBindService();
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

    Messenger mService;

    @Override
    protected void onStop() {
        super.onStop();
    }

    public class RecordServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(LOG_TAG, "onServiceConnected");
            mService = new Messenger(iBinder);

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
            mService = null;
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
                    mService.send(Message.obtain(null, RECORD_READY));
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "error", e);
                }
                Log.d(LOG_TAG, "start");
            }
        }
    }

    private void startRecord(Surface surface) {
        Toast.makeText(getBaseContext(), R.string.start_recording, Toast.LENGTH_SHORT).show();
        mediaSureface = surface;
        if (projection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        }

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
