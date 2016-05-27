package me.ali.coolenglishmagazine;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.IBinder;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import me.ali.coolenglishmagazine.broadcast_receivers.AlarmBroadcastReceiver;
import me.ali.coolenglishmagazine.model.MagazineContent;
import me.ali.coolenglishmagazine.model.WaitingItems;
import me.ali.coolenglishmagazine.util.FontManager;
import me.ali.coolenglishmagazine.util.LogHelper;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class ReadAndListenActivity extends AppCompatActivity implements View.OnClickListener, MusicService.OnMediaStateChangedListener, SeekBar.OnSeekBarChangeListener {

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
    protected int state = PlaybackStateCompat.STATE_NONE;

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

    WebViewJavaScriptInterface webViewJavaScriptInterface;

    protected ViewGroup mediaControllerButtons;
    private Animation slide_down, slide_up;
    private boolean slideUpAnimating, isSlideDownAnimating;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        File webContentFile;

        try {
            item = MagazineContent.getItem(new File(getIntent().getStringExtra(ARG_ROOT_DIRECTORY)));

            webContentFile = new File(item.rootDirectory, MagazineContent.Item.contentFileName);
            final Document doc = Jsoup.parse(webContentFile, "UTF-8", "");

            newWords = getNewWords(doc);
            timePoints = getTimePoints(doc);

        } catch (IOException e) {
            e.printStackTrace();
            finish();
            return;
        }

        setContentView(R.layout.activity_read_and_listen);

        accentColor = getResources().getIntArray(R.array.levelColors)[item.level];

        if (savedInstanceState != null) {
            transcriptLocked = savedInstanceState.getBoolean("transcriptLocked");
            useLockControls = savedInstanceState.getBoolean("useLockControls");
            webViewState = savedInstanceState.getString("webViewState");

        } else {
            // if the item has no audio, suppose user has learnt this item. for items with audio,
            // music service does this job:
            // increment hit count if it is in the list of waiting items.
            if (item.audioFileName.length() == 0)
                WaitingItems.incrementHitCount(this, item);
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(getResources().getIntArray(R.array.darkLevelColors)[item.level]);

        appBar = (AppBarLayout) findViewById(R.id.app_bar);
        if (appBar != null)
            appBar.setBackgroundColor(accentColor);

        mediaControllerButtons = (ViewGroup) findViewById(R.id.controllers);
        mediaControllerButtons.getChildAt(0).setBackgroundColor(accentColor);

        slide_up = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
        slide_up.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                slideUpAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        slide_down = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);
        slide_down.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mediaControllerHiddenForSpace)
                    mediaControllerButtons.setVisibility(View.INVISIBLE);
                isSlideDownAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        // Get a support ActionBar corresponding to this toolbar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false); // hide action bar title
            actionBar.setDisplayHomeAsUpEnabled(true); // Enable the Up button
            actionBar.setHomeAsUpIndicator(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_arrow_back).sizeDp(24).paddingDp(4).colorRes(R.color.md_light_appbar));
        }

        // set item type as action bar title
        TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(item.type);

        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                final String command = "javascript:adjustLayout({"
                        + "topMargin: " + appBar.getMeasuredHeight() // HTML content top margin for content
                        + ", bottomMargin: " + mediaControllerButtons.getMeasuredHeight()
                        + ", horizontalMargin: " + getResources().getDimension(R.dimen.activity_horizontal_margin)
                        + ", verticalMargin: " + getResources().getDimension(R.dimen.activity_vertical_margin)
                        + ", spacing: " + getResources().getDimension(R.dimen.spacing_normal)
                        + ", backgroundColor: " + ContextCompat.getColor(getApplicationContext(), android.R.color.background_light)
                        + ", height: " + webView.getMeasuredHeight() // window height
                        + ", accentColor: " + accentColor // accent color
                        + ", primaryColor: " + ContextCompat.getColor(getApplicationContext(), R.color.primary) // accent color
                        + ", textColor: " + ContextCompat.getColor(getApplicationContext(), android.R.color.secondary_text_light) // text color
                        + "});";
                webView.loadUrl(command);
                webView.loadUrl("javascript:restoreInstanceState('" + (webViewState != null ? webViewState.replace("'", "\\'") : "{}") + "');");
                webView.loadUrl("javascript:setTimeout(function() { app.onAdjustLayoutComplete(); }, 300);");

                if (webViewJavaScriptInterface != null)
                    webViewJavaScriptInterface.lockTranscript(transcriptLocked);

                webView.loadUrl("javascript:highlight(" + currentTimePoint + ");");
                if (state == PlaybackStateCompat.STATE_PLAYING) {
                    currentTimePoint = -1; // force redo highlight current snippet if playing
                }
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webViewJavaScriptInterface = new WebViewJavaScriptInterface();
        webView.addJavascriptInterface(webViewJavaScriptInterface, "app");
        webView.setVerticalScrollBarEnabled(false);
        // prevent long click, because jquery mobile handles it to unlock transcript.
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        webView.setLongClickable(false);

        ((ImageView) findViewById(R.id.hourglass)).setImageDrawable(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_hourglass_full).sizeDp(72).color(accentColor));

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        startText = (TextView) findViewById(R.id.startText);
        endText = (TextView) findViewById(R.id.endText);

        playButton = (ImageView) findViewById(R.id.play);
        pauseButton = (ImageView) findViewById(R.id.pause);
        prevButton = (ImageView) findViewById(R.id.prev);
        nextButton = (ImageView) findViewById(R.id.next);

        prevButton.setEnabled(false);
        playButton.setEnabled(false);
        pauseButton.setEnabled(false);
        nextButton.setEnabled(false);
        seekBar.setEnabled(false);
        mediaControllerButtons.setVisibility(item.audioFileName.length() > 0 ? View.VISIBLE : View.GONE);

        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        prevButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);

        playButton.setImageDrawable(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_play_arrow).sizeDp(24).colorRes(android.R.color.primary_text_dark));
        pauseButton.setImageDrawable(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_pause).sizeDp(24).colorRes(android.R.color.primary_text_dark));
        prevButton.setImageDrawable(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_fast_rewind).sizeDp(24).colorRes(android.R.color.primary_text_dark));
        nextButton.setImageDrawable(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_fast_forward).sizeDp(24).colorRes(android.R.color.primary_text_dark));

        webView.loadUrl(webContentFile.toURI().toString());

        // cancel alarm notification to learn this item.
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(AlarmBroadcastReceiver.COOL_ENGLISH_TIME_NOTIFICATION_ID + item.getUid());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (webViewJavaScriptInterface != null) {
            webView.removeJavascriptInterface("app");
            webViewJavaScriptInterface = null;
        }
    }

    private MenuItem lockActionButton, unlockActionButton;
    protected int accentColor;

    protected ImageView playButton, pauseButton, prevButton, nextButton;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.read_and_listen, menu);
        getMenuInflater().inflate(R.menu.common, menu);

        lockActionButton = menu.findItem(R.id.action_lock);
        lockActionButton.setIcon(new IconicsDrawable(this, FontAwesome.Icon.faw_lock).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text));

        unlockActionButton = menu.findItem(R.id.action_unlock);
        unlockActionButton.setIcon(new IconicsDrawable(this, FontAwesome.Icon.faw_unlock).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text));

        menu.findItem(R.id.action_add_to_waiting_list).setIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_add).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text));

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
                if (webViewJavaScriptInterface != null)
                    webViewJavaScriptInterface.lockTranscript(true);
                return true;

            case R.id.action_unlock:
                if (webViewJavaScriptInterface != null)
                    webViewJavaScriptInterface.lockTranscript(false);
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

            case R.id.action_add_to_waiting_list:
                WaitingItems.appendToWaitingList(this, this.item);
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

    /**
     * holds state variables of the web view widget as a JSON string. meaning of them
     * is internal to and maintained by the web view itself.
     */
    protected String webViewState;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("transcriptLocked", transcriptLocked);
        outState.putBoolean("useLockControls", useLockControls);
        outState.putString("webViewState", webViewState);
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

    @Override
    public void onResume() {
        super.onResume();
        MusicService.readAndListenActivityResumed = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        MusicService.readAndListenActivityResumed = false;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (!getIntent().getStringExtra(ARG_ROOT_DIRECTORY).equals(intent.getStringExtra(ARG_ROOT_DIRECTORY))) {
            setIntent(intent);
            recreate();
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

            musicService.setTimePoints(timePoints);

            // start ACTION_PREPARE even if this item has no audio. this will help user concentrate better on the opened item.
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
                seekBar.setProgress(musicService.rewind());
                break;
            }

            case R.id.next: {
                seekBar.setProgress(musicService.fastForward());
                break;
            }

            default:
                break;
        }
    }

    protected SeekBar seekBar = null;
    protected TextView startText = null, endText = null;
    protected Timer seekBarTimer = null;

    protected WebView webView = null;

    protected String formatTime(int time) {
        return String.format(Locale.US, "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(time),
                TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1));
    }

    @Override
    public void onMediaStateChanged(int state) {
        LogHelper.i(TAG, "media playback state: ", state);

        if (state == PlaybackStateCompat.STATE_FAST_FORWARDING || state == PlaybackStateCompat.STATE_REWINDING)
            state = this.state;

        if (state != PlaybackStateCompat.STATE_NONE) {
            final int duration = musicService.getDuration();
            final int currentPosition = musicService.getCurrentMediaPosition();

            seekBar.setMax(duration);
            seekBar.setProgress(currentPosition);

            endText.setText(formatTime(duration));
        }

        playButton.setEnabled(state != PlaybackStateCompat.STATE_NONE);
        pauseButton.setEnabled(state != PlaybackStateCompat.STATE_NONE);

        final boolean canSeek = state == PlaybackStateCompat.STATE_PAUSED || state == PlaybackStateCompat.STATE_PLAYING;
        prevButton.setEnabled(canSeek);
        nextButton.setEnabled(canSeek);
        seekBar.setEnabled(canSeek);

        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);

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

        } else if (state == PlaybackStateCompat.STATE_STOPPED) {
            playButton.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.GONE);

            if (seekBarTimer != null) {
                seekBarTimer.cancel();
                seekBarTimer = null;
            }

        } else if (state == PlaybackStateCompat.STATE_PAUSED) {
            playButton.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.GONE);

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

        final int currentTimePoint = musicService.floorPosition(progress, 0, true);
        if (currentTimePoint != this.currentTimePoint) {
            this.currentTimePoint = currentTimePoint;
            webView.loadUrl("javascript:highlight(" + currentTimePoint + ");");
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        ignoreSeekBar = true; // prevent code from changing seekBar position
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        int prevPosition = musicService.floorPosition(seekBar.getProgress(), 0, false);
        musicService.seekTo(prevPosition);
        seekBar.setProgress(prevPosition);
        ignoreSeekBar = false;
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

            newWord.type = span.attr("data-type") + ' '; // italic text crop workaround
            newWord.definition.put("en", span.attr("data-en"));
            newWord.definition.put("fa", span.attr("data-fa"));

            newWords.put(span.attr("data-word"), newWord);
        }

        return newWords;
    }

    /**
     * JavaScript Interface. Web code can access methods in here
     * (as long as they have the @JavascriptInterface annotation)
     */
    public class WebViewJavaScriptInterface {
        /*
         * This method can be called from Android. @JavascriptInterface
         * required after SDK version 17.
         */
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void makeToast(String message, boolean lengthLong) {
            Toast.makeText(getApplicationContext(), message, (lengthLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)).show();
        }

        /**
         * called by the web view to deliver its state, and the android app will save it.
         *
         * @param state web view state as a JSON string, whose meaning is internal to the web view itself.
         */
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void saveInstanceState(final String state) {
            runOnUiThread(new Runnable() {
                public void run() {
                    webViewState = state;
                }
            });
        }

        /**
         * shows lock button and media playback controls if on correct slide. and hides them otherwise.
         */
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void showLockControls(boolean show) {
            useLockControls = show;
            runOnUiThread(new Runnable() {
                public void run() {
//                    findViewById(R.id.lock).setVisibility(useLockControls && transcriptLocked ? View.VISIBLE : View.GONE);
                    if (lockActionButton != null) {
                        lockActionButton.setVisible(useLockControls && !transcriptLocked);
                        unlockActionButton.setVisible(useLockControls && transcriptLocked);
                    }
                }
            });
        }

        /**
         * toggle transcript visibility
         *
         * @param lock hide transcript if true
         */
        @JavascriptInterface
        public void lockTranscript(final boolean lock) {
            runOnUiThread(new Runnable() {
                public void run() {
                    transcriptLocked = lock;
                    if (webViewJavaScriptInterface != null)
                        webViewJavaScriptInterface.showLockControls(useLockControls);
                    webView.loadUrl("javascript:lock(" + transcriptLocked + ");");
                }
            });
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void showGlossary(final String phrase, final int left, final int top, final int height) {
            runOnUiThread(new Runnable() {
                public void run() {
                    final NewWord newWord = newWords.get(phrase);

                    LayoutInflater layoutInflater = ReadAndListenActivity.this.getLayoutInflater();//.cloneInContext(new ContextThemeWrapper(ReadAndListenActivity.this, R.style.ReadAndListenTheme_BeginnerLevel));
                    View popupView = layoutInflater.inflate(R.layout.word_definition, null);

                    // could not apply level theme to this popup view. do it manually :(
                    TextView textViewNewWord = (TextView) popupView.findViewById(R.id.new_word);
                    textViewNewWord.setText(phrase);
                    textViewNewWord.setTextColor(getResources().getIntArray(R.array.levelColors)[item.level]);
                    textViewNewWord.setTypeface(FontManager.getTypeface(getApplicationContext(), FontManager.UBUNTU_BOLD));

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
                    else {
                        textViewFa.setVisibility(View.GONE);
                        popupView.findViewById(R.id.separator).setVisibility(View.GONE);
                    }

                    final PopupWindow popupWindow = new PopupWindow(
                            popupView,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    popupWindow.setBackgroundDrawable(new BitmapDrawable());
                    popupWindow.setOutsideTouchable(true);
                    popupWindow.setFocusable(true); // pressing back button will close popup window

                    if (top < webView.getHeight() / 2) {
                        popupWindow.showAtLocation(webView, Gravity.TOP | Gravity.START, left, top + 3 * height + webView.getTop());
                    } else {
                        // TODO find correct value for first parameter of this call
                        popupView.measure(View.MeasureSpec.makeMeasureSpec(webView.getWidth(), View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED);
                        popupWindow.showAtLocation(webView, Gravity.TOP | Gravity.START, left, top + height - popupView.getMeasuredHeight() + webView.getTop());
                    }
                }
            });
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void onAdjustLayoutComplete() {
            runOnUiThread(new Runnable() {
                public void run() {
                    webView.setVisibility(View.VISIBLE);
                    findViewById(R.id.hourglass).setVisibility(View.GONE);
                }
            });
        }

        /**
         * hides media controller buttons to gain space for content.
         *
         * @param hide if true, toolbar goes hidden.
         */
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void hideMediaControllerForSpace(final boolean hide) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mediaControllerHiddenForSpace = hide;

                    if (hide) {
                        // hide media controller buttons if not currently playing
                        if (mediaControllerButtons.getVisibility() == View.VISIBLE && state != PlaybackStateCompat.STATE_PLAYING && state != PlaybackStateCompat.STATE_PAUSED && !isSlideDownAnimating) {
                            isSlideDownAnimating = true;
                            mediaControllerButtons.startAnimation(slide_down);
                        }

                    } else {
                        if (mediaControllerButtons.getVisibility() == View.INVISIBLE) {
                            if (!slideUpAnimating) {
                                slideUpAnimating = true;
                                mediaControllerButtons.startAnimation(slide_up);
                            }
                            mediaControllerButtons.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }

        /**
         * makes toolbar auto hide (slide up) on scroll.
         *
         * @param scrollable if true, toolbar will automatically slide up on scroll down,
         *                   and come back on scroll up.
         */
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void setToolbarScrollable(final boolean scrollable) {
            runOnUiThread(new Runnable() {
                public void run() {
                    AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
                    params.setScrollFlags(scrollable ? AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS : 0);
                    toolbar.setLayoutParams(params);
                    if(scrollable)
                        appBar.setExpanded(false);
                }
            });
        }
    }

    protected boolean mediaControllerHiddenForSpace;

    protected AppBarLayout appBar;
    protected Toolbar toolbar;
}
