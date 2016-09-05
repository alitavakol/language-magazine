package me.ali.coolenglishmagazine;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.ali.coolenglishmagazine.model.MagazineContent;
import me.ali.coolenglishmagazine.model.WaitingItems;


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

    RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_waiting_list, container, false);

        recyclerView = (RecyclerView) v.findViewById(R.id.waiting_list);
        setupRecyclerView(recyclerView);

        adapterDataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                updateHelpContainer(v);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                updateHelpContainer(v);
            }
        };
        adapter.registerAdapterDataObserver(adapterDataObserver);

        fabLeft = (FloatingActionButton) v.findViewById(R.id.fab_left);
        fabLeft.setImageDrawable(new IconicsDrawable(getContext(), GoogleMaterial.Icon.gmd_keyboard_arrow_left).sizeDp(24).colorRes(R.color.md_dark_primary_text));
        fabLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                coolEnglishTimesFragment.viewPager.setCurrentItem(0);
            }
        });

        return v;
    }

    public FloatingActionButton fabLeft;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter.unregisterAdapterDataObserver(adapterDataObserver);
    }

    RecyclerView.AdapterDataObserver adapterDataObserver;

    protected void updateHelpContainer(View layoutView) {
        final View helpContainer = layoutView.findViewById(R.id.help_container);

        if (adapter.getItemCount() > 0) {
            helpContainer.setVisibility(View.GONE);
            return;
        }

        helpContainer.setVisibility(View.VISIBLE);

//        if (((TextView) helpContainer.findViewById(R.id.help)).getText().toString().contains("ا")) {
//            FontManager.markAsIconContainer(helpContainer, FontManager.getTypeface(getActivity(), FontManager.ADOBE_ARABIC));
//            final TextView englishText = (TextView) layoutView.findViewById(R.id.english_text);
//            if (englishText != null)
//                englishText.setTypeface(FontManager.getTypeface(getActivity(), FontManager.UBUNTU));
//
//        } else {
//            FontManager.markAsIconContainer(helpContainer, FontManager.getTypeface(getActivity(), FontManager.UBUNTU));
//        }

        ImageButton imageButton = (ImageButton) layoutView.findViewById(R.id.add);
        imageButton.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_format_list_numbered).sizeDp(72).colorRes(R.color.colorContextHelp));
    }

    @Override
    public void onStart() {
        super.onStart();

        WaitingItems.importWaitingItems(getActivity());
        adapter.notifyDataSetChanged();
        updateHelpContainer(getView());
        WaitingItems.listener = this;
    }

    @Override
    public void onResume() {
        super.onResume();
        coolEnglishTimesFragment = (CoolEnglishTimesFragment) getActivity().getSupportFragmentManager().findFragmentByTag(CoolEnglishTimesFragment.FRAGMENT_TAG);
        if (coolEnglishTimesFragment != null)
            coolEnglishTimesFragment.updateBlinker((RootActivity) getActivity(), 0);
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
        recyclerView.addItemDecoration(new SpacesItemDecoration());

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
                int repeatCount = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("repeat_count", "12"));
                final int remainingCount = repeatCount - waitingItem.practiceCount;
                holder.hitCountTextView.setText(context.getResources().getQuantityString(R.plurals.pending_count, remainingCount, remainingCount));

                // load down-sampled poster
                int w = holder.posterImageView.getMaxWidth();
                int h = holder.posterImageView.getMaxHeight();
                Picasso
                        .with(holder.itemView.getContext())
                        .load(new File(item.rootDirectory, item.posterFileName))
                        .resize(w, h)
                        .centerCrop()
                        .into(holder.posterImageView);

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
                holder.handleView.setVisibility(coolEnglishTimesFragment.actionMode == null ? View.VISIBLE : View.INVISIBLE);

                if (selectedItems.get(position, false)) {
                    holder.checkMarkImageView.setVisibility(View.VISIBLE);
                } else {
                    holder.checkMarkImageView.setVisibility(View.INVISIBLE);
                }

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (coolEnglishTimesFragment.actionMode == null) {
                            Intent intent = new Intent(getActivity(), ReadAndListenActivity.class);
                            intent.putExtra(ReadAndListenActivity.ARG_ROOT_DIRECTORY, waitingItem.itemRootDirectory.getAbsolutePath());
                            getActivity().startActivityForResult(intent, ReadAndListenActivity.RC_LESSON_ACTIVITY);
                        }
                    }
                });

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
                handleView.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_reorder).sizeDp(24).paddingDp(4).colorRes(R.color.accent));
                handleView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getActivity(), R.string.drag_hint, Toast.LENGTH_SHORT).show();
                    }
                });

                checkMarkImageView = (ImageView) view.findViewById(R.id.check_mark);
                checkMarkImageView.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_check).sizeDp(24).paddingDp(4).colorRes(R.color.accent));
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

    private CoolEnglishTimesFragment coolEnglishTimesFragment;

    private class RecyclerViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (coolEnglishTimesFragment.actionMode != null) {
                View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
                final int position = recyclerView.getChildAdapterPosition(view);
                if (position != RecyclerView.NO_POSITION)
                    toggleSelection(position);
            }
            return super.onSingleTapConfirmed(e);
        }

        public void onLongPress(MotionEvent e) {
            if (itemIsDragging) {
                itemIsDragging = false;
                return;
            }
            if (coolEnglishTimesFragment.actionMode != null)
                return;

            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            int idx = recyclerView.getChildAdapterPosition(view);
            if (idx == RecyclerView.NO_POSITION)
                return;

            // Start the CAB using the ActionMode.Callback defined above
            coolEnglishTimesFragment.actionMode = getActivity().startActionMode(WaitingListFragment.this);
            toggleSelection(idx);

            // hide handler when in action mode
            adapter.notifyItemRangeChanged(0, adapter.getItemCount());

            super.onLongPress(e);
        }
    }

    private void toggleSelection(int idx) {
        adapter.toggleSelection(idx);

        final int selectedItemsCount = adapter.getSelectedItemsCount();
        if (selectedItemsCount > 0) {
            final String title = getString(R.string.selected_count, selectedItemsCount);
            coolEnglishTimesFragment.actionMode.setTitle(title);
        } else {
            coolEnglishTimesFragment.actionMode.finish();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.waiting_list_action_mode, menu);
        menu.findItem(R.id.action_delete).setIcon(new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_delete).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        List<Integer> selectedItemPositions = adapter.getSelectedItems();

        WaitingItems.WaitingItem[] selectedWaitingItems = new WaitingItems.WaitingItem[selectedItemPositions.size()];
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--)
            selectedWaitingItems[i] = WaitingItems.waitingItems.get(selectedItemPositions.get(i));

        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                for (WaitingItems.WaitingItem waitingItem : selectedWaitingItems) {
                    final int position = WaitingItems.waitingItems.indexOf(waitingItem);
                    WaitingItems.waitingItems.remove(waitingItem);
                    adapter.notifyItemRemoved(position);
                }
                WaitingItems.saveWaitingItems(getActivity());
                if (coolEnglishTimesFragment != null)
                    coolEnglishTimesFragment.updateBlinker((RootActivity) getActivity(), 0);
                break;

            default:
                return false;
        }

        actionMode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        coolEnglishTimesFragment.actionMode = null;
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
        coolEnglishTimesFragment.finishActionMode();
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
            final int position = recyclerView.getChildAdapterPosition(view);

            outRect.left = hMargin;
            outRect.right = hMargin;
            outRect.top = position == 0 ? vMargin : spacing / 2;
            outRect.bottom = position == adapter.getItemCount() - 1 ? vMargin : spacing / 2;
        }
    }
}
