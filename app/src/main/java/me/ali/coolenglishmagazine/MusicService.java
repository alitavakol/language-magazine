package me.ali.coolenglishmagazine;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;

import me.ali.coolenglishmagazine.util.LogHelper;

/**
 * Web references:
 * foreground service: http://www.truiton.com/2014/10/android-foreground-service-example/
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    public static final int PLAYBACK_NOTIFICATION_ID = 100;

    public static final String ACTION_PLAY = "me.ali.coolenglishmagazine.ACTION_PLAY";
    public static final String ACTION_PAUSE = "me.ali.coolenglishmagazine.ACTION_PAUSE";
    public static final String ACTION_STOP = "me.ali.coolenglishmagazine.ACTION_STOP";
    public static final String ACTION_PREPARE = "me.ali.coolenglishmagazine.ACTION_PREPARE";

    private static final String TAG = LogHelper.makeLogTag(MusicService.class);

    /**
     * This is the object that receives interactions from clients.
     */
    private final IBinder binder = new MusicBinder();

    private MediaPlayer mediaPlayer = null;

    /**
     * current media player data source
     */
    private String dataSource = null;

    public void onCreate() {
    }

    public void onDestroy() {
        LogHelper.d("onDestroy");

        handleStopRequest();
    }

    // TODO implement audio becoming noisy handler

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        handleStopRequest();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction().equals(ACTION_PREPARE)) {
            handlePrepareRequest(intent.getStringExtra("dataSource"));

        } else if (intent.getAction().equals(ACTION_PLAY)) {
            LogHelper.i(TAG, "Received start foreground intent ");

            handlePlayRequest();

        } else if (intent.getAction().equals("PREV_ACTION")) {
            LogHelper.i(TAG, "Clicked Previous");

        } else if (intent.getAction().equals(ACTION_PAUSE)) {
            LogHelper.i(TAG, "Received pause foreground intent ");

            handlePauseRequest();

        } else if (intent.getAction().equals("NEXT_ACTION")) {
            LogHelper.i(TAG, "Clicked Next");

        } else if (intent.getAction().equals(ACTION_STOP)) {
            LogHelper.i(TAG, "Received stop foreground intent");

            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    private Notification getNotification(boolean isPlaying) {
        Intent notificationIntent = new Intent(this, ReadAndListenActivity.class);
        notificationIntent.setAction("MAIN_ACTION");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent previousIntent = new Intent(this, MusicService.class);
        previousIntent.setAction("PREV_ACTION");
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 0, previousIntent, 0);

        Intent playIntent = new Intent(this, MusicService.class);
        playIntent.setAction(isPlaying ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, 0);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction("NEXT_ACTION");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, 0);

        // Using RemoteViews to bind custom layouts into Notification
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.playback_notification);
        remoteViews.setTextViewText(R.id.textViewTitle, isPlaying ? "playing..." : "paused");

        // TODO design a more sophisticated notification layout with close (X) on top-right corner and media control buttons
        // see http://stackoverflow.com/a/18558404 for details

        return new NotificationCompat.Builder(this)
                .setContentTitle("title")
                .setTicker("ticker")
                .setContent(remoteViews)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher), 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
//                .addAction(android.R.drawable.ic_media_previous, "", previousPendingIntent)
//                .addAction(R.id.play_pause, "", playPendingIntent)
//                .addAction(android.R.drawable.ic_media_next, "", nextPendingIntent)
                .build();
    }

    public String getDataSource() {
        return dataSource;
    }

    public int getDuration() {
        return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
    }

    public int getCurrentMediaPosition() {
        return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
    }

    /**
     * Called when MediaPlayer is ready
     */
    public void onPrepared(MediaPlayer player) {
        LogHelper.i(TAG, "Media prepared.");

        if (onMediaStateChangedListener != null)
            onMediaStateChangedListener.onMediaStateChanged(PlaybackState.STATE_STOPPED);
    }

    private void handlePrepareRequest(String dataSource) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setWakeMode(getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);

            try {
                this.dataSource = dataSource;
                mediaPlayer.setDataSource(dataSource);
                mediaPlayer.prepareAsync(); // prepare async to not block main thread

            } catch (java.io.IOException e) {
                LogHelper.e(TAG, e.getMessage());
            }
        }
    }

    private void handlePlayRequest() {
        mediaPlayer.start();

        if (onMediaStateChangedListener != null)
            onMediaStateChangedListener.onMediaStateChanged(PlaybackState.STATE_PLAYING);
    }

    public void handleStopRequest() {
        if (mediaPlayer != null) {
            stopForeground(true);

            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (onMediaStateChangedListener != null)
            onMediaStateChangedListener.onMediaStateChanged(PlaybackState.STATE_STOPPED);
    }

    public void handlePauseRequest() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(PLAYBACK_NOTIFICATION_ID, getNotification(false));
        }

        if (onMediaStateChangedListener != null)
            onMediaStateChangedListener.onMediaStateChanged(PlaybackState.STATE_PAUSED);
    }

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

    public interface OnMediaStateChangedListener {
        void onMediaStateChanged(int state);
    }

    protected OnMediaStateChangedListener onMediaStateChangedListener = null;

    public void setOnMediaStateChangedListener(OnMediaStateChangedListener listener) {
        onMediaStateChangedListener = listener;
        onMediaStateChangedListener.onMediaStateChanged(mediaPlayer == null ? PlaybackState.STATE_STOPPED : (mediaPlayer.isPlaying()? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED));
    }

    public void removeOnMediaStateChangedListener(OnMediaStateChangedListener listener) {
        if (listener == onMediaStateChangedListener)
            onMediaStateChangedListener = null;
    }
}
