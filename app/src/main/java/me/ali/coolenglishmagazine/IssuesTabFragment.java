package me.ali.coolenglishmagazine;

import android.app.DownloadManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.BitmapHelper;
import me.ali.coolenglishmagazine.util.LogHelper;


public class IssuesTabFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener,
        Magazines.Issue.OnStatusChangedListener,
        Magazines.OnDataSetChangedListener {

    private static final String TAG = LogHelper.makeLogTag(IssuesTabFragment.class);

    /**
     * tab filter argument name
     */
    private static final String ARG_FILTER = "filter";

    /**
     * "my issues" tab index
     */
    public static final int MY_ISSUES = 0;

    /**
     * "available for download" issues tab index
     */
    public static final int AVAILABLE_ISSUES = 1;

    /**
     * "completed" issues (either saved on device or available online) tab index
     */
    public static final int COMPLETED_ISSUES = 2;

    /**
     * category of issues to include
     */
    public int filter;

    public IssuesTabFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param filter specifies whether to show available issues or my issues
     * @return A new instance of fragment IssuesTabFragment.
     */
    public static IssuesTabFragment newInstance(int filter) {
        IssuesTabFragment fragment = new IssuesTabFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FILTER, filter);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filter = getArguments().getInt(ARG_FILTER);
        }

        if (filter == AVAILABLE_ISSUES) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.available_issues_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_refresh:
                onRefresh();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private SwipeRefreshLayout swipeContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_issues_list, container, false);

        swipeContainer = (SwipeRefreshLayout) v.findViewById(R.id.swipeContainer);
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        swipeContainer.setOnRefreshListener(this);
        swipeContainer.setEnabled(filter == AVAILABLE_ISSUES);

        final View recyclerView = v.findViewById(R.id.issue_list);
        setupRecyclerView((RecyclerView) recyclerView);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        swipeContainer.setRefreshing(false);
    }

    GalleryOfIssuesFragment galleryOfIssuesFragment;

    @Override
    public void onResume() {
        super.onResume();

        galleryOfIssuesFragment = (GalleryOfIssuesFragment) getActivity().getSupportFragmentManager().findFragmentByTag(GalleryOfIssuesFragment.FRAGMENT_TAG);

        Magazines.addOnDataSetChangedListener(this);
        adapter.preNotifyDataSetChanged(true, galleryOfIssuesFragment.magazines.ISSUES);
    }

    @Override
    public void onPause() {
        super.onPause();

        for (Magazines.Issue issue : issues) {
            if (issue2timer.containsKey(issue)) {
                issue2timer.get(issue).cancel();
                issue2timer.remove(issue);
            }
        }
        for (Magazines.Issue issue : galleryOfIssuesFragment.magazines.ISSUES) {
            issue.removeOnStatusChangedListener(this);
        }
        Magazines.removeOnDataSetChangedListener(this);
    }

    protected IssuesRecyclerViewAdapter adapter = new IssuesRecyclerViewAdapter();

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(adapter);

        GridLayoutManager manager = new GridLayoutManager(getContext(), 2);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? 2 : 1;
            }
        });
        recyclerView.setLayoutManager(manager);
        recyclerView.setHasFixedSize(true);
    }

    /**
     * progress bars update timer
     */
    static HashMap<Magazines.Issue, Timer> issue2timer = new HashMap<>();

    /**
     * a {@link ArrayList<me.ali.coolenglishmagazine.model.Magazines.Issue>} inherited class,
     * which sorts its items just after adding a new item.
     */
    private class AutoSortableList extends ArrayList<Magazines.Issue> {
        @Override
        public boolean add(Magazines.Issue issue) {
            boolean b = super.add(issue);

            // sorting with respect to issue status and id
            Collections.sort(this, new Comparator<Magazines.Issue>() {
                @Override
                public int compare(Magazines.Issue issue1, Magazines.Issue issue2) {
                    int comparison = issue1.getStatusValue() - issue2.getStatusValue();
                    if (comparison == 0)
                        comparison = issue1.id - issue2.id;
                    return comparison;
                }
            });

            return b;
        }
    }

    /**
     * list of issues shown in this fragment (filtered by value of filter)
     */
    private AutoSortableList issues = new AutoSortableList();

    /**
     * header rows, which are of type {@link me.ali.coolenglishmagazine.model.Magazines.Issue}, but with {@link me.ali.coolenglishmagazine.model.Magazines.Issue.Status} as a header.
     */
    private static Magazines.Issue[] headers;

    public class IssuesRecyclerViewAdapter extends RecyclerView.Adapter<IssuesRecyclerViewAdapter.ViewHolder> {
        // http://blog.sqisland.com/2014/12/recyclerview-grid-with-header.html
        private static final int ITEM_VIEW_TYPE_HEADER = 0;
        private static final int ITEM_VIEW_TYPE_ITEM = 1;

        public IssuesRecyclerViewAdapter() {
        }

        /**
         * calculates filtered list of issues, and then calls notifyDataSetChanged()
         *
         * @param issues_ changed issue(s) that leads to adapter update.
         * @param success if true, data set has changed. if false, only the refresh animation will stop.
         */
        public void preNotifyDataSetChanged(boolean success, Set<Magazines.Issue> issues_) {
            if (!success) {
                swipeContainer.setRefreshing(false);
                return;
            }

            boolean add = false;

            for (Magazines.Issue issue : issues_) {
                issue.addOnStatusChangedListener(IssuesTabFragment.this);

                Magazines.Issue.Status status = issue.getStatus();

                switch (filter) {
                    case MY_ISSUES:
                        add = status == Magazines.Issue.Status.other_saved || status == Magazines.Issue.Status.active;
                        break;

                    case AVAILABLE_ISSUES:
                        add = status == Magazines.Issue.Status.downloading || status == Magazines.Issue.Status.available;
                        break;

                    case COMPLETED_ISSUES:
                        add = status == Magazines.Issue.Status.completed;
                        break;
                }

                if (add) {
                    if (!issues.contains(issue)) {
                        issues.add(issue);
                        adapter.notifyItemInserted(issues.indexOf(issue));

                    } else {
                        adapter.notifyItemChanged(issues.indexOf(issue));
                    }

                } else if (issues.contains(issue)) {
                    int idx = issues.indexOf(issue);
                    issues.remove(issue);
                    adapter.notifyItemRemoved(idx);
                }
            }

            Magazines.Issue.Status[] statuses = Magazines.Issue.Status.values();

            if (headers == null) {
                headers = new Magazines.Issue[statuses.length / 2];
                for (int i = 0; i < headers.length; i++) {
                    headers[i] = new Magazines.Issue();
                    headers[i].setStatus(statuses[2 * i]);
                }
            }

            int addedHeaderCount = 0;
            boolean[] isHeaderAdded = new boolean[headers.length];
            for (int i = 0; i < headers.length; i++) {
                if (issues.contains(headers[i])) {
                    isHeaderAdded[i] = true;
                    addedHeaderCount++;
                }
            }

            int issuesCount = issues.size() - addedHeaderCount; // number of issues excluding headers

            // true if header for the specified status should be included in the array
            int[] shouldAddHeader = new int[headers.length];

            for (Magazines.Issue issue : issues) {
                int status = issue.getStatusValue();
                if (status % 2 == 1)
                    shouldAddHeader[status / 2]++;
            }

            // add header for items (if there is any item with that kind of status)
            for (int i = 0; i < headers.length; i++) {
                Magazines.Issue.Status status = statuses[2 * i];

                if (shouldAddHeader[i] > 0 && (shouldAddHeader[i] < issuesCount || (status != Magazines.Issue.Status.header_available && status != Magazines.Issue.Status.header_other_saved))) {
                    if (!isHeaderAdded[i]) {
                        final Magazines.Issue header = headers[i];
                        issues.add(header);
                        adapter.notifyItemInserted(issues.indexOf(header));
                    }

                } else if (isHeaderAdded[i]) {
                    final Magazines.Issue header = headers[i];
                    int idx = issues.indexOf(header);
                    issues.remove(header);
                    adapter.notifyItemRemoved(idx);
                }
            }
        }

        public void preNotifyDataSetChanged(Magazines.Issue issue) {
            Set<Magazines.Issue> issues = new HashSet<>();
            issues.add(issue);
            adapter.preNotifyDataSetChanged(true, issues);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case ITEM_VIEW_TYPE_HEADER:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.issue_list_header, parent, false);
                    break;
                case ITEM_VIEW_TYPE_ITEM:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.issue_list_row, parent, false);
                    break;
            }
            return new ViewHolder(view);
        }

        public boolean isHeader(int position) {
            return issues.get(position).getStatusValue() % 2 == 0;
        }

        @Override
        public int getItemViewType(int position) {
            return isHeader(position) ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_ITEM;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final Magazines.Issue issue = issues.get(position);

            if (isHeader(position)) {
                ((TextView) holder.itemView.findViewById(R.id.headerTextView)).setText(getResources().obtainTypedArray(R.array.issue_list_header_titles).getString(issue.getStatusValue() / 2));
                return;
            }

            if (!holder.titleTextView.getText().equals(issue.title)) {
                holder.titleTextView.setText(issue.title);
                holder.subtitleTextView.setText(issue.title);

                // http://stackoverflow.com/a/31162004
                holder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        // FIXME: load bitmap in an async task, http://developer.android.com/training/displaying-bitmaps/process-bitmap.html
                        int w = holder.itemView.getWidth();
                        int h = 4 * w / 3;
                        final Bitmap bitmap = BitmapHelper.decodeSampledBitmapFromFile(new File(issue.rootDirectory, Magazines.Issue.posterFileName).getAbsolutePath(), w, h);
                        holder.posterImageView.setImageBitmap(bitmap);
                    }
                });

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        galleryOfIssuesFragment.mListener.onIssueSelected(issue);
                    }
                });
            }

            final int status = Magazines.getDownloadStatus(getContext(), issue);
            boolean enableTimer = true;

            switch (status) {
                case DownloadManager.STATUS_PENDING:
                case DownloadManager.STATUS_PAUSED:
                    LogHelper.i(TAG, "pending");
                    holder.progressBar.setVisibility(View.VISIBLE);
                    if (!holder.progressBar.isIndeterminate()) {
                        holder.progressBar.setIndeterminate(true);
                        holder.progressBar.resetAnimation();
                    }
                    break;

                case DownloadManager.STATUS_RUNNING:
                    LogHelper.i(TAG, "running");
                    holder.progressBar.setVisibility(View.VISIBLE);
                    if (holder.progressBar.isIndeterminate()) {
                        holder.progressBar.setIndeterminate(false);
                        holder.progressBar.resetAnimation();
                    }
                    holder.progressBar.setProgress(holder.dl_progress);
                    break;

                case -3: // the issue is being extracted
                    LogHelper.i(TAG, "extracting");
                    holder.progressBar.setVisibility(View.VISIBLE);
                    if (!holder.progressBar.isIndeterminate()) {
                        holder.progressBar.setIndeterminate(true);
                        holder.progressBar.resetAnimation();
                    }
                    break;

                default:
                    LogHelper.i(TAG, "default");
                    holder.progressBar.setVisibility(View.GONE);
                    enableTimer = false;
                    break;
            }

            if (enableTimer) {
                Timer timer = IssuesTabFragment.issue2timer.get(issue);
                if (timer == null) {
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            holder.dl_progress = Magazines.getDownloadProgress(getContext(), issue);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    int position = holder.getAdapterPosition();
                                    if (position != RecyclerView.NO_POSITION)
                                        onBindViewHolder(holder, position);
                                    LogHelper.i(TAG, "dl_progress: " + holder.dl_progress);
                                }
                            });
                        }
                    }, 0, 3000);
                    IssuesTabFragment.issue2timer.put(issue, timer);
                    LogHelper.i(TAG, "timer created for ", issue.title);
                }

            } else {
                Timer timer = IssuesTabFragment.issue2timer.get(issue);
                if (timer != null) {
                    timer.cancel();
                    IssuesTabFragment.issue2timer.remove(issue);
                    LogHelper.i(TAG, "timer for ", issue.title, " cancelled");
                }
            }
        }

        @Override
        public int getItemCount() {
            return issues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView titleTextView, subtitleTextView;
            public final ImageView posterImageView;
            public final CircularProgressView progressBar;

            public int dl_progress;

            public ViewHolder(View view) {
                super(view);

                titleTextView = (TextView) view.findViewById(R.id.title);
                subtitleTextView = (TextView) view.findViewById(R.id.subtitle);
                posterImageView = (ImageView) view.findViewById(R.id.icon);
                progressBar = (CircularProgressView) view.findViewById(R.id.progress);
            }
        }
    }

    /**
     * called when user pulls screen to refresh
     */
    @Override
    public void onRefresh() {
        swipeContainer.setRefreshing(true);
        galleryOfIssuesFragment.syncAvailableIssuesList(-1, adapter);
    }

    public void onIssueStatusChanged(Magazines.Issue issue) {
        adapter.preNotifyDataSetChanged(issue);
        int pos = issues.indexOf(issue);
        if (pos != -1)
            adapter.notifyItemChanged(pos);
    }

    @Override
    public void onIssueAdded(Magazines.Issue issue) {
        adapter.preNotifyDataSetChanged(issue);
    }

    @Override
    public void onIssueRemoved(Magazines.Issue issue) {
        adapter.preNotifyDataSetChanged(issue);
    }

}
