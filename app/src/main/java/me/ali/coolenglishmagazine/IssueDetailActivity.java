package me.ali.coolenglishmagazine;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import me.ali.coolenglishmagazine.broadcast_receivers.DownloadCompleteBroadcastReceiver;
import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.LogHelper;
import me.ali.coolenglishmagazine.widget.ObservableScrollView;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * An activity representing a single Issue detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link RootActivity}.
 */
public class IssueDetailActivity extends AppCompatActivity implements ObservableScrollView.Callbacks, Magazines.Issue.OnStatusChangedListener {

    private static final String TAG = LogHelper.makeLogTag(IssueDetailActivity.class);

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ROOT_DIRECTORY = "issue_root_directory";

    Magazines.Issue issue;

    ImageButton buttonCancel;
    Button buttonDownload, buttonOpen, buttonDelete, buttonComplete, buttonIncomplete;
    ProgressBar progressBar;
    ViewGroup progressContainer, buttonContainer;
    private ObservableScrollView mScrollView;
    private View mPhotoViewContainer;
    private LinearLayout mHeaderSession;
    private FrameLayout issueDetailsContainer;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_issue_detail);
        toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        downloadManager = (DownloadManager) IssueDetailActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);

        buttonCancel = (ImageButton) findViewById(R.id.buttonCancel);
        buttonDownload = (Button) findViewById(R.id.buttonDownload);
        buttonOpen = (Button) findViewById(R.id.buttonOpen);
        buttonComplete = (Button) findViewById(R.id.buttonComplete);
        buttonIncomplete = (Button) findViewById(R.id.buttonIncomplete);
        buttonDelete = (Button) findViewById(R.id.buttonDelete);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        buttonContainer = (ViewGroup) findViewById(R.id.button_container);
        progressContainer = (ViewGroup) findViewById(R.id.progress_container);

        try {
            issue = Magazines.getIssue(this, new File(getIntent().getStringExtra(ARG_ROOT_DIRECTORY)));

            issue.addOnStatusChangedListener(this);
            downloadReference = Magazines.getDownloadReference(this, issue);

            ((TextView) findViewById(R.id.session_title)).setText(issue.description);
            ((TextView) findViewById(R.id.session_subtitle)).setText(issue.subtitle);

            buttonOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish(); // remove this activity from back stack

                    Intent intent = new Intent(IssueDetailActivity.this, ItemListActivity.class);
                    intent.putExtra(ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());
                    startActivity(intent);
                }
            });

            // update visibility of complete and incomplete buttons
            onIssueStatusChanged(issue);

            buttonComplete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Magazines.markCompleted(issue);
                }
            });

            buttonIncomplete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Magazines.reopen(IssueDetailActivity.this, issue);
                }
            });

            buttonDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        downloadReference = Magazines.download(IssueDetailActivity.this, issue);
                        updateFab();
                    } catch (IOException e) {
                        LogHelper.e(TAG, e.getMessage());
                    }
                }
            });

            buttonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    downloadManager.remove(downloadReference);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            downloadManager.remove(downloadReference); // http://stackoverflow.com/a/34797980
                        }
                    }, 1000);
                    issue.setStatus(Magazines.Issue.Status.available);
                    updateFab();
                }
            });
            buttonCancel.setImageDrawable(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_clear).sizeRes(R.dimen.tw__login_btn_text_size).colorRes(R.color.accent));

            buttonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Magazines.deleteIssue(IssueDetailActivity.this, issue, false);
                    updateFab();
                }
            });

            // Show the Up button in the action bar.
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(false); // hide action bar title
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            mScrollView = (ObservableScrollView) findViewById(R.id.scroll_view);
            mPhotoViewContainer = findViewById(R.id.session_photo_container);
            mHeaderSession = (LinearLayout) findViewById(R.id.header_session);
            issueDetailsContainer = (FrameLayout) findViewById(R.id.issue_detail_container);

            mScrollView.addCallbacks(this);
            setOnScrollViewLayoutChangedListener();

            // savedInstanceState is non-null when there is fragment state
            // saved from previous configurations of this activity
            // (e.g. when rotating the screen from portrait to landscape).
            // In this case, the fragment will automatically be re-added
            // to its container so we don't need to manually add it.
            // For more information, see the Fragments API guide at:
            //
            // http://developer.android.com/guide/components/fragments.html
            //
            if (savedInstanceState == null) {
                // Create the detail fragment and add it to the activity
                // using a fragment transaction.
                Bundle arguments = new Bundle();

                final String issueRootDirectory = getIntent().getStringExtra(ARG_ROOT_DIRECTORY);
                arguments.putString(ARG_ROOT_DIRECTORY, issueRootDirectory);

                IssueDetailFragment fragment = new IssueDetailFragment();
                fragment.setArguments(arguments);

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.issue_detail_container, fragment)
                        .commit();
            }

        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (issue != null)
            issue.removeOnStatusChangedListener(this);
    }

    public void setOnScrollViewLayoutChangedListener() {
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
        }
    }

    Toolbar toolbar;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.common, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();

                Intent intent = new Intent(this, RootActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                startActivity(intent);
                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private long downloadReference;
    private BroadcastReceiver receiverDownloadExtracted;

    @Override
    protected void onResume() {
        super.onResume();

        receiverDownloadExtracted = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateFab();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverDownloadExtracted, new IntentFilter(DownloadCompleteBroadcastReceiver.ACTION_DOWNLOAD_EXTRACTED));

        updateFab();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) {
            timer.cancel();
            timer = null;
            LogHelper.i(TAG, "timer cancelled.");
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverDownloadExtracted);
    }

    DownloadManager downloadManager;

    /**
     * progress bar update timer
     */
    Timer timer;

    /**
     * progress bar value
     */
    int dl_progress;

    /**
     * updates visibility of buttons and progress bar, depending on current issue download status
     */
    void updateFab() {
        final int status = Magazines.getDownloadStatus(this, issue);

        switch (status) {
            case DownloadManager.STATUS_PENDING:
            case DownloadManager.STATUS_PAUSED:
                buttonContainer.setVisibility(View.INVISIBLE);
                progressContainer.setVisibility(View.VISIBLE);
                buttonCancel.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
                break;

            case DownloadManager.STATUS_RUNNING:
                buttonContainer.setVisibility(View.INVISIBLE);
                progressContainer.setVisibility(View.VISIBLE);
                buttonCancel.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(false);
                progressBar.setProgress(dl_progress);
                break;

            case -3: // the issue is being extracted
                buttonContainer.setVisibility(View.INVISIBLE);
                progressContainer.setVisibility(View.VISIBLE);
                buttonCancel.setVisibility(View.GONE);
                progressBar.setIndeterminate(true);
                break;

            default:
                buttonContainer.setVisibility(View.VISIBLE);
                progressContainer.setVisibility(View.INVISIBLE);

                if (new File(issue.rootDirectory, Magazines.Issue.downloadedFileName).exists()) {
                    buttonDownload.setVisibility(View.GONE);
                    buttonOpen.setVisibility(View.VISIBLE);
                    buttonDelete.setVisibility(View.VISIBLE);
                } else {
                    buttonDownload.setText(status == DownloadManager.STATUS_FAILED ? R.string.retry_download : R.string.download);
                    buttonDownload.setVisibility(View.VISIBLE);
                    buttonOpen.setVisibility(View.GONE);
                    buttonDelete.setVisibility(View.GONE);
                }

                if (timer != null) {
                    timer.cancel();
                    timer = null;
                    LogHelper.i(TAG, "timer cancelled.");
                }
                return;
        }

        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    dl_progress = Magazines.getDownloadProgress(IssueDetailActivity.this, issue);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateFab();
                            LogHelper.i(TAG, "download progress: " + dl_progress);
                        }
                    });
                }
            }, 0, 1000);
            LogHelper.i(TAG, "timer created.");
        }
    }

    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        int scrollY = mScrollView.getScrollY();
        mPhotoViewContainer.setTranslationY(scrollY * 0.5f);
        mHeaderSession.setTranslationY(Math.max(headerTranslation, scrollY));
