package me.ali.coolenglishmagazine;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;

import java.io.File;
import java.util.ArrayList;

import me.ali.coolenglishmagazine.broadcast_receivers.RemoteControlReceiver;
import me.ali.coolenglishmagazine.util.LogHelper;

/**
 * Web references:
 * foreground service: http://www.truiton.com/2014/10/android-foreground-service-example/
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    public static final int PLAYBACK_NOTIFICATION_ID = 100;

    public static final String ACTION_PLAY = "me.ali.coolenglishmagazine.ACTION_PLAY";
    public static final String ACTION_PAUSE = "me.ali.coolenglishmagazine.ACTION_PAUSE";
    public static final String ACTION_STOP = "me.ali.coolenglishmagazine.ACTION_STOP";
    public static final String ACTION_PREPARE = "me.ali.coolenglishmagazine.ACTION_PREPARE";
    public static final String ACTION_FAST_FORWARD = "me.ali.coolenglishmagazine.ACTION_FAST_FORWARD";
    public static final String ACTION_REWIND = "me.ali.coolenglishmagazine.ACTION_REWIND";

    private static final String TAG = LogHelper.makeLogTag(MusicService.class);

    /**
     * This is the object that receives interactions from clients.
     */
    private final IBinder binder = new MusicBinder();

    private MediaPlayer mediaPlayer = null;
    private boolean paused = false;

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
        final String action = intent != null ? intent.getAction() : null;

        if (ACTION_PREPARE.equals(action)) {
            handlePrepareRequest(intent.getStringExtra("dataSource"));

        } else if (ACTION_PLAY.equals(action)) {
            LogHelper.i(TAG, "Received start foreground intent ");

            handlePlayRequest();

        } else if (ACTION_PAUSE.equals(action)) {
            LogHelper.i(TAG, "Received pause foreground intent ");

            handlePauseRequest();

        } else if (ACTION_STOP.equals(action)) {
            handleStopRequest();

        } else if (ACTION_FAST_FORWARD.equals(action)) {
            if (mediaPlayer != null) {
                fastForward();
            }

        } else if (ACTION_REWIND.equals(action)) {
            if (mediaPlayer != null) {
                rewind();
            }
        }

        return START_NOT_STICKY;
    }

    private Notification getNotification(boolean isPlaying) {
        Intent notificationIntent = new Intent(this, ReadAndListenActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final String itemRootDirectory = new File(dataSource).getParent();
        notificationIntent.putExtra(ReadAndListenActivity.ARG_ROOT_DIRECTORY, itemRootDirectory);

        // http://stackoverflow.com/a/31445004
        // killed me :(
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 100, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        // add the back stack
//        stackBuilder.addParentStack(ReadAndListenActivity.class);
//        // add the Intent to the top of the stack
//        stackBuilder.addNextIntent(notificationIntent);
//        // get a PendingIntent containing the entire back stack
//        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

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

    public int getDuration() {
        return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
    }

    public int getCurrentMediaPosition() {
        return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
    }

    private void handlePrepareRequest(String dataSource) {
        if (mediaPlayer != null && !this.dataSource.equals(dataSource)) {
            this.dataSource = dataSource;
            handleStopRequest();
        }

        if (mediaPlayer == null && dataSource != null && dataSource.length() > 0) {
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

    /**
     * Called when MediaPlayer is ready
     */
    public void onPrepared(MediaPlayer player) {
        LogHelper.i(TAG, "Media prepared.");

        if (onMediaStateChangedListener != null)
            onMediaStateChangedListener.onMediaStateChanged(PlaybackState.STATE_STOPPED);
    }

    AudioManager audioManager;
    private ComponentName mediaButtonReceiverComponent;

    private void handlePlayRequest() {
        if (mediaPlayer != null) {
            audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            mediaButtonReceiverComponent = new ComponentName(this.getPackageName(), RemoteControlReceiver.class.getName());

            // Request audio focus for playback
            int requestAudioFocusResult = audioManager.requestAudioFocus(this,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN);

            if (requestAudioFocusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioManager.registerMediaButtonEventReceiver(mediaButtonReceiverComponent);

                mediaPlayer.start();
                paused = false;

                noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();
                registerReceiver(noisyAudioStreamReceiver, intentFilter);

                startForeground(PLAYBACK_NOTIFICATION_ID, getNotification(true));

                if (onMediaStateChangedListener != null)
                    onMediaStateChangedListener.onMediaStateChanged(PlaybackState.STATE_PLAYING);

//                int volumeControlsBehaviour = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("volume_controls_behaviour", "1"));
//                if (volumeControlsBehaviour != 0) {
                mSettingsContentObserver = new SettingsContentObserver(new Handler());
                getApplicationContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver);
//                }
            }
        }
    }

    SettingsContentObserver mSettingsContentObserver = null;

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // Pause playback
            handlePauseRequest();

        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Resume playback
            handlePlayRequest();

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            // Stop playback
            handleStopRequest();
        }
    }

    public void handleStopRequest() {
        if (mediaPlayer != null) {
            stopForeground(true);

            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            paused = false;

            if (mSettingsContentObserver != null) {
                getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
                mSettingsContentObserver = null;
            }

            if (audioManager != null) {
                audioManager.abandonAudioFocus(this);
                audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiverComponent);
            }

            if (noisyAudioStreamReceiver != null) {
                unregisterReceiver(noisyAudioStreamReceiver);
                noisyAudioStreamReceiver = null;
            }
        }

        if (onMediaStateChangedListener != null)
            onMediaStateChangedListener.onMediaStateChanged(PlaybackState.STATE_STOPPED);

        handlePrepareRequest(dataSource);
    }

    public void handlePauseRequest() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            paused = true;

            if (mSettingsContentObserver != null) {
                getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
                mSettingsContentObserver = null;
            }

            if (audioManager != null) {
                audioManager.abandonAudioFocus(this);
                audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiverComponent);
            }

            if (noisyAudioStreamReceiver != null) {
                unregisterReceiver(noisyAudioStreamReceiver);
                noisyAudioStreamReceiver = null;
            }

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(PLAYBACK_NOTIFICATION_ID, getNotification(false));

            if (onMediaStateChangedListener != null)
                onMediaStateChangedListener.onMediaStateChanged(PlaybackState.STATE_PAUSED);
        }
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
        onMediaStateChangedListener.onMediaStateChanged(mediaPlayer == null ? PlaybackState.STATE_NONE : (mediaPlayer.isPlaying() ? PlaybackState.STATE_PLAYING : (paused ? PlaybackState.STATE_PAUSED : PlaybackState.STATE_STOPPED)));
    }

    public void removeOnMediaStateChangedListener(OnMediaStateChangedListener listener) {
        if (listener == onMediaStateChangedListener)
            onMediaStateChangedListener = null;
    }

    public void seekTo(int position) {
        try {
            if (mediaPlayer != null && (mediaPlayer.isPlaying() || paused))
                mediaPlayer.seekTo(position);

        } catch (IllegalStateException e) {
            LogHelper.e(TAG, e.getMessage());
        }
    }

    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private NoisyAudioStreamReceiver noisyAudioStreamReceiver;

    private class NoisyAudioStreamReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                handlePauseRequest();
            }
        }
    }


    /**
     * array of voice timestamps (start and end of voice snippets)
     */
    protected ArrayList<int[]> timePoints = null;

    /**
     * mathematical floor
     *
     * @param currentPosition apply floor on this time position
     * @param margin          returned value is behind current position by at least this margin
     * @param closed          if true, end of audio snippet is considered too
     * @return music position of the latest sound snippet behind current position
     */
    public int floorPosition(int currentPosition, int margin, boolean closed) {
        int prevTimePoint = 0;

        for (int i = timePoints.size() - 1; i >= 0; i--) {
            final int[] timePoint = timePoints.get(i);
            if (currentPosition - timePoint[0] >= margin && (!closed || currentPosition <= timePoint[1])) {
                prevTimePoint = timePoint[0];
                break;
            }
        }

        return prevTimePoint;
    }

    /**
     * mathematical ceil
     *
     * @param currentPosition apply ceil on this time position
     * @return music position of the first sound snippet after current position
     */
    protected int ceilPosition(int currentPosition) {
        int nextTimePoint = getDuration();

        for (int[] timePoint : timePoints) {
            if (timePoint[0] > currentPosition) {
                nextTimePoint = timePoint[0];
                break;
            }
        }

        return nextTimePoint;
    }

    public void setTimePoints(ArrayList<int[]> timePoints) {
        this.timePoints = timePoints;
    }

    /**
     * fast forward to start of next time phrase.
     *
     * @return new media position
     */
    public int fastForward() {
        int nextTimePoint = ceilPosition(getCurrentMediaPosition()); // time point to seek to
        if (nextTimePoint < getDuration()) {
            seekTo(nextTimePoint);
        }
        return nextTimePoint;
    }

    /**
     * rewind to start of previous time phrase.
     *
     * @return new media position
     */
    public int rewind() {
        int prevTimePoint = floorPosition(getCurrentMediaPosition(), 2000, false); // time point to seek to
        seekTo(prevTimePoint);
        return prevTimePoint;
    }

    /**
     * shows whether or not the lesson activity containing media control buttons is in the foreground.
     */
    public static boolean readAndListenActivityResumed;

    /**
     * this class listens for device volume buttons.
     * taken from <a href="http://stackoverflow.com/a/15292255">this link</a>.
     */
    public class SettingsContentObserver extends ContentObserver {
        int previousVolume;

        /**
         * if true, do not handle volume change, which is originated from code.
         */
        boolean ignore;

        public SettingsContentObserver(Handler handler) {
            super(handler);

            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

            if (ignore) {
                ignore = false;
                return;
            }

            boolean useVolumeButtonsAsPlaybackNavigation = false;

            int volumeControlsBehaviour = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(MusicService.this).getString("volume_controls_behaviour", "1"));
            switch (volumeControlsBehaviour) {
                case 1: // volume controls are used to navigate playback when screen is off
                    // http://stackoverflow.com/a/34651569
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                        useVolumeButtonsAsPlaybackNavigation = !pm.isInteractive();
                    } else {
                        useVolumeButtonsAsPlaybackNavigation = !pm.isScreenOn();
                    }
                    break;

                case 2: // volume controls are used to navigate playback when lesson activity is not in resumed state
                    useVolumeButtonsAsPlaybackNavigation = !MusicService.readAndListenActivityResumed;
                    break;
            }

            if (useVolumeButtonsAsPlaybackNavigation) {
                if (previousVolume > currentVolume)
                    rewind();
                else
                    fastForward();

                ignore = true; // ignore next volume change which is originated from code

                // undo volume change
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);

            } else {
                // reset fixed volume
                previousVolume = currentVolume;
            }
        }
    }

}
