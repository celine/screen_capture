package com.screencapture;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by wenchihhsieh on 2017/3/20.
 */

public class RecordScreenService extends Service implements View.OnClickListener, Constants {
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    List<Messenger> messengerList = new ArrayList<>();
    private static final String LOG_TAG = RecordScreenService.class.getSimpleName();
    MediaRecorder mMediaRecorder;
    int DISPLAY_WIDTH = 1080;
    int DISPLAY_HEIGHT = 1920;
    Handler uiHandler;
    int viewState = VIEW_STATE_NOT_READY;
    private static final int VIEW_STATE_NOT_READY = 0;
    private static final int VIEW_STATE_READY = 1;
    private static final int VIEW_STATE_START = 2;
    String videoPath;
    Button mView;
    WindowManager wm;

    public void setViewState(int state) {
        if (mView.getVisibility() != View.VISIBLE) {
            mView.setVisibility(View.VISIBLE);
        }
        if (state == viewState) {
            return;
        }
        viewState = state;

        switch (state) {
            case VIEW_STATE_NOT_READY:
                mView.setText(R.string.get_record_screen);
                break;
            case VIEW_STATE_READY:
                mView.setText(R.string.start_recording);
                break;
            case VIEW_STATE_START:
                mView.setText(R.string.stop_recording);
                break;
        }


    }

    public class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "msg " + msg.what);
            switch (msg.what) {
                case REGISTER_MESSENGER:
                    Log.d(LOG_TAG, "register");
                    messengerList.add(msg.replyTo);
                    if (viewState == VIEW_STATE_NOT_READY) {
                        setViewState(VIEW_STATE_READY);
                    }
                    break;
                case UNREGISTER_MESSENGER:
                    Log.d(LOG_TAG, "unregister");

                    messengerList.remove(msg.replyTo);
                    if (messengerList.size() == 0 && viewState == VIEW_STATE_READY) {
                        setViewState(VIEW_STATE_NOT_READY);
                    }
                    break;
                case RECORD_READY:
                    Log.d(LOG_TAG, "start recording");
                    mMediaRecorder.start();
                    break;
                case SEND_TO_MESSENGER:
                    Log.d(LOG_TAG, "messenger size " + messengerList.size());

                    for (Messenger messenger : messengerList) {
                        try {
                            messenger.send(Message.obtain(null, msg.arg1, msg.obj));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.setTitle("Load Average");
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mView = new Button(getBaseContext());
        setViewState(VIEW_STATE_NOT_READY);
        mView.setOnTouchListener(new ViewTouchListener(getBaseContext(), wm, params));
        mView.setOnClickListener(this);
        wm.addView(mView, params);
        uiHandler = new Handler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wm.removeView(mView);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        DISPLAY_WIDTH = intent.getIntExtra(SCREEN_WIDTH, DISPLAY_WIDTH);
        DISPLAY_HEIGHT = intent.getIntExtra(SCREEN_HEIGHT, DISPLAY_HEIGHT);
        Log.d(LOG_TAG, "width " + DISPLAY_WIDTH + " height " + DISPLAY_HEIGHT);
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onClick(View view) {
        try {
            switch (viewState) {
                case VIEW_STATE_NOT_READY:
                    Intent intent = new Intent(getApplicationContext(), RecordScreenActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return;
                case VIEW_STATE_READY:
                    videoPath = initRecorder();
                    setViewState(VIEW_STATE_START);
                    break;
                case VIEW_STATE_START:
                    mMessenger.send(Message.obtain(null, SEND_TO_MESSENGER, STOP_RECORD, 0, null));
                    stopRecord();
                    mView.setVisibility(View.GONE);
                    if (videoPath != null) {
                        launchPlayer(videoPath);
                    } else {
                        Toast.makeText(getBaseContext(), R.string.file_not_found, Toast.LENGTH_SHORT).show();
                    }
                    stopSelf();
                    break;

            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private String initRecorder() {
        String videoPath = null;
        try {
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            videoPath = Environment
                    .getExternalStoragePublicDirectory(Environment
                            .DIRECTORY_DOWNLOADS) + String.format("/%s_video.mp4", new SimpleDateFormat("yyMMddHHmmssZ").format(new Date()));
            mMediaRecorder.setOutputFile(videoPath);
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(30);
            //int rotation = wm.getDefaultDisplay().getRotation();
            int orientation = 0;
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
            try {
                mMessenger.send(Message.obtain(null, SEND_TO_MESSENGER, SET_SURFACE, 0, mMediaRecorder.getSurface()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return videoPath;
    }

    private void stopRecord() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
    }

    private void launchPlayer(String videoPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoPath));
        intent.setDataAndType(Uri.parse(videoPath), "video/mp4");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Log.d(LOG_TAG, "start video " + videoPath);
        startActivity(intent);
    }

}
