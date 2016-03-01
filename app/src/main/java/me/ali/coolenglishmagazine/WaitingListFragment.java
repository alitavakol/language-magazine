package me.ali.coolenglishmagazine;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.ali.coolenglishmagazine.model.MagazineContent;


public class WaitingListFragment extends Fragment implements RecyclerView.OnItemTouchListener, ActionMode.Callback {

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

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

        waitingItems = importWaitingItems(getActivity());

        adapter.notifyDataSetChanged();
    }

    protected WaitingListRecyclerViewAdapter adapter = new WaitingListRecyclerViewAdapter();

    private ItemTouchHelper itemTouchHelper;
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

    /**
     * represent one lesson item that is in the waiting list of the Cool English Times.
     */
    public static class WaitingItem implements Serializable {
        String itemRootDirectory;
    }

    ArrayList<WaitingItem> waitingItems;

    /**
     * list of waiting items is saved in this file, within the internal files directory.
     */
    public static final String WAITING_LIST_FILE_NAME = "waiting_list";

    public static void saveWaitingItems(Context context, ArrayList<WaitingItem> waitingItems) {
        try {
            FileOutputStream fileOut = new FileOutputStream(new File(context.getFilesDir(), WAITING_LIST_FILE_NAME));
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(waitingItems);
            out.close();
            fileOut.close();

        } catch (IOException e) {
        }
    }

    /**
     * loads list of waiting lesson items from {@code WAITING_LIST_FILE_NAME}
     *
     * @param context context
     * @return an array list of {@link me.ali.coolenglishmagazine.WaitingListFragment.WaitingItem}
     */
    public static ArrayList<WaitingItem> importWaitingItems(Context context) {
        ArrayList<WaitingItem> waitingItems = new ArrayList<>();

        try {
            FileInputStream fileIn = new FileInputStream(new File(context.getFilesDir(), WAITING_LIST_FILE_NAME));
            ObjectInputStream in = new ObjectInputStream(fileIn);
            waitingItems = (ArrayList<WaitingItem>) in.readObject();
            in.close();
            fileIn.close();

        } catch (ClassNotFoundException e) {
        } catch (IOException e) {
        }

        return waitingItems;
    }

    public static void appendToWaitingList(Context context, MagazineContent.Item item) {
        ArrayList<WaitingItem> waitingItems = importWaitingItems(context);

        WaitingItem waitingItem = new WaitingItem();
        waitingItem.itemRootDirectory = item.rootDirectory.getAbsolutePath();

        waitingItems.add(waitingItem);
        saveWaitingItems(context, waitingItems);
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
            final WaitingItem waitingItem = waitingItems.get(position);

            holder.titleTextView.setText(waitingItem.itemRootDirectory);

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
        }

        @Override
        public int getItemCount() {
            return waitingItems.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;
            ImageView handleView;

            public ViewHolder(View view) {
                super(view);

                titleTextView = (TextView) view.findViewById(R.id.title);

                handleView = (ImageView) view.findViewById(R.id.handle);
                handleView.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_reorder).sizeDp(20).color(Color.LTGRAY));
                handleView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getActivity(), R.string.drag_hint, Toast.LENGTH_SHORT).show();
                    }
                });
//                view.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Toast.makeText(getActivity(), R.string.action_mode_hint, Toast.LENGTH_SHORT).show();
//                    }
//                });
            }
        }

        public void remove(int position) {
            waitingItems.remove(position);
            notifyItemRemoved(position);
            saveWaitingItems(getActivity(), waitingItems);
        }

        public void swap(int firstPosition, int secondPosition) {
            Collections.swap(waitingItems, firstPosition, secondPosition);
            adapter.notifyItemMoved(firstPosition, secondPosition);
            saveWaitingItems(getActivity(), waitingItems);
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

        public int getSelectedItemCount() {
            return selectedItems.size();
        }

        public List<Integer> getSelectedItems() {
            List<Integer> items = new ArrayList<Integer>(selectedItems.size());
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

    ActionMode actionMode;

    private class RecyclerViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
//            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
//            onClick(view);
            return super.onSingleTapConfirmed(e);
        }

        public void onLongPress(MotionEvent e) {
            if(itemIsDragging) {
                itemIsDragging = false;
                return;
            }
            if (actionMode != null)
                return;

            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());

            // Start the CAB using the ActionMode.Callback defined above
            actionMode = getActivity().startActionMode(WaitingListFragment.this);
            int idx = recyclerView.getChildPosition(view);
//            myToggleSelection(idx);
            super.onLongPress(e);
        }
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
//            case R.id.menu_delete:
//                List<Integer> selectedItemPositions = adapter.getSelectedItems();
//                int currPos;
//                for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
//                    currPos = selectedItemPositions.get(i);
//                    RecyclerViewDemoApp.removeItemFromList(currPos);
//                    adapter.removeData(currPos);
//                }
//                actionMode.finish();
//                return true;
            default:
                return false;
        }
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
}
