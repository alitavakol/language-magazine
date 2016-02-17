package me.ali.coolenglishmagazine;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;

import java.io.File;

import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.LogHelper;

public class RootActivity extends AppCompatActivity implements GalleryOfIssuesFragment.OnFragmentInteractionListener {

    private static final String TAG = LogHelper.makeLogTag(RootActivity.class);

    /**
     * intent action that activates available issues tab, to show downloads in progress
     */
    public static final String ACTION_SHOW_DOWNLOADS = "me.ali.coolenglishmagazine.ACTION_SHOW_DOWNLOADS";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);

        if (savedInstanceState == null) {
            int tabIndex = 0;
            if (ACTION_SHOW_DOWNLOADS.equals(getIntent().getAction()))
                tabIndex = 1; // switch to available issues tab within the gallery of issues fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.root_fragment, GalleryOfIssuesFragment.newInstance(tabIndex), GalleryOfIssuesFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.issue_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * navigation drawer
     */
    Drawer drawer;

    public void onToolbarCreated(Toolbar toolbar) {
        setSupportActionBar(toolbar);
        PrimaryDrawerItem item1 = new PrimaryDrawerItem().withName(R.string.app_name).withIcon(R.drawable.key);
        SecondaryDrawerItem item2 = new SecondaryDrawerItem().withName(R.string.action_settings);
        drawer = new DrawerBuilder().withActivity(this).withToolbar(toolbar).withHeader(R.layout.drawer_header).addDrawerItems(
                item1,
                new DividerDrawerItem(),
                item2,
                new SecondaryDrawerItem().withName(R.string.active_issue)
        ).build();
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    public void onIssueSelected(Magazines.Issue issue) {
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putString(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());
            IssueDetailFragment fragment = new IssueDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.issue_detail_container, fragment)
                    .commit();

        } else {
            final File downloaded = new File(issue.rootDirectory, Magazines.Issue.downloadedFileName);
            if (downloaded.exists()) {
                // jump straight into issue's table of contents if it is downloaded
                Intent intent = new Intent(this, ItemListActivity.class);
                intent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());
                startActivity(intent);

            } else {
                // show intro and advertise the issue if it is not downloaded yet
                Intent intent = new Intent(this, IssueDetailActivity.class);
                intent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());
                startActivity(intent);
            }
        }
    }

}
