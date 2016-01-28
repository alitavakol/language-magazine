package me.ali.coolenglishmagazine;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import me.ali.coolenglishmagazine.broadcast_receivers.DownloadCompleteBroadcastReceiver;
import me.ali.coolenglishmagazine.model.MagazineContent;
import me.ali.coolenglishmagazine.model.Magazines;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            issue = Magazines.getIssue(new File(getArguments().getString(IssueDetailActivity.ARG_ROOT_DIRECTORY)));
            magazineContent.loadItems(issue);

        } catch (IOException e) {
        }

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

            ((ImageView) vi.findViewById(R.id.poster)).setImageBitmap(BitmapFactory.decodeFile(new File(item.rootDirectory, item.posterFileName).getAbsolutePath()));
            ((TextView) vi.findViewById(R.id.title)).setText(item.title);
            ((TextView) vi.findViewById(R.id.type)).setText(item.type);
            if(item.flagFileName != null && item.flagFileName.length() > 0)
                ((ImageView) vi.findViewById(R.id.flag)).setImageBitmap(BitmapFactory.decodeFile(new File(item.rootDirectory, item.flagFileName).getAbsolutePath()));
            ((TextView) vi.findViewById(R.id.type)).setTextColor(getResources().getIntArray(R.array.levelColors)[item.level]);
            ((TextView) vi.findViewById(R.id.itemNo)).setText(Integer.toString(item.id));

            int color = getResources().getIntArray(R.array.levelColors)[item.level];
            int transparentColor = Color.argb(200, Color.red(color), Color.green(color), Color.blue(color));
            vi.findViewById(R.id.title).setBackgroundColor(transparentColor);

            return vi;
        }
    }
}
