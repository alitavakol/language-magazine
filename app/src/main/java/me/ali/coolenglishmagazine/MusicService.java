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
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.io.File;
import java.util.ArrayList;

import me.ali.coolenglishmagazine.broadcast_receivers.RemoteControlReceiver;
import me.ali.coolenglishmagazine.model.MagazineContent;
import me.ali.coolenglishmagazine.model.WaitingItems;
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

    /**
     * sent when user swipes notification to stop playback
     */
    public static final String ACTION_STOP_BY_USER = "me.ali.coolenglishmagazine.ACTION_STOP_BY_USER";

    public static final String ACTION_PREPARE = "me.ali.coolenglishmagazine.ACTION_PREPARE";
    public static final String ACTION_FAST_FORWARD = "me.ali.coolenglishmagazine.ACTION_FAST_FORWARD";
    public static final String ACTION_REWIND = "me.ali.coolenglishmagazine.ACTION_REWIND";

    private static final String TAG = LogHelper.makeLogTag(MusicService.class);

    /**
     * This is the object that receives interactions from clients.
     */
    private final IBinder binder = new MusicBinder();

    private MediaPlayer mediaPlayer = null;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat mediaController;

    private boolean paused = false;

    /**
     * current media player data source
     */
    private String dataSource = null;

    /**
     * duration in milliseconds reported by manifest.xml
     */
    private float duration;

    /**
     * duration reported by {@link MediaPlayer#getDuration()}
     */
    private float duration_;

    /**
     * helps us to keep playback notification visible unless media source changes,
     */
    private String previousDataSource;

    /**
     * if set to true, removes notification after playback completion.
     */
    private boolean removeNotification;

    public void onCreate() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AudioPlaybackWakelockTag");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    public void onDestroy() {
        LogHelper.d("music service onDestroy");

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

        if (mediaSession != null) {
            mediaController.getTransportControls().stop();
            mediaSession.release();
            mediaSession = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mediaController.getTransportControls().stop();

        // user has learnt this item. increment hit count if it is in the list of waiting items.
        WaitingItems.incrementHitCount(this, item);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    protected MagazineContent.Item item;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent != null ? intent.getAction() : null;

        if (mediaSession == null)
            initMediaSessions();

        if (ACTION_PREPARE.equals(action)) {
            handlePrepareRequest(intent.getStringExtra("dataSource"), intent.getIntExtra("duration", 0));

        } else if (ACTION_PLAY.equals(action)) {
            mediaController.getTransportControls().play();

        } else if (ACTION_PAUSE.equals(action)) {
            mediaController.getTransportControls().pause();

        } else if (ACTION_STOP.equals(action)) {
            mediaController.getTransportControls().stop();

        } else if (ACTION_STOP_BY_USER.equals(action)) {
            removeNotification = true;
            mediaController.getTransportControls().stop();
            disableShadeDetection();

        } else if (ACTION_FAST_FORWARD.equals(action)) {
            mediaController.getTransportControls().fastForward();

        } else if (ACTION_REWIND.equals(action)) {
            mediaController.getTransportControls().rewind();
        }

        return START_NOT_STICKY;
    }

    private void initMediaSessions() {
        mediaSession = new MediaSessionCompat(getApplicationContext(), "Hot English Magazine Player Session");

        try {
            mediaController = new MediaControllerCompat(getApplicationContext(), mediaSession.getSessionToken());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        mediaSession.setActive(true);

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
                                     @Override
                                     public void onPlay() {
                                         super.onPlay();
                                         LogHelper.i(TAG, "media session onPlay");

                                         handlePlayRequest();
                                     }

                                     @Override
                                     public void onPause() {
                                         super.onPause();
                                         LogHelper.i(TAG, "media session onPause");

                                         handlePauseRequest();
                                     }

                                     @Override
                                     public void onFastForward() {
                                         super.onFastForward();
                                         LogHelper.i(TAG, "media session onFastForward");

                                         if (mediaPlayer != null) {
                                             fastForward();
                                             if (onMediaStateChangedListener != null)
                                                 onMediaStateChangedListener.onMediaStateChanged(PlaybackStateCompat.STATE_FAST_FORWARDING);
                                         }
                                     }

                                     @Override
                                     public void onRewind() {
                                         super.onRewind();
                                         LogHelper.i(TAG, "media session onRewind");

                                         if (mediaPlayer != null) {
                                             rewind();
                                             if (onMediaStateChangedListener != null)
                                                 onMediaStateChangedListener.onMediaStateChanged(PlaybackStateCompat.STATE_REWINDING);
                                         }
                                     }

                                     @Override
                                     public void onStop() {
                                         super.onStop();
                                         LogHelper.i(TAG, "media session onStop");

                                         handleStopRequest();
                                     }

                                     @Override
                                     public void onSeekTo(long pos) {
                                         super.onSeekTo(pos);
                                     }

                                     @Override
                                     public void onSetRating(RatingCompat rating) {
                                         super.onSetRating(rating);
                                     }

//                                     public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
//                                         LogHelper.i(TAG, "onMediaButtonEvent called: " + mediaButtonIntent);
//                                         return false;
//                                     }
                                 }
        );
    }

    private Notification buildNotification(NotificationCompat.Action action) {
        // when user clicks on the notification, lesson activity should open.
        Intent notificationIntent = new Intent(this, ReadAndListenActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra(ReadAndListenActivity.ARG_ROOT_DIRECTORY, item.rootDirectory.getAbsolutePath());

        // http://stackoverflow.com/a/31445004
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 100, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.notification_large_icon))
                .setSmallIcon(R.drawable.sunglasses)
                .setColor(getResources().getColor(R.color.primary_dark))
                .setContentTitle(item.title)
                .setContentText(getResources().getString(R.string.app_name))
                .setDeleteIntent(PendingIntent.getService(this, 1, new Intent(getApplicationContext(), MusicService.class).setAction(ACTION_STOP_BY_USER), 0)) // stop playback when notification is cancelled.
                .setContentIntent(pendingIntent);

        android.support.v7.app.NotificationCompat.MediaStyle style = new android.support.v7.app.NotificationCompat.MediaStyle();
        builder.setStyle(style);

        builder.addAction(generateAction(android.R.drawable.ic_media_rew, "Rewind", ACTION_REWIND));
        builder.addAction(action);
        builder.addAction(generateAction(android.R.drawable.ic_media_ff, "Fast Forward", ACTION_FAST_FORWARD));
        style.setShowActionsInCompactView(0, 1, 2);

        final Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PLAYBACK_NOTIFICATION_ID, notification);
        enableShakeDetection();
        removeNotification = false;

        return notification;
    }

    private NotificationCompat.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), MusicService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    public int getDuration() {
        return mediaPlayer != null ? (int) duration : 0;
    }

    public int getCurrentMediaPosition() {
        return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
    }

    private void handlePrepareRequest(String dataSource, int duration) {
        if (mediaPlayer != null && !this.dataSource.equals(dataSource)) {
            previousDataSource = this.dataSource;

            this.dataSource = dataSource;
            this.duration = duration;
            mediaController.getTransportControls().stop();

            // handleStopRequest will call this function later, so do nothing at the moment.
            return;
        }

        if (dataSource == null || !dataSource.equals(previousDataSource)) { // remove notification if data source has changed.
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(PLAYBACK_NOTIFICATION_ID);
            disableShadeDetection();
            previousDataSource = dataSource;
        }

        if (dataSource != null && mediaPlayer == null && dataSource.length() > 0) {
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setWakeMode(getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);

            try {
                this.dataSource = dataSource;
                this.duration = duration;

                mediaPlayer.setDataSource(dataSource);
                mediaPlayer.prepareAsync(); // prepare async to not block main thread

                item = MagazineContent.getItem(new File(dataSource).getParentFile());

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

        if (onMediaStateChangedListener != null) {
            if (duration == 0)
                duration = mediaPlayer.getDuration();
            duration_ = mediaPlayer.getDuration();
            onMediaStateChangedListener.onMediaStateChanged(PlaybackStateCompat.STATE_STOPPED);
        }
    }

    AudioManager audioManager;
    private ComponentName mediaButtonReceiverComponent;

    private void handlePlayRequest() {
        if (mediaPlayer != null) {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mediaButtonReceiverComponent = new ComponentName(this.getPackageName(), RemoteControlReceiver.class.getName());

            // Request audio focus for playback
            int requestAudioFocusResult = audioManager.requestAudioFocus(this,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN);

            if (requestAudioFocusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioManager.registerMediaButtonEventReceiver(mediaButtonReceiverComponent);

                if (paused) // rewind to start of audio snippet
                    seekTo(floorPosition(getCurrentMediaPosition(), 0, false));

                mediaPlayer.start();
                paused = false;

                noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();
                registerReceiver(noisyAudioStreamReceiver, intentFilter);

//                startForeground(PLAYBACK_NOTIFICATION_ID,
//                        buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)));
                buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));

                if (onMediaStateChangedListener != null)
                    onMediaStateChangedListener.onMediaStateChanged(PlaybackStateCompat.STATE_PLAYING);

                // prevent maximum volume
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Math.min(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) - 1), 0);

                mSettingsContentObserver = new SettingsContentObserver(new Handler());
                getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver);

