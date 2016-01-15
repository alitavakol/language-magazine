package me.ali.coolenglishmagazine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;

import me.ali.coolenglishmagazine.data.Magazines;
import me.ali.coolenglishmagazine.util.LogHelper;

/**
 * An activity representing a single Issue detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link IssueListActivity}.
 */
public class IssueDetailActivity extends AppCompatActivity {

    private static final String TAG = LogHelper.makeLogTag(IssueDetailActivity.class);

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ROOT_DIRECTORY = "issue_root_directory";

    Magazines.Issue issue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            downloadReference = savedInstanceState.getLong("downloadReference");
        }

        setContentView(R.layout.activity_issue_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        try {
            issue = Magazines.getIssue(new File(getIntent().getStringExtra(ARG_ROOT_DIRECTORY)));
        } catch (Exception e) {
        }

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(IssueDetailActivity.this, ItemListActivity.class);
                intent.putExtra(ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());

                startActivity(intent);
                finish(); // remove this activity from back stack
            }
        });

        findViewById(R.id.fab_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    downloadReference = Magazines.download(IssueDetailActivity.this, issue);
                    findViewById(R.id.fab_download).setClickable(false);
                    updateFab();
                } catch (IOException e) {
                    LogHelper.e(TAG, e.getMessage());
                }
            }
        });

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();

            Intent intent = new Intent(this, IssueListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private long downloadReference;
    private BroadcastReceiver receiverDownloadExtracted;

    @Override
    protected void onResume() {
        super.onResume();

        updateFab();

        receiverDownloadExtracted = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateFab();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverDownloadExtracted, new IntentFilter(DownloadCompleteBroadcastReceiver.ACTION_DOWNLOAD_EXTRACTED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverDownloadExtracted);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong("downloadReference", downloadReference);
    }

    void updateFab() {
        final int status = Magazines.getDownloadStatus(this, issue);
        // TODO: take the above download status into account

        if (new File(issue.rootDirectory, Magazines.Issue.downloadedFileName).exists()) {
            findViewById(R.id.fab_download).setVisibility(View.GONE);
            findViewById(R.id.fab).setVisibility(View.VISIBLE);

        } else {
            findViewById(R.id.fab_download).setVisibility(View.VISIBLE);
            findViewById(R.id.fab).setVisibility(View.GONE);
        }
    }

}
