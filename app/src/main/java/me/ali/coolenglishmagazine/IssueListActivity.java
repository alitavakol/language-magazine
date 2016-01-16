package me.ali.coolenglishmagazine;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import java.util.List;

/**
 * An activity representing a list of Issues. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link IssueDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class IssueListActivity extends AppCompatActivity {

    private static final String TAG = LogHelper.makeLogTag(IssueListActivity.class);

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        magazines.loadIssues(getExternalFilesDir(null).getAbsolutePath());
        firstMissingIssueNumber = findFirstMissingIssueNumber();

        View recyclerView = findViewById(R.id.issue_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        if (findViewById(R.id.issue_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
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

    protected IssuesRecyclerViewAdapter adapter;

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        adapter = new IssuesRecyclerViewAdapter(magazines.ISSUES);
        recyclerView.setAdapter(adapter);
    }

    public class IssuesRecyclerViewAdapter
            extends RecyclerView.Adapter<IssuesRecyclerViewAdapter.ViewHolder> {

        private final List<Magazines.Issue> mValues;

        public IssuesRecyclerViewAdapter(List<Magazines.Issue> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.issue_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.issue = mValues.get(position);
            holder.mContentView.setText(mValues.get(position).title);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(IssueDetailActivity.ARG_ROOT_DIRECTORY, holder.issue.rootDirectory.getAbsolutePath());
                        IssueDetailFragment fragment = new IssueDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.issue_detail_container, fragment)
                                .commit();

                    } else {
                        Context context = v.getContext();

                        final File downloaded = new File(holder.issue.rootDirectory, "downloaded");
                        if (downloaded.exists()) {
                            // jump straight into issue's table of contents if it is downloaded
                            Intent intent = new Intent(context, ItemListActivity.class);
                            intent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, holder.issue.rootDirectory.getAbsolutePath());
                            context.startActivity(intent);

                        } else {
                            // show intro and advertise the issue if it is not downloaded yet
                            Intent intent = new Intent(context, IssueDetailActivity.class);
                            intent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, holder.issue.rootDirectory.getAbsolutePath());
                            context.startActivity(intent);
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mContentView;
            public Magazines.Issue issue;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mContentView = (TextView) view.findViewById(R.id.content);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
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
            case R.id.action_refresh:
                syncAvailableIssuesList(firstMissingIssueNumber);
                return true;

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
     * gets list of available issues from server.
     */
    void syncAvailableIssuesList(int firstMissingIssueNumber) {
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
                        File cacheDir = IssueListActivity.this.getExternalCacheDir();
                        File zipFile = File.createTempFile("issues-preview", ".zip", cacheDir);

                        FileOutputStream f = new FileOutputStream(zipFile);
                        f.write(response, 0, response.length);
                        f.close();

                        ZipHelper.unzip(zipFile, IssueListActivity.this.getExternalFilesDir(null));
                        zipFile.delete();

                        // get next bunch of available issues, until the saved list of issues remain unchanged
                        int firstMissingIssueNumber = findFirstMissingIssueNumber();
                        if (firstMissingIssueNumber > IssueListActivity.this.firstMissingIssueNumber) {
                            syncAvailableIssuesList(firstMissingIssueNumber);
                        }

                        magazines.loadIssues(getExternalFilesDir(null).getAbsolutePath());
                        IssueListActivity.this.firstMissingIssueNumber = firstMissingIssueNumber;

                        adapter.notifyDataSetChanged();
//                        setupRecyclerView((RecyclerView) IssueListActivity.this.findViewById(R.id.issue_list));

                    } catch (IOException e) {
                        LogHelper.e(TAG, e.getMessage());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
//                    LogHelper.e(TAG, error.getMessage()); // error
                    Toast.makeText(IssueListActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                    requestQueue.cancelAll(IssueListActivity.this);
                }
            }, null);

            request.setTag(this);

            // Add the request to the RequestQueue.
            requestQueue.add(request);

        } else {
            Toast.makeText(this, R.string.check_connection, Toast.LENGTH_SHORT).show();
            requestQueue.cancelAll(this);
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
        while (new File(getExternalFilesDir(null), "" + i).exists())
            i++;

        return i;
    }
}
