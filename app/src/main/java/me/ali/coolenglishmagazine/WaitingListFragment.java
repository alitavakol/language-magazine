package me.ali.coolenglishmagazine;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
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

import me.ali.coolenglishmagazine.model.MagazineContent;


public class WaitingListFragment extends Fragment {

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_waiting_list, container, false);

        final View recyclerView = v.findViewById(R.id.waiting_list);
        setupRecyclerView((RecyclerView) recyclerView);

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

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        ItemTouchHelper.Callback callback = new WaitingItemTouchHelper();
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
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

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.waiting_list_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final WaitingItem waitingItem = waitingItems.get(position);

            holder.titleTextView.setText(waitingItem.itemRootDirectory);
            holder.deleteImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.remove(holder.getAdapterPosition());
                }
            });
            holder.handleView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper.startDrag(holder);
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
            ImageView deleteImageView;
            ImageView handleView;

            public ViewHolder(View view) {
                super(view);

                titleTextView = (TextView) view.findViewById(R.id.title);
                deleteImageView = (ImageView) view.findViewById(R.id.delete);

                handleView = (ImageView) view.findViewById(R.id.handle);
                handleView.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_reorder).sizeDp(20).color(Color.LTGRAY));
                handleView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getActivity(), R.string.drag_hint, Toast.LENGTH_SHORT).show();
                    }
                });
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
    }

    public class WaitingItemTouchHelper extends ItemTouchHelper.SimpleCallback {
        public WaitingItemTouchHelper() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
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

}
