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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import me.ali.coolenglishmagazine.util.LogHelper;


public class ReadAndListenActivity extends AppCompatActivity implements View.OnClickListener, MusicService.OnMediaStateChangedListener, SeekBar.OnSeekBarChangeListener, View.OnLongClickListener {

    private static final String TAG = LogHelper.makeLogTag(ReadAndListenActivity.class);

    private MusicService musicService;
    private boolean boundToMusicService = false;

    /**
     * music playback state
     */
    protected int state = PlaybackState.STATE_NONE;

    protected final String transcriptFilePath = "/mnt/sdcard/cool-english-magazine/swimming-squirrel.html";

    /**
     * array of voice timestamps (start and end of voice snippets)
     */
    ArrayList<int[]> timePoints = null;
    protected int[] currentTimePoint = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_and_listen);

        // TODO read http://javarticles.com/2015/09/android-toolbar-example.html to add toolbar

        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file://" + transcriptFilePath);

        if(savedInstanceState != null) {
            if(!savedInstanceState.getBoolean("transciptLocked"))
                onLongClick(findViewById(R.id.lock));
        }

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        startText = (TextView) findViewById(R.id.startText);
        endText = (TextView) findViewById(R.id.endText);

        findViewById(R.id.play).setEnabled(false);
        findViewById(R.id.pause).setEnabled(false);
        findViewById(R.id.prev).setEnabled(false);
        findViewById(R.id.next).setEnabled(false);
        seekBar.setEnabled(false);

        findViewById(R.id.play).setOnClickListener(this);
        findViewById(R.id.pause).setOnClickListener(this);
        findViewById(R.id.prev).setOnClickListener(this);
        findViewById(R.id.next).setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        findViewById(R.id.lock).setOnClickListener(this);
        findViewById(R.id.lock).setOnLongClickListener(this);

        try {
            timePoints = getTimePoints(transcriptFilePath);

        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
        }
    }

    protected boolean transciptLocked = true;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("transciptLocked", transciptLocked);
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

            Intent startIntent = new Intent(ReadAndListenActivity.this, MusicService.class);
            startIntent.setAction(MusicService.ACTION_PREPARE);
            startIntent.putExtra("dataSource", "file:///mnt/sdcard/cool-english-magazine/swimming-squirrel.mp3");
            startService(startIntent);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
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
        }
        musicService = null;
        boundToMusicService = false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play:
                Intent startIntent = new Intent(ReadAndListenActivity.this, MusicService.class);
                startIntent.setAction(MusicService.ACTION_PLAY);
                startService(startIntent);
                break;

            case R.id.pause:
                Intent pauseIntent = new Intent(ReadAndListenActivity.this, MusicService.class);
                pauseIntent.setAction(MusicService.ACTION_PAUSE);
                startService(pauseIntent);
                break;

            case R.id.prev: {
                int currentPosition = musicService.getCurrentMediaPosition();
                int prevTimePoint = 0; // time point to seek to

                for (int i = timePoints.size() - 1; i >= 0; i--) {
                    final int[] timePoint = timePoints.get(i);
                    if (currentPosition - timePoint[0] > 1500) {
                        prevTimePoint = timePoint[0];
                        break;
                    }
                }

                musicService.seekTo(prevTimePoint);
                seekBar.setProgress(prevTimePoint);
                break;
            }

            case R.id.next: {
                int currentPosition = musicService.getCurrentMediaPosition();
                int nextTimePoint = musicService.getDuration(); // time point to seek to

                for (int[] timePoint : timePoints) {
                    if (timePoint[0] > currentPosition) {
                        nextTimePoint = timePoint[0];
                        break;
                    }
                }

                musicService.seekTo(nextTimePoint);
                seekBar.setProgress(nextTimePoint);
                break;
            }

            case R.id.lock:
                Toast.makeText(this, R.string.unlock_hint, Toast.LENGTH_SHORT).show();
                break;

            default:
                break;
        }
    }

    protected SeekBar seekBar = null;
    protected TextView startText = null, endText = null;
    protected Timer seekBarTimer = null;

    protected WebView webView = null;

    protected String formatTime(int time) {
        return String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(time),
                TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1));
    }

    @Override
    public void onMediaStateChanged(int state) {
        LogHelper.i(TAG, "media playback state: ", state);

        if (state != PlaybackState.STATE_NONE) {
            final int duration = musicService.getDuration();
            final int currentPosition = musicService.getCurrentMediaPosition();

            seekBar.setMax(duration);
            seekBar.setProgress(currentPosition);

            endText.setText(formatTime(duration));
        }

        findViewById(R.id.play).setEnabled(state != PlaybackState.STATE_NONE);
        findViewById(R.id.pause).setEnabled(state != PlaybackState.STATE_NONE);

        final boolean canSeek = state == PlaybackState.STATE_PAUSED || state == PlaybackState.STATE_PLAYING;
        findViewById(R.id.prev).setEnabled(canSeek);
        findViewById(R.id.next).setEnabled(canSeek);
        seekBar.setEnabled(canSeek);

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
                            if(musicService == null) {
                                seekBarTimer.cancel();
                                return;
                            }

                            if (!ignoreSeekBar)
                                seekBar.setProgress(musicService.getCurrentMediaPosition());
                        }
                    });
                }
            }, 0, 250);

        } else if (state == PlaybackState.STATE_STOPPED) {
            findViewById(R.id.play).setVisibility(View.VISIBLE);
            findViewById(R.id.pause).setVisibility(View.GONE);

            if (seekBarTimer != null) {
                seekBarTimer.cancel();
                seekBarTimer = null;
            }

        } else if (state == PlaybackState.STATE_PAUSED) {
            findViewById(R.id.play).setVisibility(View.VISIBLE);
            findViewById(R.id.pause).setVisibility(View.GONE);

            if (seekBarTimer != null) {
                seekBarTimer.cancel();
                seekBarTimer = null;
            }
        }

        this.state = state;
    }

    /**
     * if true, don't change seekBar position.
     */
    boolean ignoreSeekBar = false;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser)
            musicService.seekTo(progress);

        startText.setText(formatTime(progress));

        int[] currentTimePoint = null;
        for(int[] timePoint : timePoints) {
            if(progress >= timePoint[0] && progress <= timePoint[1]) {
                currentTimePoint = timePoint;
                break;
            }
        }
        if(currentTimePoint != this.currentTimePoint) {
            this.currentTimePoint = currentTimePoint;
            webView.loadUrl("javascript:$('.highlight').removeClass('highlight');");
            if(currentTimePoint != null)
                webView.loadUrl("javascript:$('[data-start=" + currentTimePoint[0] + "]').addClass('highlight');");
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        ignoreSeekBar = true;
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        ignoreSeekBar = false;
    }

    public static ArrayList<int[]> getTimePoints(String filePath) throws IOException {
        ArrayList<int[]> timePoints = new ArrayList<>();

        File input = new File(filePath);
        Document doc = Jsoup.parse(input, "UTF-8", "");

        Elements spans = doc.getElementsByAttribute("data-start");
        for (Element span : spans) {
            String start = span.attr("data-start");
            String end = span.attr("data-end");

            try {
                int[] timePoint = new int[2];
                timePoint[0] = Integer.valueOf(start);
                timePoint[1] = Integer.valueOf(end);
                timePoints.add(timePoint);

            } catch (java.lang.NumberFormatException e) {
                LogHelper.e(TAG, e.getMessage());
            }
        }

        return timePoints;
    }

    @Override
    public boolean onLongClick(View v) {
        switch(v.getId()) {
            case R.id.lock:
                v.setVisibility(View.GONE);
                webView.loadUrl("javascript:$('.blur').removeClass('blur');");
                transciptLocked = false;
                break;
        }
        return true;
    }

}