//                ShakeDetector.create(this, new ShakeDetector.OnShakeListener() {
//                    @Override
//                    public void OnShake() {
//                        boolean shakeToPausePlayback = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(MusicService.this).getString("shake_behaviour", "0")) == 1;
//                        if (shakeToPausePlayback) {
//                            if (mediaPlayer != null && mediaPlayer.isPlaying())
//                                mediaController.getTransportControls().pause();
//                            else
//                                mediaController.getTransportControls().play();
//                        }
//                    }
//                });
//                wakeLock.acquire();
//                ShakeDetector.start();
            }
        }
    }

//    private VolumeProviderCompat myVolumeProvider = null;

    SettingsContentObserver mSettingsContentObserver = null;

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // Pause playback
            mediaController.getTransportControls().pause();

        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Resume playback
            mediaController.getTransportControls().play();

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            // Stop playback
            mediaController.getTransportControls().stop();
        }
    }

    public void handleStopRequest() {
        if (mediaPlayer != null) {
            stopForeground(false);

            if (!removeNotification) // keep notification shown unless it is swiped out by user.
                buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));

            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            paused = false;

            if (mSettingsContentObserver != null) {
                getContentResolver().unregisterContentObserver(mSettingsContentObserver);
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

//            ShakeDetector.stop();
//            ShakeDetector.destroy();
//            if (wakeLock.isHeld())
//                wakeLock.release();
        }

        if (onMediaStateChangedListener != null)
            onMediaStateChangedListener.onMediaStateChanged(PlaybackStateCompat.STATE_STOPPED);

        handlePrepareRequest(dataSource, (int) duration);
    }

    public void handlePauseRequest() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            paused = true;

            if (mSettingsContentObserver != null) {
                getContentResolver().unregisterContentObserver(mSettingsContentObserver);
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

//            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            notificationManager.notify(PLAYBACK_NOTIFICATION_ID, getNotification(false));

            if (onMediaStateChangedListener != null)
                onMediaStateChangedListener.onMediaStateChanged(PlaybackStateCompat.STATE_PAUSED);

            buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
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
        onMediaStateChangedListener.onMediaStateChanged(mediaPlayer == null ? PlaybackStateCompat.STATE_NONE : (mediaPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : (paused ? PlaybackStateCompat.STATE_PAUSED : PlaybackStateCompat.STATE_STOPPED)));
    }

    public void removeOnMediaStateChangedListener(OnMediaStateChangedListener listener) {
        if (listener == onMediaStateChangedListener)
            onMediaStateChangedListener = null;
    }

    public void seekTo(int position) {
        try {
            if (mediaPlayer != null && (mediaPlayer.isPlaying() || paused))
                mediaPlayer.seekTo((int) (position * duration_ / duration) + 10); // workaround: sometimes it seeks to previous time span with 1 ms error

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
                mediaController.getTransportControls().pause();
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

        long lastVolumeChangeTime;

        public SettingsContentObserver(Handler handler) {
            super(handler);

            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
//            if (!uri.toString().contains("volume_music_last_audible_speaker"))
//                return;

            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            LogHelper.i(TAG, uri.toString());

            long currentVolumeChangeTime = System.currentTimeMillis();
            if (ignore || currentVolumeChangeTime - lastVolumeChangeTime < 120) {
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
                if (previousVolume > currentVolume) {
                    rewind();
                    lastVolumeChangeTime = currentVolumeChangeTime;
                } else if (previousVolume < currentVolume) {
                    fastForward();
                    lastVolumeChangeTime = currentVolumeChangeTime;
                }

                // undo volume change
                if (currentVolume != previousVolume) {
                    ignore = true; // ignore next volume change which is originated from code
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);
                }

            } else if (currentVolume != previousVolume) {
                // prevent maximum volume
                previousVolume = Math.min(currentVolume, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) - 1);
                ignore = true;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);
            }
        }
    }

    /**
     * prevents device from sleeping, so we can detect shake.
     */
    PowerManager.WakeLock wakeLock;

    private SensorManager sensorManager;

    /**
     * acceleration apart from gravity
     */
    private float accel;

    /**
     * current acceleration including gravity
     */
    private float accelCurrent;

    /**
     * last acceleration including gravity
     */
    private float accelLast;

    /**
     * last time device was shaken
     */
    private long lastShakeTime;

