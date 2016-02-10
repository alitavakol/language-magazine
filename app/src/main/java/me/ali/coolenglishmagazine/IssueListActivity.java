package me.ali.coolenglishmagazine;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.InputStreamVolleyRequest;
import me.ali.coolenglishmagazine.util.LogHelper;
import me.ali.coolenglishmagazine.util.NetworkHelper;
import me.ali.coolenglishmagazine.util.ZipHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An activity representing a list of Issues. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link IssueDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class IssueListActivity extends AppCompatActivity implements IssuesListFragment.OnFragmentInteractionListener {

    private static final String TAG = LogHelper.makeLogTag(IssueListActivity.class);

    /**
     * intent action that activates available issues tab, to show downloads in progress
     */
    public static final String ACTION_SHOW_DOWNLOADS = "me.ali.coolenglishmagazine.ACTION_SHOW_DOWNLOADS";

    Magazines magazines = new Magazines();

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        toolbar.setTitle(getTitle());

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        if(ACTION_SHOW_DOWNLOADS.equals(getIntent().getAction()))
            tabLayout.getTabAt(1).select();

        magazines.loadIssues(getExternalFilesDir(null).getAbsolutePath());
        firstMissingIssueNumber = findFirstMissingIssueNumber();

        if (findViewById(R.id.issue_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(IssuesListFragment.newInstance(IssuesListFragment.MY_ISSUES), R.string.my_issues);
        adapter.addFragment(IssuesListFragment.newInstance(IssuesListFragment.AVAILABLE_ISSUES), R.string.available_issues);
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, int titleId) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(getResources().getString(titleId));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }


    @Override
    protected void onStop() {
        super.onStop();

        if (requestQueue != null) {
            requestQueue.cancelAll(this);
            requestQueue = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.issue_list, menu);
        return true;
    }

    RequestQueue requestQueue = null;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
//            case R.id.action_refresh:
//                syncAvailableIssuesList(firstMissingIssueNumber);
//                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * issue number of the first missing issue
     */
    int firstMissingIssueNumber;

    /**
     * do not perform sync when already in a sync job
     */
    boolean syncing = false;

    /**
     * gets list of available issues from server.
     */
    void syncAvailableIssuesList(int firstMissingIssueNumber, final RecyclerView.Adapter adapter) {
        if (firstMissingIssueNumber == -1) {
            if (syncing)
                return;
            syncing = true;
            firstMissingIssueNumber = this.firstMissingIssueNumber;
        }

        // Instantiate the RequestQueue.
        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(this);

        if (NetworkHelper.isOnline(this)) {
            final Uri uri = Uri.parse(PreferenceManager.getDefaultSharedPreferences(this).getString("server_address", getResources().getString(R.string.pref_default_server_address)));
            // http://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
            final String url = uri.toString() + "/api/issues?min_issue_number=" + firstMissingIssueNumber;

            // Request a string response from the provided URL.
            InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, url, new Response.Listener<byte[]>() {
                @Override
                public void onResponse(byte[] response) {
                    try {
                        final File cacheDir = IssueListActivity.this.getExternalCacheDir();
                        final File zipFile = File.createTempFile("issues-preview", ".zip", cacheDir);

                        FileOutputStream f = new FileOutputStream(zipFile);
                        f.write(response, 0, response.length);
                        f.close();

                        ZipHelper.unzip(zipFile, IssueListActivity.this.getExternalFilesDir(null));
                        zipFile.delete();

                        // get next bunch of available issues, until the saved list of issues remain unchanged
                        int firstMissingIssueNumber = findFirstMissingIssueNumber();
                        if (firstMissingIssueNumber > IssueListActivity.this.firstMissingIssueNumber) {
                            syncAvailableIssuesList(firstMissingIssueNumber, adapter);
                        }

                        magazines.loadIssues(getExternalFilesDir(null).getAbsolutePath());
                        IssueListActivity.this.firstMissingIssueNumber = firstMissingIssueNumber;

                        ((IssuesListFragment.IssuesRecyclerViewAdapter) adapter).preNotifyDataSetChanged(true);

                    } catch (IOException e) {
                        LogHelper.e(TAG, e.getMessage());

                    } finally {
                        syncing = false;
                        ((IssuesListFragment.IssuesRecyclerViewAdapter) adapter).preNotifyDataSetChanged(false);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
//                    LogHelper.e(TAG, error.getMessage()); // error
                    Toast.makeText(IssueListActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                    requestQueue.cancelAll(IssueListActivity.this);
                    syncing = false;
                    ((IssuesListFragment.IssuesRecyclerViewAdapter) adapter).preNotifyDataSetChanged(false);
                }
            }, null);

            request.setTag(this);

            // Add the request to the RequestQueue.
            requestQueue.add(request);

        } else {
            Toast.makeText(this, R.string.check_connection, Toast.LENGTH_SHORT).show();
            requestQueue.cancelAll(this);
            syncing = false;
            ((IssuesListFragment.IssuesRecyclerViewAdapter) adapter).preNotifyDataSetChanged(false);
        }
    }

    /**
     * in order to ask server for new issues, we need to know minimum issue number to request for.
     * in response to each request, server sends compressed file issues-preview-{10k}-{10k+9}.zip,
     * which also contains issue with requested minimum issue number.
     *
     * @return issue number of the first missing magazine
     */
    protected int findFirstMissingIssueNumber() {
        int i = 1;
        while (new File(getExternalFilesDir(null), Integer.toString(i)).exists())
            i++;

        return i;
    }

    public void onItemClicked(Magazines.Issue issue) {
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putString(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());
            IssueDetailFragment fragment = new IssueDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.issue_detail_container, fragment)
                    .commit();

        } else {
            final File downloaded = new File(issue.rootDirectory, "downloaded");
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
