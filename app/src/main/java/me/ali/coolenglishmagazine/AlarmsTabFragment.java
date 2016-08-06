package me.ali.coolenglishmagazine;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.widget.TimePicker;
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
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import me.ali.coolenglishmagazine.broadcast_receivers.AlarmBroadcastReceiver;
import me.ali.coolenglishmagazine.broadcast_receivers.BootReceiver;
import me.ali.coolenglishmagazine.util.FontManager;
import me.ali.coolenglishmagazine.util.LogHelper;
import me.ali.coolenglishmagazine.widget.MyAnalogClock;


public class AlarmsTabFragment extends Fragment implements RecyclerView.OnItemTouchListener, ActionMode.Callback {

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

        setHasOptionsMenu(true);
        alarms = importAlarms(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        coolEnglishTimesFragment = (CoolEnglishTimesFragment) getActivity().getSupportFragmentManager().findFragmentByTag(CoolEnglishTimesFragment.FRAGMENT_TAG);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.alarms_fragment_menu, menu);
        if (isAdded())
            menu.findItem(R.id.action_add).setIcon(new IconicsDrawable(getContext(), GoogleMaterial.Icon.gmd_alarm_add).sizeDp(24).paddingDp(3).colorRes(R.color.md_dark_primary_text));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add:
                createAlarm();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void createAlarm() {
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog;
        timePickerDialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                if (!timePicker.isShown())
                    return;

                Alarm alarm = new Alarm();
                alarm.hour = selectedHour;
                alarm.minute = selectedMinute;

                for (Alarm a : alarms) {
                    if (alarm.getId() == a.getId()) {
                        Toast.makeText(getContext(), R.string.alarm_already_exists, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                alarms.add(alarm);

                // sorting with respect to time
                Collections.sort(alarms, new Comparator<Alarm>() {
                    @Override
                    public int compare(Alarm alarm1, Alarm alarm2) {
                        return alarm1.getId() - alarm2.getId();
                    }
                });

                adapter.notifyItemInserted(alarms.indexOf(alarm));

                // turn this alarm on.
                turnOnAlarm(getActivity(), alarm);

                saveAlarms();
            }
        }, hour, minute, false);

        timePickerDialog.setTitle(R.string.select_time);
        timePickerDialog.show();
    }

    RecyclerView recyclerView;
    int nColumns;

