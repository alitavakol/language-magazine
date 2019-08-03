package me.ali.coolenglishmagazine.broadcast_receivers;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import me.ali.coolenglishmagazine.IssueDetailActivity;
import me.ali.coolenglishmagazine.ItemListActivity;
import me.ali.coolenglishmagazine.R;
import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.FileHelper;
import me.ali.coolenglishmagazine.util.LogHelper;
import me.ali.coolenglishmagazine.util.NetworkHelper;
import me.ali.coolenglishmagazine.util.ZipHelper;

public class DownloadCompleteBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = LogHelper.makeLogTag(DownloadCompleteBroadcastReceiver.class);

    /**
     * it takes up {@link Magazines#MAX_ISSUES} interval.
     */
    public static final int ISSUE_DOWNLOADED_NOTIFICATION_ID = 200;

    /**
     * local broadcast intent action that is sent when an issue has been downloaded and extracted.
     */
    public static final String ACTION_DOWNLOAD_EXTRACTED = "me.ali.coolenglishmagazine.ACTION_DOWNLOAD_EXTRACTED";

    public DownloadCompleteBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent_) {
        long reference = intent_.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

        // do something with the download file
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(reference);

        Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
        if (cursor.moveToFirst()) {
            // get the status of the download
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));

            switch (status) {
                case DownloadManager.STATUS_SUCCESSFUL:
                    int fileNameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                    final File f = new File(cursor.getString(fileNameIndex));
                    new UnzipOperation().execute(context, f);
                    break;

                case DownloadManager.STATUS_FAILED:
                    // get the reason - more detail on the status
                    int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

                    if (reason == 426) { // 426 Upgrade Required
                        NetworkHelper.showUpgradeDialog(context, true);

                    } else {
                        Toast.makeText(context, context.getResources().getString(R.string.download_failed_msg, reason), Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
        cursor.close();
    }

    /**
     * a background task that extracts downloaded zip archive, and emits a download extracted broadcast intent.
     */
    private class UnzipOperation extends AsyncTask<Object, Void, File> {
        Context context;
        File f;

        @Override
        protected File doInBackground(Object... params) {
            f = (File) params[1];
            context = (Context) params[0];
            try {
                return ZipHelper.unzip(new FileInputStream(f), context.getExternalFilesDir(null), false);
            } catch (FileNotFoundException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(File rootFile) {
            Magazines.Issue issue = null;

            try {
                if (rootFile == null) {
                    issue = Magazines.getIssueFromFile(context, f);
                    Toast.makeText(context, R.string.extract_error, Toast.LENGTH_LONG).show();
                    throw new Exception("Could not unzip");

                } else {
                    issue = Magazines.getIssueFromFile(context, rootFile);

                    final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    final long reference = Magazines.getDownloadReference(context, issue);
                    dm.remove(reference);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dm.remove(reference); // http://stackoverflow.com/a/34797980
                        }
                    }, 1000);

                    // prepare intent which is triggered if the notification is selected
                    Intent intent = new Intent(context, ItemListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());

                    // use System.currentTimeMillis() to have a unique ID for the pending intent
                    PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

                    // build notification
                    // the addAction re-use the same intent to keep the example short
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                            .setContentTitle(issue.subtitle)
                            .setContentText(context.getResources().getString(R.string.issue_downloaded_notification))
                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_large_icon))
                            .setSmallIcon(R.drawable.sunglasses)
                            .setColor(context.getResources().getColor(R.color.primary_dark))
                            .setContentIntent(pIntent)
                            .setAutoCancel(true);

                    ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(ISSUE_DOWNLOADED_NOTIFICATION_ID + issue.id, builder.build());

                    Magazines.computeIssueStatus(context, issue);
                    issue.setStatus(issue.getStatus());

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    Set<String> newSavedIssues = new HashSet<>(preferences.getStringSet("new_saved_issues", new HashSet<String>(0)));
                    newSavedIssues.add(Integer.toString(issue.id));
                    preferences.edit().putStringSet("new_saved_issues", newSavedIssues).apply();
                }

            } catch (Exception e) {
                if (issue != null)
                    Magazines.deleteIssue(context, issue, false);
                LogHelper.e(TAG, e.getMessage());

            } finally {
                FileHelper.delete(f);
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(DownloadCompleteBroadcastReceiver.ACTION_DOWNLOAD_EXTRACTED));
            }
        }
    }

}