//    /**
//     * helps ignore heavy shakes if device was not calm before
//     */
//    boolean ignoreNextShake;

    /**
     * counts number of consecutively intensive shakes
     */
    int shakeCount;

//    int sumAccelSign;

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            accelLast = accelCurrent;
            accelCurrent = x * x + y * y + z * z;
            float delta = accelCurrent - accelLast;
            accel = .75f * accel + .25f * Math.abs(delta); // perform low-cut filter

            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            boolean isInteractive;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                isInteractive = pm.isInteractive();
            } else {
                isInteractive = pm.isScreenOn();
            }
            if (isInteractive)
                return;

//            android.util.Log.i(TAG, "" + accel);

            if (accel > 325) {
                shakeCount++;
                if (shakeCount < 3)
                    return;
                shakeCount = 0;

//                // device should accelerate positively and negatively to be counted as shake
//                sumAccelSign += Math.signum(accel);
//                if(sumAccelSign == shakeCount || sumAccelSign == -shakeCount)
//                    return;
//                sumAccelSign = 0;

                long shakeTime = System.currentTimeMillis();
                if (shakeTime - lastShakeTime < 2000)
                    return;
                lastShakeTime = shakeTime;

//                if (ignoreNextShake)
//                    return;
//                ignoreNextShake = true;

                if (mediaPlayer != null && mediaPlayer.isPlaying())
                    mediaController.getTransportControls().pause();
                else
                    mediaController.getTransportControls().play();

//            } else if (accel < 50) {
//                ignoreNextShake = false;
//                shakeCount = 0;

            } else {
                shakeCount = 0;
//                sumAccelSign = 0;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    /**
     * enables shake to pause/resume playback. it is recommended to enable it if and only if playback
     * notification is present in the status bar.
     */
    protected void enableShakeDetection() {
        boolean shakeToPausePlayback = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(MusicService.this).getString("shake_behaviour", "0")) == 1;
        if (!shakeToPausePlayback) {
            disableShadeDetection();

        } else if (!wakeLock.isHeld()) {
            final Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                if (sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)) {
                    accel = 0;
                    accelCurrent = SensorManager.GRAVITY_EARTH;
                    accelLast = SensorManager.GRAVITY_EARTH;
                    wakeLock.acquire();
                }
            }
        }
    }

    /**
     * disables shake to pause/resume playback.
     */
    public void disableShadeDetection() {
        if (wakeLock.isHeld()) {
            sensorManager.unregisterListener(sensorEventListener);
            wakeLock.release();
        }
    }

    /**
     * enables/disables shake to pause/resume playback if preference changes while playing
     */
    protected SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("shake_behaviour") && mediaPlayer != null && (mediaPlayer.isPlaying() || paused))
                enableShakeDetection();
        }
    };

}
