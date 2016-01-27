package me.ali.coolenglishmagazine;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.media.session.PlaybackState;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import me.ali.coolenglishmagazine.model.MagazineContent;
import me.ali.coolenglishmagazine.util.LogHelper;


public class ReadAndListenActivity extends AppCompatActivity implements View.OnClickListener, MusicService.OnMediaStateChangedListener, SeekBar.OnSeekBarChangeListener, View.OnLongClickListener {

    private static final String TAG = LogHelper.makeLogTag(ReadAndListenActivity.class);

    /**
     * The activity argument representing the item ID that this activity
     * represents.
     */
    public static final String ARG_ROOT_DIRECTORY = "item_root_directory";

    /**
     * current lesson item descriptor
     */
    protected MagazineContent.Item item;

    private MusicService musicService;
    private boolean boundToMusicService = false;

    /**
     * music playback state
     */
    protected int state = PlaybackState.STATE_NONE;

    /**
     * array of voice timestamps (start and end of voice snippets)
     */
    protected ArrayList<int[]> timePoints = null;

    /**
     * start time of currently playing audio snippet
     */
    protected int currentTimePoint = -1;

    public static class NewWord {
        Map<String, String> definition;
        String type;
    }

    /**
     * new words extracted from audio transcript HTML file
     */
    protected HashMap<String, NewWord> newWords;

