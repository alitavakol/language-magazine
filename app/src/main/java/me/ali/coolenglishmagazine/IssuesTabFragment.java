package me.ali.coolenglishmagazine;

import android.app.DownloadManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.FontManager;
import me.ali.coolenglishmagazine.util.LogHelper;


public class IssuesTabFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener,
        Magazines.Issue.OnStatusChangedListener,
        Magazines.OnDataSetChangedListener,
        RecyclerView.OnItemTouchListener,
        ActionMode.Callback {

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
        if (isAdded())
            menu.findItem(R.id.action_refresh).setIcon(new IconicsDrawable(getContext(), GoogleMaterial.Icon.gmd_refresh).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text));
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
        swipeContainer.setColorSchemeResources(R.color.accent);
        swipeContainer.setOnRefreshListener(this);
        swipeContainer.setEnabled(filter == AVAILABLE_ISSUES);

        recyclerView = (RecyclerView) v.findViewById(R.id.issue_list);
        nColumns = getResources().getInteger(R.integer.issues_column_count);
        setupRecyclerView();

        return v;
    }

    private RecyclerView recyclerView;

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
        while (adapter.preNotifyDataSetChanged(true, galleryOfIssuesFragment.magazines.ISSUES))
            adapter.ignoreItemChanged = true;
        adapter.ignoreItemChanged = false;

        updateHelpContainer();
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

    /**
     * grid layout column count
     */
    int nColumns;

    private void setupRecyclerView() {
        recyclerView.setAdapter(adapter);

        GridLayoutManager manager = new GridLayoutManager(getContext(), nColumns);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? nColumns : 1;
            }
        });
        recyclerView.setLayoutManager(manager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new SpacesItemDecoration());

        recyclerView.addOnItemTouchListener(this);
        gestureDetector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());

        int hMargin = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        int spacing = getResources().getDimensionPixelSize(R.dimen.spacing_normal);
        recyclerView.setPadding(hMargin - spacing / 2, 0, hMargin - spacing / 2, 0);
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

        /**
         * adds the specified object in the correct position, according to sort rules:
         * issues with lower status come first. then issues with lower IDs come first,
         *
         * @param issue object to insert into list
         * @return object position in the list
         */
        public int addAndSort(Magazines.Issue issue) {
            int issueStatusValue = issue.getStatusValue();
            int issueId = issue.id;
            int i;

            for (i = 0; i < size(); i++) {
                final Magazines.Issue issue2 = get(i);
                int issue2StatusValue = issue2.getStatusValue();

                if (issue2StatusValue > issueStatusValue
                        || (issue2StatusValue == issueStatusValue && issue2.id > issueId))
                    break;
            }
            add(i, issue);

            return i;
        }
    }

    /**
     * list of issues shown in this fragment (filtered by value of filter)
     */
    private AutoSortableList issues = new AutoSortableList();

    /**
     * header rows, which are of type {@link me.ali.coolenglishmagazine.model.Magazines.Issue}, but with an even {@link Magazines.Issue#status} value.
     */
    private static Magazines.Issue[] headers;

    public class IssuesRecyclerViewAdapter extends RecyclerView.Adapter<IssuesRecyclerViewAdapter.ViewHolder> {
        private SparseBooleanArray selectedItems = new SparseBooleanArray();

        // http://blog.sqisland.com/2014/12/recyclerview-grid-with-header.html
        private static final int ITEM_VIEW_TYPE_HEADER = 0;
        private static final int ITEM_VIEW_TYPE_ITEM = 1;

        public IssuesRecyclerViewAdapter() {
        }

        /**
         * prevents redundantly notifying update of the same item from onResume()
         */
        boolean ignoreItemChanged = false;

        /**
         * calculates filtered list of issues, and then calls notifyDataSetChanged()
         *
         * @param issues_ changed issue(s) that leads to adapter update.
         * @param success if true, data set has changed. if false, only the refresh animation will stop.
         * @return true if a structural change occurs in items (excluding headers)
         */
        public boolean preNotifyDataSetChanged(boolean success, Set<Magazines.Issue> issues_) {
            if (!success) {
                swipeContainer.setRefreshing(false);
                return false;
            }

            boolean add = false;
            boolean changed = false;

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
                        int idx = issues.addAndSort(issue);
                        adapter.notifyItemInserted(idx);
                        changed = true;

                    } else {
                        int fromPosition = issues.indexOf(issue);

                        if (!ignoreItemChanged)
                            // notify item status change
                            adapter.notifyItemChanged(fromPosition);

                        // status change results in change in item order (structural change).
                        // sort again.
                        issues.remove(fromPosition);
                        int toPosition = issues.addAndSort(issue); // add again, which inserts it in correct sorted position.

                        if (fromPosition != toPosition) {
                            adapter.notifyItemMoved(fromPosition, toPosition);
                            changed = true;
                        }
                    }

                } else if (issues.contains(issue)) {
                    int idx = issues.indexOf(issue);
                    issues.remove(issue);
                    adapter.notifyItemRemoved(idx);
                    changed = true;
                }
            }

            if (updateHeaders() || changed) {
                recyclerView.invalidateItemDecorations();
                galleryOfIssuesFragment.finishActionMode();
                updateHelpContainer();
            }

            return changed;
        }

        Magazines.Issue.Status[] statuses = Magazines.Issue.Status.values();

        /**
         * shows whether or not each header is added and visible.
         */
        boolean[] isHeaderAdded;

        /**
         * number of header rows currently added to list and visible.
         */
        int addedHeaderCount;

        /**
         * count of issues of some status value
         */
        int[] shouldAddHeader = new int[Magazines.Issue.Status.values().length / 2];

        /**
         * adds/removes header rows to the list, according to the structure and status values.
         *
         * @return true if headers changed
         */
        protected boolean updateHeaders() {
            if (headers == null) {
                headers = new Magazines.Issue[statuses.length / 2];
                for (int i = 0; i < headers.length; i++) {
                    headers[i] = new Magazines.Issue();
                    headers[i].setStatus(statuses[2 * i]);
                }
            }

            if (isHeaderAdded == null)
                isHeaderAdded = new boolean[headers.length];

            int issuesCount = issues.size() - addedHeaderCount; // number of issues excluding headers

            // count issues with the same status value
            for (int i = shouldAddHeader.length - 1; i >= 0; i--)
                shouldAddHeader[i] = 0;
            for (Magazines.Issue issue : issues) {
                int status = issue.getStatusValue();
                if (status % 2 == 1)
                    shouldAddHeader[status / 2]++;
            }

            boolean changed = false;

            // add header for items (if there is any item with that kind of status)
            for (int i = 0; i < headers.length; i++) {
                Magazines.Issue.Status status = statuses[2 * i];

                if (shouldAddHeader[i] > 0 && (shouldAddHeader[i] < issuesCount || (status != Magazines.Issue.Status.header_available && status != Magazines.Issue.Status.header_other_saved && status != Magazines.Issue.Status.header_completed))) {
                    if (!isHeaderAdded[i]) {
                        final Magazines.Issue header = headers[i];
                        int idx = issues.addAndSort(header);
                        adapter.notifyItemInserted(idx);
                        changed = true;

                        isHeaderAdded[i] = true;
                        addedHeaderCount++;
                    }

                } else if (isHeaderAdded[i]) {
                    final Magazines.Issue header = headers[i];
                    int idx = issues.indexOf(header);
                    issues.remove(header);
                    adapter.notifyItemRemoved(idx);
                    changed = true;

                    isHeaderAdded[i] = false;
                    addedHeaderCount--;
                }
            }

            return changed;
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
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final Magazines.Issue issue = issues.get(position);

            if (isHeader(position)) {
                ((TextView) holder.itemView.findViewById(R.id.headerTextView)).setText(getResources().obtainTypedArray(R.array.issue_list_header_titles).getString(issue.getStatusValue() / 2));
                return;
            }

            if (!holder.titleTextView.getText().equals(issue.title)) {
                holder.titleTextView.setText(issue.title);
                holder.subtitleTextView.setText(issue.subtitle);

                // postpone image setup to be done later in main thread,
                // because we need width and height of the posterImageView,
                holder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        int w = holder.itemView.getWidth();
                        int h = 4 * w / 3;

                        Picasso
                                .with(holder.itemView.getContext())
                                .load(new File(issue.rootDirectory, Magazines.Issue.posterFileName))
                                .resize(w, h)
                                .centerCrop()
                                .into(holder.posterImageView);
                    }
                });

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (galleryOfIssuesFragment.actionMode == null && !preventOnItemClickListener)
                            galleryOfIssuesFragment.mListener.onIssueSelected(issue);
                        preventOnItemClickListener = false;
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

            if (selectedItems.get(position, false)) {
                holder.checkMarkImageView.setVisibility(View.VISIBLE);
            } else {
                holder.checkMarkImageView.setVisibility(View.GONE);
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
            public final ImageView checkMarkImageView;

            public int dl_progress;

            public ViewHolder(View view) {
                super(view);

                titleTextView = (TextView) view.findViewById(R.id.title);
                subtitleTextView = (TextView) view.findViewById(R.id.subtitle);
                posterImageView = (ImageView) view.findViewById(R.id.icon);
                progressBar = (CircularProgressView) view.findViewById(R.id.progress);

                checkMarkImageView = (ImageView) view.findViewById(R.id.check_mark);
                if (checkMarkImageView != null)
                    checkMarkImageView.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_check).sizeDp(36).paddingRes(R.dimen.padding_normal).colorRes(R.color.primary));
            }
        }

        public void toggleSelection(int pos) {
            if (selectedItems.get(pos, false)) {
                selectedItems.delete(pos);
            } else {
                selectedItems.put(pos, true);
            }
            notifyItemChanged(pos);
        }

        public void clearSelections() {
            SparseBooleanArray selectedItems = this.selectedItems.clone();
            this.selectedItems.clear();
            for (int i = selectedItems.size() - 1; i >= 0; i--)
                notifyItemChanged(selectedItems.keyAt(i));
        }

        public int getSelectedItemsCount() {
            return selectedItems.size();
        }

        public List<Integer> getSelectedItems() {
            List<Integer> items = new ArrayList<>(selectedItems.size());
            for (int i = 0; i < selectedItems.size(); i++) {
                items.add(selectedItems.keyAt(i));
            }
            return items;
        }
    }

    private class RecyclerViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (galleryOfIssuesFragment.actionMode != null) {
                View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
                final int position = recyclerView.getChildAdapterPosition(view);
                if (position != RecyclerView.NO_POSITION && adapter.getItemViewType(position) != IssuesRecyclerViewAdapter.ITEM_VIEW_TYPE_HEADER)
                    toggleSelection(position);
            }
            return super.onSingleTapConfirmed(e);
        }

        public void onLongPress(MotionEvent e) {
            if (galleryOfIssuesFragment.actionMode != null)
                return;

            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            int idx = recyclerView.getChildAdapterPosition(view);
            if (idx == RecyclerView.NO_POSITION || adapter.getItemViewType(idx) == IssuesRecyclerViewAdapter.ITEM_VIEW_TYPE_HEADER)
                return;

            // Start the CAB using the ActionMode.Callback defined above
            galleryOfIssuesFragment.actionMode = getActivity().startActionMode(IssuesTabFragment.this);
            toggleSelection(idx);

            super.onLongPress(e);
        }
    }

    private void toggleSelection(int idx) {
        adapter.toggleSelection(idx);

        final int selectedItemsCount = adapter.getSelectedItemsCount();
        if (selectedItemsCount > 0) {
            final String title = getString(R.string.selected_count, selectedItemsCount);
            galleryOfIssuesFragment.actionMode.setTitle(title);

        } else { // last selected item just unselected. finish action mode.
            preventOnItemClickListener = true;
            galleryOfIssuesFragment.actionMode.finish();
        }
    }

    /**
     * prevents listening to onItemClick when action mode is finishing
     */
    boolean preventOnItemClickListener;

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.issues_list_action_mode, menu);

        if (filter == 1 || filter == 2)
            menu.findItem(R.id.action_download).setIcon(new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_file_download).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text)).setVisible(true).setVisible(true).setShowAsActionFlags(filter == 2 ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_ALWAYS);

        if (filter == 0) {
            menu.findItem(R.id.action_mark_complete).setIcon(new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_thumb_up).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text)).setVisible(true);
            menu.findItem(R.id.action_delete).setIcon(new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_delete).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text)).setVisible(true);
        }

        if (filter == 2) {
            menu.findItem(R.id.action_mark_incomplete).setIcon(new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_thumb_down).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text)).setVisible(true);
            menu.findItem(R.id.action_free).setIcon(new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_disc_full).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text)).setVisible(true).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        if (filter == 1) {
            menu.findItem(R.id.action_cancel).setIcon(new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_clear).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text)).setVisible(true).setVisible(true).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.findItem(R.id.action_delete_permanently).setIcon(new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_delete_forever).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text)).setVisible(true).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        List<Integer> selectedItemPositions = adapter.getSelectedItems();

        Magazines.Issue[] selectedIssues = new Magazines.Issue[selectedItemPositions.size()];
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--)
            selectedIssues[i] = issues.get(selectedItemPositions.get(i));

        switch (menuItem.getItemId()) {
            case R.id.action_delete:
            case R.id.action_cancel:
            case R.id.action_free:
                for (Magazines.Issue issue : selectedIssues)
                    Magazines.deleteIssue(getActivity(), issue, false);
                break;

            case R.id.action_delete_permanently:
                for (Magazines.Issue issue : selectedIssues)
                    Magazines.deleteIssue(getActivity(), issue, true);
                break;

            case R.id.action_mark_complete:
                for (Magazines.Issue issue : selectedIssues)
                    Magazines.markCompleted(issue);
                break;

            case R.id.action_mark_incomplete:
                for (Magazines.Issue issue : selectedIssues)
                    Magazines.reopen(getActivity(), issue);
                break;

            case R.id.action_download:
                for (Magazines.Issue issue : selectedIssues) {
                    try {
                        if (!(new File(issue.rootDirectory, Magazines.Issue.downloadedFileName).exists()))
                            Magazines.download(getActivity(), issue);
                    } catch (IOException e) {
                        LogHelper.e(TAG, e.getMessage());
                    }
                }
                break;

            default:
                return false;
        }

        actionMode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        galleryOfIssuesFragment.actionMode = null;
        adapter.clearSelections();
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean b) {
    }

    /**
     * called when user pulls screen to refresh
     */
    @Override
    public void onRefresh() {
        swipeContainer.setRefreshing(true);
        galleryOfIssuesFragment.syncAvailableIssuesList(getActivity(), -1, adapter);
    }

    public void onIssueStatusChanged(Magazines.Issue issue) {
        adapter.preNotifyDataSetChanged(issue);
    }

    @Override
    public void onIssueAdded(Magazines.Issue issue) {
        adapter.preNotifyDataSetChanged(issue);
    }

    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int vMargin, spacing2;

        public SpacesItemDecoration() {
            vMargin = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
            spacing2 = getResources().getDimensionPixelSize(R.dimen.spacing_normal) / 2;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            final int position = recyclerView.getChildAdapterPosition(view);

            final GridLayoutManager.LayoutParams params = (GridLayoutManager.LayoutParams) view.getLayoutParams();
            int spanSize = params.getSpanSize();

            outRect.left = spacing2;
            outRect.right = spacing2;

            if (spanSize == nColumns) {
                outRect.bottom = spacing2;
                outRect.top = position == 0 ? vMargin : 0;

            } else {
                outRect.top = adapter.addedHeaderCount > 0 || position >= nColumns ? spacing2 : vMargin;
                outRect.bottom = position == adapter.getItemCount() - 1 || issues.get(position + 1).getStatusValue() % 2 == 0 ? vMargin : spacing2;
            }
        }
    }

    GestureDetectorCompat gestureDetector;

    /**
     * shows help if adapter has no items
     */
    protected void updateHelpContainer() {
        final View layoutView = getView();
        if (layoutView == null)
            return;

        final View helpContainer = layoutView.findViewById(R.id.help_container);

        if (adapter.getItemCount() > 0) {
            helpContainer.setVisibility(View.GONE);
            return;
        }

        helpContainer.setVisibility(View.VISIBLE);

        if (getResources().getConfiguration().locale.getLanguage().equals("fa"))
            FontManager.markAsIconContainer(helpContainer, FontManager.getTypeface(getActivity(), FontManager.ADOBE_ARABIC_REGULAR));

        final ImageButton imageButton = (ImageButton) layoutView.findViewById(R.id.add);
        final TextView helpTextView = (TextView) layoutView.findViewById(R.id.help);

        switch (filter) {
            case 0:
                imageButton.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_file_download).sizeDp(72).colorRes(R.color.colorAccentTransparent));
                helpTextView.setText(R.string.about_saved_issues);
                break;

            case 1:
                imageButton.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_refresh).sizeDp(72).colorRes(R.color.colorAccentTransparent));
                helpTextView.setText(R.string.about_available_issues);
                break;

            case 2:
                imageButton.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_assignment_turned_in).sizeDp(72).colorRes(R.color.colorAccentTransparent));
                helpTextView.setText(R.string.about_completed_issues);
                break;
        }

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (filter) {
                    case 0:
                        galleryOfIssuesFragment.viewPager.setCurrentItem(AVAILABLE_ISSUES, true);
                        break;
                    case 1:
                        onRefresh();
                        break;
                }
            }
        });
    }

}
