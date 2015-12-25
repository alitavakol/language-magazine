package me.ali.coolenglishmagazine;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import me.ali.coolenglishmagazine.util.LogHelper;


public class ReadAndListenActivity extends AppCompatActivity {

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogHelper.d(TAG, "onStart");

        bindToMusicService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogHelper.d(TAG, "onStop");

        unbindMusicService();
    }

    /**
     * connection to music service
     */
    private ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();

            LogHelper.d(TAG, "Bound to music service.");

            final String dataSource = "file:///mnt/sdcard/cool-english-magazine/swimming-squirrel.mp3";
            if (!dataSource.equals(musicService.getDataSource())) {
                musicService.stopPlayback();
            }
            boundToMusicService = true;
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
}