    WebViewJavaScriptInterface webViewJavaScriptInterface = new WebViewJavaScriptInterface();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_and_listen);

        if (savedInstanceState != null) {
            transcriptLocked = savedInstanceState.getBoolean("transcriptLocked");
            useLockControls = savedInstanceState.getBoolean("useLockControls");
            webViewState = savedInstanceState.getStringArray("webViewState");
        }

        // TODO read http://javarticles.com/2015/09/android-toolbar-example.html to add toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowTitleEnabled(false); // hide action bar title
            ab.setDisplayHomeAsUpEnabled(true); // Enable the Up button
        }

        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                // show html content below action bar, keeping background untouched
                int[] actionBarSizeAttr = new int[]{android.R.attr.actionBarSize};
                int indexOfAttrActionBarSize = 0;
                TypedArray a = obtainStyledAttributes(new TypedValue().data, actionBarSizeAttr);
                int actionBarSize = a.getDimensionPixelSize(indexOfAttrActionBarSize, -1);
                a.recycle();

                final String command = "javascript:adjustLayout("
                        + actionBarSize // HTML content top margin
                        + ", " + findViewById(R.id.controllers).getMeasuredHeight()
                        + ", " + webView.getMeasuredHeight() // poster height
                        + ", " + ContextCompat.getColor(getApplicationContext(), R.color.colorAccent) // accent color
                        + ", 0xc5c5c5, " // text color
                        + ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary) // background color
                        + ", 0xf8f8f8" // new word color
                        + ");";
                webView.loadUrl(command);

                webView.loadUrl("javascript:setInstanceState(" + new JSONArray(Arrays.asList(webViewState)) + ");");

                lockTranscript(transcriptLocked);

                webView.loadUrl("javascript:highlight(" + currentTimePoint + ");");
                if (state == PlaybackState.STATE_PLAYING) {
                    currentTimePoint = -1; // force redo highlight current snippet when playing
                }
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(webViewJavaScriptInterface, "app");
        webView.setVerticalScrollBarEnabled(false);

        try {
            item = MagazineContent.getItem(new File(getIntent().getStringExtra(ARG_ROOT_DIRECTORY)));

        } catch (IOException e) {
            LogHelper.e(TAG, e.getMessage());
        }

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        startText = (TextView) findViewById(R.id.startText);
        endText = (TextView) findViewById(R.id.endText);

        findViewById(R.id.play).setEnabled(false);
        findViewById(R.id.pause).setEnabled(false);
        findViewById(R.id.prev).setEnabled(false);
        findViewById(R.id.next).setEnabled(false);
        seekBar.setEnabled(false);
        findViewById(R.id.controllers).setVisibility(item.audioFileName.length() > 0 ? View.VISIBLE : View.GONE);

        findViewById(R.id.play).setOnClickListener(this);
        findViewById(R.id.pause).setOnClickListener(this);
        findViewById(R.id.prev).setOnClickListener(this);
        findViewById(R.id.next).setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        findViewById(R.id.lock).setOnClickListener(this);
        findViewById(R.id.lock).setOnLongClickListener(this);

        try {
            File input = new File(item.rootDirectory, MagazineContent.Item.contentFileName);
            final Document doc = Jsoup.parse(input, "UTF-8", "");

            webView.loadUrl("file://" + input.getAbsolutePath());

            newWords = getNewWords(doc);
            timePoints = getTimePoints(doc);

        } catch (IOException e) {
            LogHelper.e(TAG, e.getMessage());
        }
    }

    private MenuItem lockActionButton;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.read_and_listen, menu);
        lockActionButton = menu.findItem(R.id.action_lock);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_lock:
                lockTranscript(true);
                return true;

            case R.id.action_unlock:
                lockTranscript(false);
                return true;

            case android.R.id.home:
                finish();

                // when this activity is launched from the notification, back button goes to home screen.
                // I could not find any solution except manually creating parent.
                Intent intent = new Intent(this, ItemListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                final String issueRootDirectory = this.item.rootDirectory.getParent();
                intent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, issueRootDirectory);

                startActivity(intent);

                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * if true, transcript is hidden, and user cannot cheat to understand audio.
     */
    protected boolean transcriptLocked = true;

    /**
     * if true, item has transcript that may become hidden, so that user can listen to audio without cheating.
     */
    protected boolean useLockControls = false;

    protected String[] webViewState = new String[0];

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("transcriptLocked", transcriptLocked);
        outState.putBoolean("useLockControls", useLockControls);
        outState.putStringArray("webViewState", webViewState);
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

        if (boundToMusicService) {
            musicService.removeOnMediaStateChangedListener(ReadAndListenActivity.this);
            unbindMusicService();
        }
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
            if (item.audioFileName.length() > 0)
                startIntent.putExtra("dataSource", new File(item.rootDirectory, item.audioFileName).getAbsolutePath());
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
                if (musicService != null && state == PlaybackState.STATE_PAUSED) { // rewind to start of audio snippet
                    musicService.seekTo(floorPosition(musicService.getCurrentMediaPosition(), 0, false));
                }
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
                int prevTimePoint = floorPosition(musicService.getCurrentMediaPosition(), 1500, false); // time point to seek to
                musicService.seekTo(prevTimePoint);
                seekBar.setProgress(prevTimePoint);
                break;
            }

            case R.id.next: {
                int nextTimePoint = ceilPosition(musicService.getCurrentMediaPosition()); // time point to seek to
                if (nextTimePoint < musicService.getDuration()) {
                    musicService.seekTo(nextTimePoint);
                    seekBar.setProgress(nextTimePoint);
                }
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
                            if (musicService == null) {
                                if (seekBarTimer != null) {
                                    seekBarTimer.cancel();
                                    seekBarTimer = null;
                                }
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
     * if true, code won't change seekBar position.
     */
    boolean ignoreSeekBar = false;

    /**
     * handle seekBar progress change
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        startText.setText(formatTime(progress));

        final int currentTimePoint = floorPosition(progress, 0, true);
        if (currentTimePoint != this.currentTimePoint) {
            this.currentTimePoint = currentTimePoint;
            webView.loadUrl("javascript:highlight(" + currentTimePoint + ");");
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        ignoreSeekBar = true; // prevent code from changing seekBar position
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        int prevPosition = floorPosition(seekBar.getProgress(), 0, false);
        musicService.seekTo(prevPosition);
        seekBar.setProgress(prevPosition);
        ignoreSeekBar = false;
    }

    /**
     * mathematical floor
     *
     * @param currentPosition apply floor on this time position
     * @param margin          returned value is behind current position by at least this margin
     * @param closed          if true, end of audio snippet is considered too
     * @return music position of the latest sound snippet behind current position
     */
    protected int floorPosition(int currentPosition, int margin, boolean closed) {
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
        int nextTimePoint = musicService.getDuration();

        for (int[] timePoint : timePoints) {
            if (timePoint[0] > currentPosition) {
                nextTimePoint = timePoint[0];
                break;
            }
        }

        return nextTimePoint;
    }

    public static ArrayList<int[]> getTimePoints(final Document doc) throws IOException {
        ArrayList<int[]> timePoints = new ArrayList<>();

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

    public static HashMap<String, NewWord> getNewWords(final Document doc) throws IOException {
        HashMap<String, NewWord> newWords = new HashMap<>();

        Elements spans = doc.getElementsByClass("new-word");
        for (Element span : spans) {
            NewWord newWord = new NewWord();
            newWord.definition = new HashMap<>();

            newWord.type = span.attr("data-type");
            newWord.definition.put("en", span.attr("data-en"));
            newWord.definition.put("fa", span.attr("data-fa"));

            newWords.put(span.attr("data-word"), newWord);
        }

        return newWords;
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.lock:
                lockTranscript(false);
                break;
        }
        return true;
    }

    /**
     * Hide transcript
     */
    protected void lockTranscript(boolean lock) {
        transcriptLocked = lock;
        webViewJavaScriptInterface.showLockControls(useLockControls);
        webView.loadUrl("javascript:lock(" + transcriptLocked + ");");
    }

    /*
     * JavaScript Interface. Web code can access methods in here
     * (as long as they have the @JavascriptInterface annotation)
     */
    public class WebViewJavaScriptInterface {
        /*
         * This method can be called from Android. @JavascriptInterface
         * required after SDK version 17.
         */
        @JavascriptInterface
        public void makeToast(String message, boolean lengthLong) {
            Toast.makeText(getApplicationContext(), message, (lengthLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)).show();
        }

        @JavascriptInterface
        public void saveInstanceState(final String[] state) {
            runOnUiThread(new Runnable() {
                public void run() {
                    webViewState = state;
                }
            });
        }

        /**
         * shows lock button and media playback controls if on correct slide. and hides them otherwise.
         */
        @JavascriptInterface
        public void showLockControls(boolean show) {
            useLockControls = show;
            runOnUiThread(new Runnable() {
                public void run() {
                    findViewById(R.id.lock).setVisibility(useLockControls && transcriptLocked ? View.VISIBLE : View.INVISIBLE);
                    if (lockActionButton != null)
                        lockActionButton.setVisible(useLockControls && !transcriptLocked);
                }
            });
        }

        @JavascriptInterface
        public void showGlossary(final String phrase, final int left, final int top, final int height) {
//            Toast.makeText(getApplicationContext(), newWords.get(phrase).definition.get("fa"), Toast.LENGTH_LONG).show();

            runOnUiThread(new Runnable() {
                public void run() {
                    final NewWord newWord = newWords.get(phrase);

                    LayoutInflater layoutInflater
                            = (LayoutInflater) getBaseContext()
                            .getSystemService(LAYOUT_INFLATER_SERVICE);

                    View popupView = layoutInflater.inflate(R.layout.word_definition, null);
                    ((TextView) popupView.findViewById(R.id.new_word)).setText(phrase);
                    ((TextView) popupView.findViewById(R.id.word_type)).setText(newWord.type);

                    final TextView textViewEn = (TextView) popupView.findViewById(R.id.def_en);
                    final String en = newWord.definition.get("en");
                    if (en.length() > 0)
                        textViewEn.setText(en);
                    else
                        textViewEn.setVisibility(View.GONE);

                    final TextView textViewFa = (TextView) popupView.findViewById(R.id.def_fa);
                    final String fa = newWord.definition.get("fa");
                    if (fa.length() > 0)
                        textViewFa.setText(fa);
                    else
                        textViewFa.setVisibility(View.GONE);

                    final PopupWindow popupWindow = new PopupWindow(
                            popupView,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    popupWindow.setBackgroundDrawable(new BitmapDrawable());
                    popupWindow.setOutsideTouchable(true);
                    popupWindow.setFocusable(true); // pressing back button will close popup window

                    if (top < webView.getHeight() / 2) {
                        popupWindow.showAtLocation(webView, Gravity.TOP | Gravity.LEFT, left, top + 3 * height + webView.getTop());
                    } else {
                        // TODO find correct value for first parameter of this call
                        popupView.measure(View.MeasureSpec.makeMeasureSpec(webView.getWidth(), View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED);
                        popupWindow.showAtLocation(webView, Gravity.TOP | Gravity.LEFT, left, top + height - popupView.getMeasuredHeight() + webView.getTop());
                    }
                }
            });
        }
    }
}
