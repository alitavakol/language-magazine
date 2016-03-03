package me.ali.coolenglishmagazine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.ali.coolenglishmagazine.model.MagazineContent;
import me.ali.coolenglishmagazine.model.WaitingItems;
import me.ali.coolenglishmagazine.util.BitmapHelper;


public class WaitingListFragment extends Fragment implements
        RecyclerView.OnItemTouchListener,
        ActionMode.Callback,
        WaitingItems.OnWaitingItemChangedListener {

    public WaitingListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WaitingListFragment.
     */
    public static WaitingListFragment newInstance() {
        WaitingListFragment fragment = new WaitingListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//        }
//    }

    RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_waiting_list, container, false);

        recyclerView = (RecyclerView) v.findViewById(R.id.waiting_list);
        setupRecyclerView(recyclerView);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        WaitingItems.importWaitingItems(getActivity());
        adapter.notifyDataSetChanged();
        WaitingItems.listener = this;
    }

    @Override
    public void onStop() {
        super.onStop();
        WaitingItems.listener = null;
    }

    protected WaitingListRecyclerViewAdapter adapter = new WaitingListRecyclerViewAdapter();

    /**
     * item drag and swipe helper
     */
    private ItemTouchHelper itemTouchHelper;

    /**
     * prevent switching to action mode when user is dragging and item.
     */
    boolean itemIsDragging = false;

    GestureDetectorCompat gestureDetector;

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        ItemTouchHelper.Callback callback = new WaitingItemTouchHelper();
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.addOnItemTouchListener(this);
        gestureDetector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
    }

    public class WaitingListRecyclerViewAdapter extends RecyclerView.Adapter<WaitingListRecyclerViewAdapter.ViewHolder> {
        private SparseBooleanArray selectedItems = new SparseBooleanArray();

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.waiting_list_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final WaitingItems.WaitingItem waitingItem = WaitingItems.waitingItems.get(position);

            try {
                MagazineContent.Item item = MagazineContent.getItem(waitingItem.itemRootDirectory);

                holder.titleTextView.setText(item.title);

                final Context context = getActivity();
                int repeatCount = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("repeat_count", "8"));
                holder.hitCountTextView.setText(context.getResources().getString(R.string.pending_count, repeatCount - waitingItem.hitCount));

                // load downsampled poster
                int w = holder.posterImageView.getMaxWidth();
                int h = holder.posterImageView.getMaxHeight();
                // TODO: load bitmap in an async task, http://developer.android.com/training/displaying-bitmaps/process-bitmap.html
                final Bitmap bitmap = BitmapHelper.decodeSampledBitmapFromFile(new File(item.rootDirectory, item.posterFileName).getAbsolutePath(), w, h);
                holder.posterImageView.setImageBitmap(bitmap);

                holder.handleView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                            itemTouchHelper.startDrag(holder);
                            itemIsDragging = true;
                        }
                        return false;
                    }
                });

                // hide handler in action mode
                holder.handleView.setVisibility(actionMode == null ? View.VISIBLE : View.INVISIBLE);

                if (selectedItems.get(position, false)) {
                    holder.checkMarkImageView.setVisibility(View.VISIBLE);
                } else {
                    holder.checkMarkImageView.setVisibility(View.INVISIBLE);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return WaitingItems.waitingItems.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView, hitCountTextView;
            ImageView handleView, checkMarkImageView, posterImageView;

            public ViewHolder(View view) {
                super(view);

                titleTextView = (TextView) view.findViewById(R.id.title);
                posterImageView = (ImageView) view.findViewById(R.id.poster);
                hitCountTextView = (TextView) view.findViewById(R.id.hit_count);

                handleView = (ImageView) view.findViewById(R.id.handle);
                handleView.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_reorder).sizeDp(20).color(Color.LTGRAY));
                handleView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getActivity(), R.string.drag_hint, Toast.LENGTH_SHORT).show();
                    }
                });

                checkMarkImageView = (ImageView) view.findViewById(R.id.check_mark);
                checkMarkImageView.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_check).sizeDp(20).color(Color.LTGRAY));

                view.findViewById(R.id.title_container).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (actionMode == null)
                            Toast.makeText(getActivity(), R.string.action_mode_hint, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        public void remove(int position) {
            WaitingItems.waitingItems.remove(position);
            WaitingItems.saveWaitingItems(getActivity());
            notifyItemRemoved(position);
        }

        public void swap(int firstPosition, int secondPosition) {
            Collections.swap(WaitingItems.waitingItems, firstPosition, secondPosition);
            WaitingItems.saveWaitingItems(getActivity());
            adapter.notifyItemMoved(firstPosition, secondPosition);
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
            selectedItems.clear();
            notifyDataSetChanged();
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

    public class WaitingItemTouchHelper extends ItemTouchHelper.Callback {
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            adapter.swap(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            adapter.remove(viewHolder.getAdapterPosition());
        }
    }

    /**
     * when user long presses an item, action mode is turned on.
     */
    ActionMode actionMode;

    private class RecyclerViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (actionMode != null) {
                View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
                toggleSelection(recyclerView.getChildAdapterPosition(view));
            }
            return super.onSingleTapConfirmed(e);
        }

        public void onLongPress(MotionEvent e) {
            if (itemIsDragging) {
                itemIsDragging = false;
                return;
            }
            if (actionMode != null)
                return;

            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());

            // Start the CAB using the ActionMode.Callback defined above
            actionMode = getActivity().startActionMode(WaitingListFragment.this);
            int idx = recyclerView.getChildAdapterPosition(view);
            toggleSelection(idx);

            // hide handler when in action mode
            adapter.notifyItemRangeChanged(0, adapter.getItemCount());

            super.onLongPress(e);
        }
    }

    private void toggleSelection(int idx) {
        adapter.toggleSelection(idx);
        String title = getString(R.string.selected_count, adapter.getSelectedItemsCount());
        actionMode.setTitle(title);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.waiting_list_action_mode, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                List<Integer> selectedItemPositions = adapter.getSelectedItems();
                int currPos;
                for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                    currPos = selectedItemPositions.get(i);
                    WaitingItems.waitingItems.remove(currPos);
                    adapter.notifyItemRemoved(currPos);
                }
                actionMode.finish();
                WaitingItems.saveWaitingItems(getActivity());
                return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        this.actionMode = null;
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

    @Override
    public void onWaitingItemHitCountChanged(WaitingItems.WaitingItem waitingItem) {
        adapter.notifyItemChanged(WaitingItems.waitingItems.indexOf(waitingItem));
    }

    @Override
    public void onWaitingItemRemoved(WaitingItems.WaitingItem waitingItem) {
        adapter.notifyItemRemoved(WaitingItems.waitingItems.indexOf(waitingItem));
    }
}
