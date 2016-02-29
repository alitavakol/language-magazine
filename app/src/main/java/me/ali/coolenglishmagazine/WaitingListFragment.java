package me.ali.coolenglishmagazine;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;


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

        waitingItems = importWaitingItems();

        for (int i = 0; i < 25; i++) {
            WaitingItem waitingItem = new WaitingItem();
            waitingItem.itemRootDirectory = "Item Root Directory " + i;
            waitingItems.add(waitingItem);
        }

        adapter.notifyDataSetChanged();
    }

    protected WaitingListRecyclerViewAdapter adapter = new WaitingListRecyclerViewAdapter();

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        ItemTouchHelper.Callback callback = new WaitingItemTouchHelper();
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(recyclerView);
    }

    public static class WaitingItem implements Serializable {
        String itemRootDirectory;
    }

    ArrayList<WaitingItem> waitingItems;

    /**
     * list of waiting items is saved in this file, within the internal files directory.
     */
    public static final String WAITING_LIST_FILE_NAME = "waiting_list";

    public void saveWaitingItems() {
        try {
            FileOutputStream fileOut = new FileOutputStream(new File(getContext().getFilesDir(), WAITING_LIST_FILE_NAME));
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(waitingItems);
            out.close();
            fileOut.close();

        } catch (IOException e) {
        }
    }

    public ArrayList<WaitingItem> importWaitingItems() {
        ArrayList<WaitingItem> waitingItems = new ArrayList<>();

        try {
            FileInputStream fileIn = new FileInputStream(new File(getActivity().getFilesDir(), WAITING_LIST_FILE_NAME));
            ObjectInputStream in = new ObjectInputStream(fileIn);
            waitingItems = (ArrayList<WaitingItem>) in.readObject();
            in.close();
            fileIn.close();

        } catch (ClassNotFoundException e) {
        } catch (IOException e) {
        }

        return waitingItems;
    }

    public class WaitingListRecyclerViewAdapter extends RecyclerView.Adapter<WaitingListRecyclerViewAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.waiting_list_row, parent, false);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), R.string.drag_hint, Toast.LENGTH_SHORT).show();
                }
            });
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final WaitingItem waitingItem = waitingItems.get(position);

            holder.titleTextView.setText(waitingItem.itemRootDirectory);
            holder.deleteImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.remove(position);
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

            public ViewHolder(View view) {
                super(view);

                titleTextView = (TextView) view.findViewById(R.id.title);
                deleteImageView = (ImageView) view.findViewById(R.id.delete);
            }
        }

        public void remove(int position) {
            waitingItems.remove(position);
            notifyItemRemoved(position);
        }

        public void swap(int firstPosition, int secondPosition) {
            Collections.swap(waitingItems, firstPosition, secondPosition);
            adapter.notifyItemMoved(firstPosition, secondPosition);
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
