package me.ali.coolenglishmagazine;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.PopupMenu;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import me.ali.coolenglishmagazine.broadcast_receivers.DownloadCompleteBroadcastReceiver;
import me.ali.coolenglishmagazine.model.MagazineContent;
import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.BitmapHelper;
import me.ali.coolenglishmagazine.util.FontManager;

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
public class ItemListFragment extends ListFragment {

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
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

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
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(MagazineContent.Item item) {
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemListFragment() {
    }

    Typeface levelTypeface, titleTypeface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            issue = Magazines.getIssue(getActivity(), new File(getArguments().getString(IssueDetailActivity.ARG_ROOT_DIRECTORY)));
            magazineContent.loadItems(issue);

        } catch (IOException e) {
        }

        levelTypeface = FontManager.getTypeface(getActivity(), FontManager.BOOSTER_ITALIC);
        titleTypeface = FontManager.getTypeface(getActivity(), FontManager.BOOSTER_BOLD);

        // TODO: replace with a real list adapter.
        setListAdapter(new Adapter());

        ((NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(DownloadCompleteBroadcastReceiver.ISSUE_DOWNLOADED_NOTIFICATION_ID + issue.id);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }

        getListView().setDivider(null);
        getListView().setDividerHeight(16);
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
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(magazineContent.ITEMS.get(position));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    public class Adapter extends BaseAdapter {
        private LayoutInflater inflater = null;

        public Adapter() {
            inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return magazineContent.ITEMS.size();
        }

        public Object getItem(int position) {
            return magazineContent.ITEMS.get(position);
        }

        public long getItemId(int position) {
            return magazineContent.ITEMS.get(position).id;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final MagazineContent.Item item = magazineContent.ITEMS.get(position);

            View vi = convertView;
            if (convertView == null)
                vi = inflater.inflate(R.layout.item_list_row, null);

            int color = getResources().getIntArray(R.array.levelColors)[item.level];
            int transparentColor = Color.argb(200, Color.red(color), Color.green(color), Color.blue(color));
//            int moreTransparentColor = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color));
//            int levelColor = getResources().getIntArray(R.array.levelColors)[item.level];

            // load downsampled poster
            int w = parent.getMinimumWidth();
            int h = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());
            // TODO: load bitmap in an async task, http://developer.android.com/training/displaying-bitmaps/process-bitmap.html
            final Bitmap bitmap = BitmapHelper.decodeSampledBitmapFromFile(new File(item.rootDirectory, item.posterFileName).getAbsolutePath(), w, h);
            ((ImageView) vi.findViewById(R.id.poster)).setImageBitmap(bitmap);

            final TextView textViewTitle = (TextView) vi.findViewById(R.id.title);
            textViewTitle.setText(item.title);
            textViewTitle.setTypeface(titleTypeface);
//            textViewTitle.setBackgroundColor(transparentColor);

            final TextView textViewType = (TextView) vi.findViewById(R.id.type);
            textViewType.setText(item.type);
//            textViewType.setTextColor(levelColor);

            final ImageView flagImageView = (ImageView) vi.findViewById(R.id.flag);
            if (item.flagFileName != null && item.flagFileName.length() > 0) {
                flagImageView.setImageBitmap(BitmapFactory.decodeFile(new File(item.rootDirectory, item.flagFileName).getAbsolutePath()));
            } else {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) textViewType.getLayoutParams();
                params.removeRule(RelativeLayout.END_OF);
                params.addRule(RelativeLayout.ALIGN_START, R.id.title);
                textViewType.setLayoutParams(params);
            }

            final TextView textViewLevel = (TextView) vi.findViewById(R.id.level);
            final String level = getResources().getStringArray(R.array.levels)[item.level];
            textViewLevel.setText(level);
//            textViewLevel.setTextColor(levelColor);
            textViewLevel.setBackgroundColor(transparentColor);
            textViewLevel.setTypeface(levelTypeface);

            final View overflowButton = vi.findViewById(R.id.overflowMenu);
            overflowButton.setOnClickListener(new View.OnClickListener() {
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
                                case R.id.add_to_waiting_list:
                                    WaitingListFragment.appendToWaitingList(getActivity(), item);
                                    return true;
                            }

                            return false;
                        }
                    });
                }
            });

            return vi;
        }
    }

}
