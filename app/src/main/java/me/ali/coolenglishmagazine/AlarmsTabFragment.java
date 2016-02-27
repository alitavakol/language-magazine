package me.ali.coolenglishmagazine;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import me.ali.coolenglishmagazine.util.LogHelper;


public class AlarmsTabFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(AlarmsTabFragment.class);

    public AlarmsTabFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AlarmsTabFragment.
     */
    public static AlarmsTabFragment newInstance() {
        AlarmsTabFragment fragment = new AlarmsTabFragment();
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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_alarms_list, container, false);

        final View recyclerView = v.findViewById(R.id.alarm_list);
        setupRecyclerView((RecyclerView) recyclerView);

        alarms.add(new Alarm());
        adapter.notifyDataSetChanged();

        return v;
    }

    protected AlarmsRecyclerViewAdapter adapter = new AlarmsRecyclerViewAdapter();

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
    }

    public class Alarm {
        public Alarm() {
            time = new Date();
            new Date(22);
        }

        Date time;
        boolean enabled;
    }

    ArrayList<Alarm> alarms = new ArrayList<>();

    public class AlarmsRecyclerViewAdapter extends RecyclerView.Adapter<AlarmsRecyclerViewAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.alarm_list_row, parent, false);
            return new ViewHolder(view);
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm");
        SimpleDateFormat amPmFormat = new SimpleDateFormat("a");

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
//            ((ImageView) holder.itemView.findViewById(R.id.delete)).setImageDrawable(new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_delete).sizeDp(72).color(Color.LTGRAY));
            Alarm alarm = alarms.get(position);
            holder.timeTextView.setText(timeFormat.format(alarm.time));
            holder.amPmTextView.setText(amPmFormat.format(alarm.time));
        }

        @Override
        public int getItemCount() {
            return alarms.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView timeTextView, amPmTextView;

            public ViewHolder(View view) {
                super(view);

                timeTextView = (TextView) view.findViewById(R.id.time);
                amPmTextView = (TextView) view.findViewById(R.id.am_pm);
            }
        }
    }

}