    RecyclerView.AdapterDataObserver adapterDataObserver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_alarms_list, container, false);

        recyclerView = (RecyclerView) v.findViewById(R.id.alarm_list);
        nColumns = getResources().getInteger(R.integer.alarm_column_count);
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

        // alarms was imported in onCreate()
        adapter.notifyDataSetChanged();
        updateHelpContainer(v);

        FloatingActionButton fab = (FloatingActionButton) v.findViewById(R.id.add_alarm);
        fab.setImageDrawable(new IconicsDrawable(getContext(), GoogleMaterial.Icon.gmd_alarm_add).sizeDp(24).colorRes(R.color.md_dark_primary_text));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAlarm();
            }
        });

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter.unregisterAdapterDataObserver(adapterDataObserver);
    }

    protected void updateHelpContainer(View layoutView) {
        final View helpContainer = layoutView.findViewById(R.id.help_container);

        if (adapter.getItemCount() > 0) {
            helpContainer.setVisibility(View.GONE);
            return;
        }

        helpContainer.setVisibility(View.VISIBLE);

//        FontManager.markAsIconContainer(helpContainer, FontManager.getTypeface(getActivity(),
//                ((TextView) helpContainer.findViewById(R.id.help)).getText().toString().contains("ا") ? FontManager.ADOBE_ARABIC : FontManager.UBUNTU
//        ));

        ImageButton add = (ImageButton) layoutView.findViewById(R.id.add);
        add.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_alarm_add).sizeDp(72).colorRes(R.color.colorContextHelp));
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAlarm();
            }
        });
    }

    /**
     * creates a new alarm to be triggered at the specified time every day.
     *
     * @param alarm object containing alarm time
     */
    public static void turnOnAlarm(Context context, Alarm alarm) {
        Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, alarm.getId(), intent, 0);

        // Set the alarm to start at the specified time of day.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
        calendar.set(Calendar.MINUTE, alarm.minute);

        // increase by one day if the time is in the past on today.
        if (calendar.getTimeInMillis() < System.currentTimeMillis())
            calendar.add(Calendar.DATE, 1);

        // With setInexactRepeating(), you have to use one of the AlarmManager interval
        // constants--in this case, AlarmManager.INTERVAL_DAY.
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);
    }

    /**
     * cancels an alarm that was previously setup.
     *
     * @param alarm object containing alarm time
     */
    public void turnOffAlarm(Alarm alarm) {
        Context context = getActivity();

        Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, alarm.getId(), intent, 0);

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(alarmIntent);
        alarmIntent.cancel();
    }

    /**
     * starts all alarms
     */
    public static void startAllAlarms(Context context) {
        ArrayList<AlarmsTabFragment.Alarm> alarms = AlarmsTabFragment.importAlarms(context);
        for (AlarmsTabFragment.Alarm alarm : alarms) {
            AlarmsTabFragment.turnOnAlarm(context, alarm);
        }
    }

    protected AlarmsRecyclerViewAdapter adapter = new AlarmsRecyclerViewAdapter();

    GestureDetectorCompat gestureDetector;

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), nColumns));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new SpacesItemDecoration());

        recyclerView.addOnItemTouchListener(this);
        gestureDetector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());

        int hMargin = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        int spacing = getResources().getDimensionPixelSize(R.dimen.spacing_normal);
        recyclerView.setPadding(hMargin - spacing / 2, 0, hMargin - spacing / 2, 0);
    }

    public static class Alarm implements Serializable {
        int hour;
        int minute;

        /**
         * @return an id that is unique for the time specified by this object.
         */
        int getId() {
            return hour * 60 + minute;
        }
    }

    ArrayList<Alarm> alarms;

    /**
     * list of alarms is saved in this file, within the internal files directory.
     */
    public static final String ALARMS_FILE_NAME = "alarms";

    public void saveAlarms() {
        enableOrDisableBootReceiver(getActivity());

        try {
            FileOutputStream fileOut = new FileOutputStream(new File(getContext().getFilesDir(), ALARMS_FILE_NAME));
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(alarms);
            out.close();
            fileOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Alarm> importAlarms(Context context) {
        ArrayList<Alarm> alarms = new ArrayList<>();

        try {
            FileInputStream fileIn = new FileInputStream(new File(context.getFilesDir(), ALARMS_FILE_NAME));
            ObjectInputStream in = new ObjectInputStream(fileIn);
            alarms = (ArrayList<Alarm>) in.readObject();
            in.close();
            fileIn.close();

        } catch (Exception e) {
        }

        return alarms;
    }

    /**
     * enables BootReceiver if there is any alarm. Otherwise disables it.
     *
     * @param context a valid context
     */
    public void enableOrDisableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                alarms.size() > 0 ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public class AlarmsRecyclerViewAdapter extends RecyclerView.Adapter<AlarmsRecyclerViewAdapter.ViewHolder> {
        private SparseBooleanArray selectedItems = new SparseBooleanArray();

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.alarm_list_row, parent, false);
            return new ViewHolder(view);
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm", Locale.ENGLISH);
        SimpleDateFormat amPmFormat = new SimpleDateFormat(" a", Locale.ENGLISH);

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final Alarm alarm = alarms.get(position);

            Time time = new Time(alarm.hour, alarm.minute, 0);

            holder.timeTextView.setText(timeFormat.format(time));
            holder.amPmTextView.setText(amPmFormat.format(time));
            holder.analogClock.setTime(alarm.hour, alarm.minute, 0);

            if (selectedItems.get(position, false)) {
                holder.checkMarkImageView.setVisibility(View.VISIBLE);
                holder.amPmTextView.setVisibility(View.INVISIBLE);
                holder.timeTextView.setVisibility(View.INVISIBLE);
            } else {
                holder.checkMarkImageView.setVisibility(View.INVISIBLE);
                holder.amPmTextView.setVisibility(View.VISIBLE);
                holder.timeTextView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return alarms.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView timeTextView, amPmTextView;
            MyAnalogClock analogClock;
            ImageView checkMarkImageView;

            public ViewHolder(View view) {
                super(view);

                timeTextView = (TextView) view.findViewById(R.id.time);
                amPmTextView = (TextView) view.findViewById(R.id.am_pm);
                analogClock = (MyAnalogClock) view.findViewById(R.id.clock);

                checkMarkImageView = (ImageView) view.findViewById(R.id.check_mark);
                checkMarkImageView.setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_check).sizeDp(24).paddingDp(4).colorRes(R.color.accent));

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (coolEnglishTimesFragment.actionMode == null)
                            Toast.makeText(getActivity(), R.string.action_mode_hint, Toast.LENGTH_SHORT).show();
                    }
                });
            }
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
            if (coolEnglishTimesFragment.actionMode != null)
                return;

            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            int idx = recyclerView.getChildAdapterPosition(view);
            if (idx == RecyclerView.NO_POSITION)
                return;

            // Start the CAB using the ActionMode.Callback defined above
            coolEnglishTimesFragment.actionMode = getActivity().startActionMode(AlarmsTabFragment.this);
            toggleSelection(idx);

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
        inflater.inflate(R.menu.alarms_list_action_mode, menu);
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

        Alarm[] selectedAlarms = new Alarm[selectedItemPositions.size()];
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--)
            selectedAlarms[i] = alarms.get(selectedItemPositions.get(i));

        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                for (Alarm alarm : selectedAlarms) {
                    int position = alarms.indexOf(alarm);
                    alarms.remove(alarm);
                    adapter.notifyItemRemoved(position);

                    // cancel this alarm
                    turnOffAlarm(alarm);
                }
                saveAlarms();
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

    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int vMargin, spacing;

        public SpacesItemDecoration() {
            vMargin = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
            spacing = getResources().getDimensionPixelSize(R.dimen.spacing_normal);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            final int position = recyclerView.getChildAdapterPosition(view);

            outRect.left = spacing / 2;
            outRect.right = spacing / 2;
            outRect.top = position < nColumns ? vMargin : spacing / 2;
            outRect.bottom = position == adapter.getItemCount() - 1 ? vMargin : spacing / 2;
        }
    }
}
