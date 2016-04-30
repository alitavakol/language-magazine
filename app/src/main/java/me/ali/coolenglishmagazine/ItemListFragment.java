package me.ali.coolenglishmagazine;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.io.File;
import java.io.IOException;

import me.ali.coolenglishmagazine.broadcast_receivers.DownloadCompleteBroadcastReceiver;
import me.ali.coolenglishmagazine.model.MagazineContent;
import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.model.WaitingItems;
import me.ali.coolenglishmagazine.util.BitmapHelper;

/**
 * A list fragment representing a list of magazine items. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * TODO: fix the following line
 * currently being viewed in an item detail fragment ????.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ItemListFragment extends Fragment {

    protected MagazineContent magazineContent = new MagazineContent();
    protected Magazines.Issue issue;

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        void onItemSelected(MagazineContent.Item item);
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            issue = Magazines.getIssue(getActivity(), new File(getArguments().getString(IssueDetailActivity.ARG_ROOT_DIRECTORY)));
            magazineContent.loadItems(issue);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // this fragment wants to add menu items to action bar.
        setHasOptionsMenu(true);

        ((NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(DownloadCompleteBroadcastReceiver.ISSUE_DOWNLOADED_NOTIFICATION_ID + issue.id);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_item_list, container, false);

        recyclerView = (RecyclerView) v.findViewById(R.id.item_list);
        nColumns = getResources().getInteger(R.integer.items_column_count);
        setupRecyclerView();

        return v;
    }

    private RecyclerView recyclerView;

    /**
     * grid layout column count
     */
    private int nColumns;

    protected ItemsRecyclerViewAdapter adapter = new ItemsRecyclerViewAdapter();

    private void setupRecyclerView() {
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), nColumns));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new SpacesItemDecoration());
    }

    public class ItemsRecyclerViewAdapter extends RecyclerView.Adapter<ItemsRecyclerViewAdapter.ViewHolder> {
        public ItemsRecyclerViewAdapter() {

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final MagazineContent.Item item = magazineContent.ITEMS.get(position);

            int levelColor = getResources().getIntArray(R.array.levelColors)[item.level];
            final String level = getResources().getStringArray(R.array.levels)[item.level];

            // load down-sampled poster
            holder.itemView.post(new Runnable() {
                @Override
                public void run() {
                    int w = holder.itemView.getWidth();
                    int h = w / 2;
                    // TODO: load bitmap in an async task, http://developer.android.com/training/displaying-bitmaps/process-bitmap.html
                    final Bitmap bitmap = BitmapHelper.decodeSampledBitmapFromFile(new File(item.rootDirectory, item.posterFileName).getAbsolutePath(), w, h);
                    holder.posterImageView.setImageBitmap(bitmap);
                }
            });

            holder.textViewTitle.setText(item.title);
            holder.textViewType.setText(item.type);

            if (item.flagFileName != null && item.flagFileName.length() > 0) { // item has audio, so it has accent
                holder.flagImageView.setImageBitmap(BitmapFactory.decodeFile(new File(item.rootDirectory, item.flagFileName).getAbsolutePath()));

            } else { // no audio, hence accent flag becomes invisible
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.textViewType.getLayoutParams();
                params.removeRule(RelativeLayout.END_OF);
                params.addRule(RelativeLayout.ALIGN_START, R.id.title);
                holder.textViewType.setLayoutParams(params);
            }

            holder.textViewLevel.setText(level);
            holder.textViewLevel.setBackgroundColor(levelColor);

            holder.overflowImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(getActivity(), v);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.read_and_listen, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            int id = menuItem.getItemId();

                            switch (id) {
                                case R.id.action_add_to_waiting_list:
                                    WaitingItems.appendToWaitingList(getActivity(), item);
                                    return true;
                            }

                            return false;
                        }
                    });
                }
            });

            ((ViewGroup) holder.itemView).getChildAt(0).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallbacks.onItemSelected(magazineContent.ITEMS.get(recyclerView.getChildAdapterPosition(holder.itemView)));
                }
            });
        }

        @Override
        public int getItemCount() {
            return magazineContent.ITEMS.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textViewTitle, textViewType, textViewLevel;
            final ImageView flagImageView, posterImageView;
            final ImageButton overflowImageButton;

            public ViewHolder(View view) {
                super(view);

                textViewTitle = (TextView) view.findViewById(R.id.title);
                textViewType = (TextView) view.findViewById(R.id.type);
                flagImageView = (ImageView) view.findViewById(R.id.flag);
                textViewLevel = (TextView) view.findViewById(R.id.level);
                overflowImageButton = (ImageButton) view.findViewById(R.id.overflowMenu);
                posterImageView = (ImageView) view.findViewById(R.id.poster);

                overflowImageButton.setImageDrawable(new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_more_vert).sizeDp(24).paddingDp(4).colorRes(R.color.primary_light));
            }
        }
    }

    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int hMargin, vMargin, spacing;

        public SpacesItemDecoration() {
            hMargin = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
            vMargin = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
            spacing = getResources().getDimensionPixelSize(R.dimen.spacing_normal);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int spanIndex = ((GridLayoutManager.LayoutParams) view.getLayoutParams()).getSpanIndex();
            final int position = recyclerView.getChildAdapterPosition(view);

            outRect.left = spanIndex == 0 ? hMargin : spacing / 2;
            outRect.right = spanIndex == nColumns - 1 ? hMargin : spacing / 2;
            outRect.top = position < nColumns ? vMargin : spacing / 2;
            outRect.bottom = position >= nColumns * (adapter.getItemCount() / nColumns) ? vMargin : spacing / 2;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException(activity.toString() + " must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.item_list, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final Magazines.Issue.Status status = issue.getStatus();
        menu.findItem(R.id.action_mark_complete).setVisible(status != Magazines.Issue.Status.completed);
        menu.findItem(R.id.action_mark_incomplete).setVisible(status == Magazines.Issue.Status.completed);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = menuItem.getItemId();

        switch (id) {
            case R.id.action_add_to_waiting_list:
                for (MagazineContent.Item item : magazineContent.ITEMS)
                    WaitingItems.appendToWaitingList(getActivity(), item);
                return true;

            case R.id.action_open_issue_details:
                getActivity().finish();

                Intent intent = new Intent(getActivity(), IssueDetailActivity.class);
                intent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());
                startActivity(intent);
                return true;

            case R.id.action_mark_complete:
                Magazines.markCompleted(issue);
                return true;

            case R.id.action_mark_incomplete:
                Magazines.reopen(getActivity(), issue);
                return true;
        }

        return super.onOptionsItemSelected(menuItem);
    }

}
