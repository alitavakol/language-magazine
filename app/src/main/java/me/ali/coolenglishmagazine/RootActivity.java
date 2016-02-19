package me.ali.coolenglishmagazine;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.io.File;

import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.FontManager;
import me.ali.coolenglishmagazine.util.LogHelper;

public class RootActivity extends AppCompatActivity implements GalleryOfIssuesFragment.OnFragmentInteractionListener {

    private static final String TAG = LogHelper.makeLogTag(RootActivity.class);

    /**
     * intent action that activates available issues tab inside gallery of issues fragment, to show downloads in progress
     */
    public static final String ACTION_SHOW_DOWNLOADS = "me.ali.coolenglishmagazine.ACTION_SHOW_DOWNLOADS";

    /**
     * intent action that activates cool english times fragment
     */
    public static final String ACTION_SHOW_TIMES = "me.ali.coolenglishmagazine.ACTION_SHOW_TIMES";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);

        if (savedInstanceState != null) {
            drawer_selection = savedInstanceState.getInt("drawer_selection");

        } else {
            // show the gallery of issues fragment by default
            Fragment fragment = GalleryOfIssuesFragment.newInstance(ACTION_SHOW_DOWNLOADS.equals(getIntent().getAction()) ? 1 : 0);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.root_fragment, fragment, GalleryOfIssuesFragment.FRAGMENT_TAG)
                    .commit();
        }

        setupNavigationDrawer();
    }

    Typeface typeface;

    protected void setupNavigationDrawer() {
        typeface = FontManager.getTypeface(getApplicationContext(), FontManager.ROBOTO);

        // manually load drawer header, and apply custom typeface to it.
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View headerView = inflater.inflate(R.layout.drawer_header, null);
        ((TextView) headerView).setTypeface(FontManager.getTypeface(getApplicationContext(), FontManager.ROBOTO_BOLD));

        PrimaryDrawerItem galleryOfIssues = new PrimaryDrawerItem().withName(R.string.gallery_of_issues).withIcon(GoogleMaterial.Icon.gmd_playlist_play).withTypeface(typeface);
        PrimaryDrawerItem englishTimes = new PrimaryDrawerItem().withName(R.string.cool_english_times).withIcon(GoogleMaterial.Icon.gmd_alarm).withTypeface(typeface);
        PrimaryDrawerItem readme = new PrimaryDrawerItem().withName(R.string.readme).withIcon(GoogleMaterial.Icon.gmd_sentiment_satisfied).withTypeface(typeface);
        PrimaryDrawerItem about = new PrimaryDrawerItem().withName(R.string.about).withIcon(GoogleMaterial.Icon.gmd_info_outline).withTypeface(typeface);

        drawer = new DrawerBuilder().withHeaderDivider(false).withActivity(this).withHeader(headerView).addDrawerItems(
                galleryOfIssues,
                englishTimes,
                readme,
                new DividerDrawerItem(),
                about
        ).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                if (drawer_selection == position)
                    return false;

                switch (position) {
                    case 1:
                        if (drawer_selection == 5)
                            getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentById(R.id.root_fragment)).commit();
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.root_fragment, GalleryOfIssuesFragment.newInstance(0))
                                .commit();
                        break;

                    default:
                        Toast.makeText(RootActivity.this, "item clicked: " + position, Toast.LENGTH_SHORT).show();
                }

                drawer_selection = position;
                return false;
            }
        }).withSelectedItemByPosition(drawer_selection).build();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (drawer != null) {
            outState.putInt("drawer_selection", drawer.getCurrentSelectedPosition());
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

    /**
     * selected drawer item position
     */
    int drawer_selection = 1;

    public void onToolbarCreated(Toolbar toolbar) {
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayShowTitleEnabled(false);

        TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.gallery_of_issues);
        toolbarTitle.setTypeface(typeface);

        drawer.setToolbar(this, toolbar);
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