//        mHeaderSession.setElevation(scrollY > headerTranslation ? 10 : 0);
    }

    int headerTranslation;

    /**
     * adjust top margin and translationY of views contained in the FrameLayout, which depends on measured height of them
     */
    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // render cover image with its real aspect ratio; but if its height exceeds 5/7 of available height, centerCrop it.
            File coverImageFile = new File(issue.rootDirectory, Magazines.Issue.posterFileName);
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(coverImageFile.getAbsolutePath(), options);
            float aspectRatio = (float) options.outHeight / (float) options.outWidth;

            final ImageView coverImageView = (ImageView) findViewById(R.id.cover);
            int targetWidth = mScrollView.getMeasuredWidth();
            int targetHeight = Math.min((int) (targetWidth * aspectRatio), 5 * mScrollView.getMeasuredHeight() / 7);

            Picasso
                    .with(IssueDetailActivity.this)
                    .load(coverImageFile)
                    .resize(targetWidth, targetHeight)
                    .centerCrop()
                    .into(coverImageView);

            headerTranslation = targetHeight;
            int headerHeight = mHeaderSession.getHeight();

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) issueDetailsContainer.getLayoutParams();
            lp.bottomMargin = headerTranslation + headerHeight;
            issueDetailsContainer.setLayoutParams(lp);

            issueDetailsContainer.setTranslationY(headerTranslation + headerHeight);

            mScrollView.fullScroll(View.FOCUS_UP);
            onScrollChanged(0, 0);

            mScrollView.getViewTreeObserver().removeGlobalOnLayoutListener(mGlobalLayoutListener);
        }
    };

    public void onIssueStatusChanged(Magazines.Issue issue) {
        final Magazines.Issue.Status status = issue.getStatus();

        if (status == Magazines.Issue.Status.downloading || status == Magazines.Issue.Status.available) {
            buttonComplete.setVisibility(View.GONE);
            buttonIncomplete.setVisibility(View.GONE);

        } else {
            buttonIncomplete.setVisibility(status == Magazines.Issue.Status.completed ? View.VISIBLE : View.GONE);
            buttonComplete.setVisibility(status == Magazines.Issue.Status.completed ? View.GONE : View.VISIBLE);
        }
    }
}
