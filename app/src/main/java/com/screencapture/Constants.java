package com.screencapture;

/**
 * Created by wenchihhsieh on 2017/3/20.
 */

public interface Constants {
    int REGISTER_MESSENGER = 1;
    int UNREGISTER_MESSENGER = 2;
    int STOP_RECORD = 4;
    int SEND_TO_MESSENGER = 5;
    int START_RECORD = 7;
    int MSG_REGISTER_ACK_READY = 9;
    int MSG_REGISTER_ACK_NOT_READY = 10;
    String EXTRA_SURFACE = "EXTRA_SURFACE";
    String SCREEN_WIDTH = "SCREEN_WIDTH";
    String SCREEN_HEIGHT = "SCREEN_HEIGHT";
}
