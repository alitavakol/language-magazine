package me.ali.coolenglishmagazine;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

import me.ali.coolenglishmagazine.util.LogHelper;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private static final String TAG = LogHelper.makeLogTag(MusicService.class);
    public static final int PLAYBACK_NOTIFICATION_ID = 100;

    public static final String ACTION_PLAY = "me.ali.coolenglishmagazine.ACTION_PLAY";

    /**
     * local broadcast intent action sent when media status changes
     */
    public static final String MUSIC_STATUS_CHANGED = "me.ali.coolenglishmagazine.music_status_changed";

    private MediaPlayer mediaPlayer = null;

    /**
     * current media player data source
     */
    private String dataSource = null;

    public void onCreate() {
    }

    public void onDestroy() {
        LogHelper.d("onDestroy");

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * Called when MediaPlayer is ready
     */
    public void onPrepared(MediaPlayer player) {
        LogHelper.i(TAG, "Media prepared.");

        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), ReadAndListenActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setAutoCancel(false);
        builder.setTicker("this is ticker text");
        builder.setContentTitle("WhatsApp Notification");
        builder.setContentText("You have a new message");
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentIntent(pi);
        builder.setOngoing(true);
        builder.setSubText("This is subtext...");

        startForeground(PLAYBACK_NOTIFICATION_ID, builder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // TODO implement audio becoming noisy handler

    /**
     * This is the object that receives interactions from clients.
     */
    private final IBinder binder = new MusicBinder();

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_PLAY)) {
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setWakeMode(getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);

            try {
                dataSource = intent.getStringExtra("dataSource");
                mediaPlayer.setDataSource(dataSource);
                mediaPlayer.prepareAsync(); // prepare async to not block main thread

            } catch (java.io.IOException e) {
                LogHelper.e(TAG, e.getMessage());
            }
        }

        // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void stopPlayback() {
        if (mediaPlayer != null) {
            stopForeground(true);

            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
