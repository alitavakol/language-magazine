package me.ali.coolenglishmagazine;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Locale;

import me.ali.coolenglishmagazine.broadcast_receivers.AlarmBroadcastReceiver;
import me.ali.coolenglishmagazine.broadcast_receivers.BootReceiver;
import me.ali.coolenglishmagazine.util.LogHelper;
import me.ali.coolenglishmagazine.widget.MyAnalogClock;


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

        alarms = importAlarms(getContext());
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

        final FloatingActionButton fab = (FloatingActionButton) v.findViewById(R.id.fab);
        fab.setImageDrawable(new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_add).color(Color.LTGRAY));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                        alarm.enabled = true;

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
        });

        final View recyclerView = v.findViewById(R.id.alarm_list);
        setupRecyclerView((RecyclerView) recyclerView);

        // alarms was imported in onCreate()
        adapter.notifyDataSetChanged();

        return v;
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
//        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
        calendar.set(Calendar.MINUTE, alarm.minute);

        // increase by one day if the time is in the past on today.
        if (calendar.getTimeInMillis() < System.currentTimeMillis())
            calendar.add(Calendar.DATE, 1);

        // With setInexactRepeating(), you have to use one of the AlarmManager interval
        // constants--in this case, AlarmManager.INTERVAL_DAY.
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
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

    protected AlarmsRecyclerViewAdapter adapter = new AlarmsRecyclerViewAdapter();

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setHasFixedSize(true);
    }

    public static class Alarm implements Serializable {
        int hour;
        int minute;
        public boolean enabled;

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

        } catch (ClassNotFoundException e) {
        } catch (IOException e) {
        }

        return alarms;
    }

    /**
     * enables BootReceiver if there is any alarm. Otherwise disables it.
     *
     * @param context a valid context
     */
    public void enableOrDisableBootReceiver(Context context) {
        int enabledAlarms = 0;
        for (Alarm alarm : alarms) {
            if (alarm.enabled)
                enabledAlarms++;
        }

        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                enabledAlarms > 0 ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public class AlarmsRecyclerViewAdapter extends RecyclerView.Adapter<AlarmsRecyclerViewAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.alarm_list_row, parent, false);
            return new ViewHolder(view);
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm", Locale.getDefault());
        SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.getDefault());

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final Alarm alarm = alarms.get(position);

            Time time = new Time(alarm.hour, alarm.minute, 0);

            holder.timeTextView.setText(timeFormat.format(time));
            holder.amPmTextView.setText(amPmFormat.format(time));
            holder.analogClock.setTime(alarm.hour, alarm.minute, 0);

//            holder.onOffswitch.setChecked(alarm.enabled);
//            holder.onOffswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    if (isChecked)
//                        turnOnAlarm(getActivity(), alarm);
//                    else
//                        turnOffAlarm(alarm);
//
//                    saveAlarms();
//                }
//            });

//            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    alarms.remove(alarm);
//                    adapter.notifyItemRemoved(holder.getAdapterPosition());
//
//                    // cancel this alarm
//                    turnOffAlarm(alarm);
//
//                    saveAlarms();
//                }
//            });
        }

        @Override
        public int getItemCount() {
            return alarms.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView timeTextView, amPmTextView;
            MyAnalogClock analogClock;

            public ViewHolder(View view) {
                super(view);

                timeTextView = (TextView) view.findViewById(R.id.time);
                amPmTextView = (TextView) view.findViewById(R.id.am_pm);
                analogClock = (MyAnalogClock) view.findViewById(R.id.clock);
            }
        }
    }

}
