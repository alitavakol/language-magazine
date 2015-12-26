package me.ali.coolenglishmagazine;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.session.PlaybackState;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.Duration;

import me.ali.coolenglishmagazine.util.LogHelper;


public class ReadAndListenActivity extends AppCompatActivity implements View.OnClickListener, MusicService.OnMediaStateChangedListener {

    private static final String TAG = LogHelper.makeLogTag(ReadAndListenActivity.class);

    private MusicService musicService;
    private boolean boundToMusicService = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_and_listen);

        // TODO read http://javarticles.com/2015/09/android-toolbar-example.html to add toolbar

        WebView webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///mnt/sdcard/cool-english-magazine/swimming-squirrel.html");

        ((ImageView) findViewById(R.id.play)).setOnClickListener(this);
        ((ImageView) findViewById(R.id.pause)).setOnClickListener(this);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        startText = (TextView) findViewById(R.id.startText);
        endText = (TextView) findViewById(R.id.endText);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogHelper.d(TAG, "onStart");

        bindToMusicService();
    }

    /**
     * this callback is guaranteed to get called before any kind of process killing. see
     * http://developer.android.com/intl/pt-br/reference/android/app/Activity.html#ActivityLifecycle
     */
    @Override
    protected void onStop() {
        super.onStop();
        LogHelper.d(TAG, "onStop");

        musicService.removeOnMediaStateChangedListener(ReadAndListenActivity.this);
        unbindMusicService();
    }

    /**
     * connection to music service
     */
    private ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MusicBinder) service).getService();
            LogHelper.d(TAG, "Bound to music service.");

            musicService.setOnMediaStateChangedListener(ReadAndListenActivity.this);
            boundToMusicService = true;

//            final String dataSource = "file:///mnt/sdcard/cool-english-magazine/swimming-squirrel.mp3";
//            if (!dataSource.equals(musicService.getDataSource())) {
//                musicService.stopPlayback();
//            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            boundToMusicService = false;
        }
    };

    void bindToMusicService() {
        if (!boundToMusicService) {
            bindService(new Intent(this, MusicService.class), musicServiceConnection, Context.BIND_AUTO_CREATE);
//            startService(playIntent);
        }
    }

    void unbindMusicService() {
        if (boundToMusicService) {
            // Detach our existing connection.
            unbindService(musicServiceConnection);
            boundToMusicService = false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play:
                Intent startIntent = new Intent(ReadAndListenActivity.this, MusicService.class);
                startIntent.setAction(MusicService.ACTION_PLAY);
                startIntent.putExtra("dataSource", "file:///mnt/sdcard/cool-english-magazine/swimming-squirrel.mp3");
                startService(startIntent);
                break;

            case R.id.pause:
                Intent pauseIntent = new Intent(ReadAndListenActivity.this, MusicService.class);
                pauseIntent.setAction(MusicService.ACTION_PAUSE);
                startService(pauseIntent);
                break;

            default:
                break;
        }
    }

    protected SeekBar seekBar = null;
    protected TextView startText = null, endText = null;
    protected Timer seekBarTimer = null;

    @Override
    public void onMediaStateChanged(int state) {
        LogHelper.i(TAG, "media playback state: ", state);

        if(musicService != null) {
            final int duration = musicService.getDuration();
            seekBar.setMax(duration);
            seekBar.setProgress(musicService.getCurrentMediaPosition());
            endText.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration),
                    TimeUnit.MILLISECONDS.toSeconds(duration) % TimeUnit.MINUTES.toSeconds(1)));
        }

        if (state == PlaybackState.STATE_PLAYING) {
            findViewById(R.id.play).setVisibility(View.GONE);
            findViewById(R.id.pause).setVisibility(View.VISIBLE);

            seekBarTimer = new Timer();
            seekBarTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int currentPosition = musicService.getCurrentMediaPosition();
                            seekBar.setProgress(currentPosition);
                            startText.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(currentPosition),
                                    TimeUnit.MILLISECONDS.toSeconds(currentPosition) % TimeUnit.MINUTES.toSeconds(1)));
                        }
                    });
                }
            }, 0, musicService.getDuration() / 100);

        } else if (state == PlaybackState.STATE_STOPPED) {
            findViewById(R.id.play).setVisibility(View.VISIBLE);
            findViewById(R.id.pause).setVisibility(View.GONE);

//            seekBar.setMax(0);
//            seekBar.setProgress(0);
            if (seekBarTimer != null) {
                seekBarTimer.cancel();
                seekBarTimer = null;
            }

        } else if (state == PlaybackState.STATE_PAUSED) {
            findViewById(R.id.play).setVisibility(View.VISIBLE);
            findViewById(R.id.pause).setVisibility(View.GONE);

//            seekBar.setMax(musicService.getDuration());
//            seekBar.setProgress(musicService.getCurrentMediaPosition());
            if (seekBarTimer != null) {
                seekBarTimer.cancel();
                seekBarTimer = null;
            }
        }
    }
}
