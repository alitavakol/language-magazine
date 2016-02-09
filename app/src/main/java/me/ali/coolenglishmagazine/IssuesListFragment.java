package me.ali.coolenglishmagazine;

import android.content.Context;
import android.graphics.BitmapFactory;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.ali.coolenglishmagazine.model.Magazines;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link IssuesListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link IssuesListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class IssuesListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
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
     * category of issues to include
     */
    public int filter;

    private OnFragmentInteractionListener mListener;

    public IssuesListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param filter specifies whether to show available issues or my issues
     * @return A new instance of fragment IssuesListFragment.
     */
    public static IssuesListFragment newInstance(int filter) {
        IssuesListFragment fragment = new IssuesListFragment();
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

        if (filter == AVAILABLE_ISSUES)
            setHasOptionsMenu(true);
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

    @Override
    public void onResume() {
        super.onResume();
        adapter.preNotifyDataSetChanged(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    protected IssuesRecyclerViewAdapter adapter;

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        adapter = new IssuesRecyclerViewAdapter();
        recyclerView.setAdapter(adapter);

        GridLayoutManager manager = new GridLayoutManager(getActivity(), 2);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? 2 : 1;
            }
        });
        recyclerView.setLayoutManager(manager);
        recyclerView.setHasFixedSize(true);
    }

    public class IssuesRecyclerViewAdapter extends RecyclerView.Adapter<IssuesRecyclerViewAdapter.ViewHolder> {
        private List<Magazines.Issue> issues = new ArrayList<>();

        // http://blog.sqisland.com/2014/12/recyclerview-grid-with-header.html
        private static final int ITEM_VIEW_TYPE_HEADER = 0;
        private static final int ITEM_VIEW_TYPE_ITEM = 1;

        public IssuesRecyclerViewAdapter() {
        }

        /**
         * calculates filtered list of issues, and then calls notifyDataSetChanged()
         *
         * @param success if true, data set has changed
         */
        public void preNotifyDataSetChanged(boolean success) {
            swipeContainer.setRefreshing(false);
            if (!success)
                return;

            issues = new ArrayList<>();
            switch (filter) {
                case MY_ISSUES:
                    for (Magazines.Issue issue : ((IssueListActivity) getActivity()).magazines.ISSUES) {
                        if (new File(issue.rootDirectory, Magazines.Issue.downloadedFileName).exists())
                            issues.add(issue);
                    }
                    break;

                case AVAILABLE_ISSUES:
                    for (Magazines.Issue issue : ((IssueListActivity) getActivity()).magazines.ISSUES) {
                        if (!(new File(issue.rootDirectory, Magazines.Issue.downloadedFileName).exists()))
                            issues.add(issue);
                    }
                    break;
            }

            Magazines.Issue.Status[] statuses = Magazines.Issue.Status.values();

            // true if header for the issue with the specified status is included in the array
            int[] isHeaderAdded = new int[statuses.length];

            for (Magazines.Issue issue : issues) {
                int status = issue.status.ordinal();
                isHeaderAdded[status]++;
            }

            // add header for items (if there is any item with that kind of status)
            for (Magazines.Issue.Status s : statuses) {
                int status = s.ordinal();
                if (isHeaderAdded[status] > 0 && (isHeaderAdded[status] < issues.size() || (s != Magazines.Issue.Status.available && s != Magazines.Issue.Status.other_saved))) {
                    Magazines.Issue h = new Magazines.Issue();
                    h.status = statuses[status - 1];
                    issues.add(h);
                }
            }

            // sorting with respect to issue status
            Collections.sort(issues, new Comparator<Magazines.Issue>() {
                @Override
                public int compare(Magazines.Issue issue1, Magazines.Issue issue2) {
                    return issue1.status.ordinal() - issue2.status.ordinal();
                }
            });

            notifyDataSetChanged();
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
            return issues.get(position).status.ordinal() % 2 == 0;
        }

        @Override
        public int getItemViewType(int position) {
            return isHeader(position) ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_ITEM;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final Magazines.Issue issue = issues.get(position);

            if (isHeader(position)) {
                ((TextView) holder.view.findViewById(R.id.headerTextView)).setText(getResources().obtainTypedArray(R.array.issue_list_header_titles).getString(issue.status.ordinal() / 2));
                return;
            }

            holder.titleTextView.setText(issue.title);
            holder.subtitleTextView.setText(issue.title);
            holder.posterImageView.setImageBitmap(BitmapFactory.decodeFile(new File(issue.rootDirectory, Magazines.Issue.posterFileName).getAbsolutePath()));

            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClicked(issue);
                }
            });
        }

        @Override
        public int getItemCount() {
            return issues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View view;
            public final TextView titleTextView, subtitleTextView;
            public final ImageView posterImageView;

            public ViewHolder(View view) {
                super(view);

                this.view = view;
                titleTextView = (TextView) view.findViewById(R.id.title);
                subtitleTextView = (TextView) view.findViewById(R.id.subtitle);
                posterImageView = (ImageView) view.findViewById(R.id.icon);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + titleTextView.getText() + "'";
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        /**
         * called when user selects an issue
         */
        void onItemClicked(Magazines.Issue issue);
    }

    /**
     * called when user pulls screen to refresh
     */
    @Override
    public void onRefresh() {
        swipeContainer.setRefreshing(true);
        ((IssueListActivity) getActivity()).syncAvailableIssuesList(-1, adapter);
    }

}
