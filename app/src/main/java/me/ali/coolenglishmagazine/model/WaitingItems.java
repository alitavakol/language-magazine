package me.ali.coolenglishmagazine.model;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
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
import java.util.Iterator;

import me.ali.coolenglishmagazine.AlarmsTabFragment;
import me.ali.coolenglishmagazine.R;
import me.ali.coolenglishmagazine.RootActivity;
import me.ali.coolenglishmagazine.util.LogHelper;

public class WaitingItems {

    private static final String TAG = LogHelper.makeLogTag(WaitingItems.class);

    /**
     * represent one lesson item that is in the waiting list of the Cool English Times.
     */
    public static class WaitingItem implements Serializable {
        public File itemRootDirectory;

        /**
         * count of times the user has learned this item so far.
         */
        public int practiceCount;
    }

    public static ArrayList<WaitingItem> waitingItems;

    /**
     * list of waiting items is saved in this file, within the internal files directory.
     */
    public static final String WAITING_LIST_FILE_NAME = "waiting_list";

    /**
     * save list of waiting items to {@link #WAITING_LIST_FILE_NAME}, overwrites the file.
     *
     * @param context app context
     */
    public static void saveWaitingItems(Context context) {
        try {
            FileOutputStream fileOut = new FileOutputStream(new File(context.getFilesDir(), WAITING_LIST_FILE_NAME));
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(waitingItems);
            out.close();
            fileOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * loads list of waiting lesson items from {@link #WAITING_LIST_FILE_NAME}, and
     * removes waiting items whose lesson directories are not found.
     *
     * @param context context
     */
    public static void importWaitingItems(Context context) {
        if (waitingItems == null) {
            waitingItems = new ArrayList<>();

            try {
                FileInputStream fileIn = new FileInputStream(new File(context.getFilesDir(), WAITING_LIST_FILE_NAME));
                ObjectInputStream in = new ObjectInputStream(fileIn);
                waitingItems = (ArrayList<WaitingItem>) in.readObject();
                in.close();
                fileIn.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int repeatCount = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("repeat_count", "12"));

        // remove waiting items whose root directory is lost, or hit count exceeds maximum.
        Iterator<WaitingItem> i = waitingItems.iterator();
        while (i.hasNext()) {
            WaitingItem w = i.next();
            if (!w.itemRootDirectory.exists() || w.practiceCount >= repeatCount)
                i.remove();
        }
    }

    /**
     * adds item to the end of the list of waiting lesson items.
     *
     * @param context app context
     * @param item    lesson item to be added
     * @return false if item is already in the list, and true otherwise.
     */
    public static boolean appendToWaitingList(final Context context, MagazineContent.Item item) {
        importWaitingItems(context);

        WaitingItem waitingItem = new WaitingItem();
        waitingItem.itemRootDirectory = item.rootDirectory;

        for (WaitingItem w : waitingItems) {
            if (w.itemRootDirectory.equals(waitingItem.itemRootDirectory)) {
                Toast.makeText(context, context.getString(R.string.already_in_waiting_list, item.title), Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        waitingItems.add(waitingItem);
        saveWaitingItems(context);

        AlarmsTabFragment.importAlarms(context);
        if (AlarmsTabFragment.alarms.size() == 0) {
            try {
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                final String preferenceKeyHotEnglishTimesTooltipShown = "hot_english_times_tooltip_shown";

                if (!preferences.getBoolean(preferenceKeyHotEnglishTimesTooltipShown, false)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.hot_english_times_tooltip)
                            .setTitle(R.string.hot_english_times_tooltip_title)
                            .setIcon(new IconicsDrawable(context).icon(GoogleMaterial.Icon.gmd_alarm_add).sizeDp(72).colorRes(R.color.primary_dark))
                            .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    context.startActivity(new Intent(context, RootActivity.class).setAction(RootActivity.ACTION_SHOW_TIMES).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                }
                            })
                            .setNegativeButton(R.string.later, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setCancelable(false)
                            .show();

                    preferences.edit().putBoolean(preferenceKeyHotEnglishTimesTooltipShown, true).apply();
                }

            } catch (Exception e) {
            }
        }


        return true;
    }

    /**
     * icrements count of times the lesson item has been learnt so far.
     *
     * @param context app context
     * @param item    learnt lesson to increment its hit count
     */
    public static void incrementHitCount(Context context, MagazineContent.Item item) {
        int repeatCount = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("repeat_count", "12"));

        importWaitingItems(context);

        Iterator<WaitingItem> i = waitingItems.iterator();
        while (i.hasNext()) {
            WaitingItem w = i.next();
            if (w.itemRootDirectory.equals(item.rootDirectory)) {
                w.practiceCount++;

                if (w.practiceCount >= repeatCount) {
                    i.remove();
                    if (listener != null)
                        listener.onWaitingItemRemoved(w);

                } else {
                    if (listener != null)
                        listener.onWaitingItemHitCountChanged(w);
                }

                saveWaitingItems(context);
                break;
            }
        }
    }

    public interface OnWaitingItemChangedListener {
        /**
         * if a {@link me.ali.coolenglishmagazine.WaitingListFragment} is active
         * while media is playing in the background, item hit count increments after
         * playback completion. this callback notifies the listener about this change.
         *
         * @param waitingItem a {@link WaitingItem} whose hit count has changed.
         */
        void onWaitingItemHitCountChanged(WaitingItem waitingItem);

        /**
         * if a {@link me.ali.coolenglishmagazine.WaitingListFragment} is active
         * while media is playing in the background, item hit count increments after
         * playback completion. this callback notifies the listener about this change.
         *
         * @param waitingItem a {@link WaitingItem} whose hit count has exceeded maximum, and removed.
         */
        void onWaitingItemRemoved(WaitingItem waitingItem);
    }

    public static OnWaitingItemChangedListener listener;
}

