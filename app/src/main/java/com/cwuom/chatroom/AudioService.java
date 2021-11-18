package com.cwuom.chatroom;

import android.app.IntentService;
import android.content.Intent;

import android.app.Service;

/**
 * 多线程实现后台播放背景音乐的service
 */
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

public class AudioService extends Service implements
        MediaPlayer.OnCompletionListener {
    // 实例化MediaPlayer对象
    MediaPlayer player;
    private final IBinder binder = new AudioBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void onCreate() {
        super.onCreate();
        // 从raw文件夹中获取一个应用自带的mp3文件
        player = MediaPlayer.create(this, R.raw.alert005);
        player.setOnCompletionListener(this);
        player.setLooping(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (!player.isPlaying()) {
            new MusicPlayThread().start();
        } else
            player.isPlaying();
        return START_STICKY;
    }

    /**
     * 当Audio播放完的时候触发该动作
     */
    public void onCompletion(MediaPlayer mp) {
        stopSelf();// 结束了，则结束Service

    }

    public void onDestroy() {
        super.onDestroy();
        if (player.isPlaying()) {
            player.stop();
        }
        player.release();
    }

    // 为了和Activity交互，我们需要定义一个Binder对象
    public class AudioBinder extends Binder {
        // 返回Service对象
        public AudioService getService() {
            return AudioService.this;
        }
    }

    private class MusicPlayThread extends Thread {
        public void run() {
            if (!player.isPlaying()) {
                player.start();
            }
        }
    }

}