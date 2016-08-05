package me.ali.coolenglishmagazine.model;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import me.ali.coolenglishmagazine.BuildConfig;
import me.ali.coolenglishmagazine.R;
import me.ali.coolenglishmagazine.broadcast_receivers.DownloadCompleteBroadcastReceiver;
import me.ali.coolenglishmagazine.util.FileHelper;
import me.ali.coolenglishmagazine.util.LogHelper;

public class Magazines {

    private static final String TAG = LogHelper.makeLogTag(Magazines.class);

    /**
     * maximum number of issues
     */
    public static final int MAX_ISSUES = 1000;

    /**
     * An array of magazine issues.
     */
    public final Set<Issue> ISSUES = new HashSet<>();

    /**
     * populates list of {@link #ISSUES} from the specified root directory of device's local storage
     */
    public void loadIssues(Context context) {
        final File filesDir = context.getExternalFilesDir(null);
        if (filesDir == null) {
            Toast.makeText(context, R.string.media_error, Toast.LENGTH_SHORT).show();
            return;
        }

        File f = new File(filesDir.getAbsolutePath());
        File[] files = f.listFiles();
        if (files != null) {
            for (File g : files) {
                if (g.isDirectory()) {
                    try {
                        addIssue(getIssue(context, g));

                    } catch (IOException e) {
                        LogHelper.e(TAG, e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * a mapping from issue root directories to {@link Issue}, to prevent duplicate objects of the same issue, and have the {@link Issue.OnStatusChangedListener} instance value stable.
     */
    static HashMap<File, Issue> file2issue = new HashMap<>();

    public static Issue getIssue(Context context, File issueRootDirectory) throws IOException, NumberFormatException {
        Issue issue = file2issue.get(issueRootDirectory);

        // verify issue integrity,
        // and delete issue on error
        boolean ok = new File(issueRootDirectory, Issue.contentFileName).exists()
                && new File(issueRootDirectory, Issue.posterFileName).exists();
        if (!ok) {
            throw new IOException("Cannot find issue introduction file.");
        }

        if (issue == null) {
            File input = new File(issueRootDirectory, Issue.manifestFileName);
            final Document doc = Jsoup.parse(input, "UTF-8", "");

//            MessageDigest digest = MessageDigest.getInstance( "SHA-1" );
//            byte[] bytes = toHash.getBytes("UTF-8");
//            digest.update(bytes, 0, bytes.length);
//            bytes = digest.digest();

            Element e = doc.getElementsByTag("issue").first();
            if (e == null) {
                throw new IOException("Invalid manifest file.");
            }

            issue = new Issue();

            issue.rootDirectory = issueRootDirectory;
            issue.title = e.attr("title");
            issue.subtitle = e.attr("subtitle");
            issue.description = e.attr("description");
            issue.id = Integer.parseInt(issueRootDirectory.getName());
            issue.price = e.attr("price");
            issue.purchased = Boolean.parseBoolean(e.attr("purchased"));
            issue.free = Boolean.parseBoolean(e.attr("free"));

            computeIssueStatus(context, issue);

            file2issue.put(issueRootDirectory, issue);
        }

        return issue;
    }

    /**
     * sets issue {@link Issue.Status} value. it does not trigger {@link Issue.OnStatusChangedListener}.
     *
     * @param context application context
     * @param issue   issue whose status we want to update
     */
    public static void computeIssueStatus(Context context, Issue issue) {
        int downloadStatus = getDownloadStatus(context, issue);

        if (new File(issue.rootDirectory, Issue.completedFileName).exists())
            issue.status = Issue.Status.completed;
        else if (new File(issue.rootDirectory, Issue.activeFileName).exists())
            issue.status = Issue.Status.active;
        else if (new File(issue.rootDirectory, Issue.downloadedFileName).exists())
            issue.status = Issue.Status.other_saved;
        else if (downloadStatus != -1 && downloadStatus != DownloadManager.STATUS_SUCCESSFUL)
            issue.status = Issue.Status.downloading;
        else
            issue.status = Issue.Status.available;
    }

    /**
     * make sure issue is not already added. otherwise bad behaviour
     * occurs when calling {@link me.ali.coolenglishmagazine.model.Magazines.OnDataSetChangedListener} functions.
     *
     * @param issue to add
     */
    private void addIssue(Issue issue) {
        if (ISSUES.add(issue)) {
            for (OnDataSetChangedListener listener : listeners)
                listener.onIssueAdded(issue);
        }
    }

    /**
     * Properties of a magazine issue
     */
    public static class Issue {
        /**
         * file containing issue properties
         */
        public static final String manifestFileName = "manifest.xml";

        /**
         * issue introduction (details) page
         */
        public static final String contentFileName = "content.html";

        /**
         * magazine cover picture
         */
        public static final String posterFileName = "cover.jpg";

        /**
         * if this file is present, issue is downloaded and available to read offline.
         */
        public static final String downloadedFileName = "downloaded";

        public static final String signatureFreeFileName = "signature-free";

        public static final String signaturePaidFileName = "signature-paid";

//        /**
//         * if this file is present, user has downloaded full-text "free" version of the issue.
//         * this file is included in free issue's zip archive.
//         * NOTE: completely free issues do not contain this file.
//         */
//        public static final String paidContentDownloadedFileName = "free-content-downloaded";

        /**
         * if this file is present, issue is the currently active one.
         */
        public static final String activeFileName = "active";

        /**
         * if this file is present, issue is completely learnt.
         */
        public static final String completedFileName = "completed";

        /**
         * an string representation of this issue, combination of year and week number.
         * e.g. "Cool English Magazine #1 (2016, Week #1)
         */
        public String subtitle;

        /**
         * featured title, which attracts reader to download it!
         */
        public String title;

        /**
         * short description of the issue. displayed in a maximum of three lines.
         */
        public String description;

        public File rootDirectory;

        /**
         * unique identifier which is also magazine root folder's name
         */
        public int id;

        /**
         * issue status is used to classify them in the issues list activity, and show them in appropriate tab and in proper section.
         */
        public enum Status {
            header_active,
            active,
            header_other_saved,
            other_saved,
            header_downloading,
            downloading, // downloading or failed downloading
            header_available,
            available,
            header_completed, // not used, because completed issues are listed in their own tab
            completed,
            header_deleted,
            deleted,
        }

        private Status status;

        // TODO: what happens to memory if there are a lot of items in the list?
//        public Bitmap poster;

        /**
         * see {@link me.ali.coolenglishmagazine.model.Magazines.Issue.Status} for more information.
         *
         * @return issue status ordinal
         */
        public int getStatusValue() {
            return status.ordinal();
        }

        /**
         * see {@link me.ali.coolenglishmagazine.model.Magazines.Issue.Status} for more information.
         *
         * @return issue status
         */
        public Status getStatus() {
            return status;
        }

        /**
         * changes issue status. see {@link me.ali.coolenglishmagazine.model.Magazines.Issue.Status} for more information.
         *
         * @param status new {@link me.ali.coolenglishmagazine.model.Magazines.Issue.Status} value
         */
        public void setStatus(Status status) {
            this.status = status;
            for (OnStatusChangedListener listener : listeners)
                listener.onIssueStatusChanged(this);
        }

        public void addOnStatusChangedListener(OnStatusChangedListener listener) {
            listeners.add(listener);
        }

        public void removeOnStatusChangedListener(OnStatusChangedListener listener) {
            listeners.remove(listener);
        }

        /**
         * implement to get informed about any change occurrence in issue's {@link Issue.Status}
         */
        public interface OnStatusChangedListener {
            void onIssueStatusChanged(Issue issue);
        }

        protected Set<OnStatusChangedListener> listeners = new HashSet<>();

        /**
         * price of the full-text version of this issue. if null or length is zero,
         * price is unknown, and has to be fetched from app store.
         */
        public String price;

        public boolean purchased;

        public boolean free;

//        public String purchaseToken;

        public boolean paidContentIsValid, freeContentIsValid;
    }

    /**
     * @return path to downloaded issue zip archive
     */
    public static File getIssueLocalDownloadUri(Context context, Issue issue) {
        return new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), Integer.toString(issue.id) + ".zip");
    }

    /**
     * downloads a single issue.
     *
     * @return download reference number, or -1 if issue is already downloaded/downloading
     */
    public static long download(Context context, Issue issue) throws IOException {
        final int status = getDownloadStatus(context, issue);
        if (status == DownloadManager.STATUS_PENDING
                || status == DownloadManager.STATUS_PAUSED
                || status == DownloadManager.STATUS_RUNNING
                || status == -3 // extracting
                || new File(issue.rootDirectory, Issue.downloadedFileName).exists())
            return -1;

        boolean wifiOnly = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("download_plan", "1")) == 0;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(getIssueDownloadUrl(context, issue)))
                .setDescription(issue.title)
                .setTitle(issue.subtitle)
                .setDestinationUri(Uri.fromFile(getIssueLocalDownloadUri(context, issue)))
                .setVisibleInDownloadsUi(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedNetworkTypes(wifiOnly ? DownloadManager.Request.NETWORK_WIFI : (DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE));

        final DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        issue.setStatus(Issue.Status.downloading);
        return downloadManager.enqueue(request);
    }

    /**
     * @return URL from which the zip archive of the given issue can be downloaded.
     */
    public static String getIssueDownloadUrl(Context context, Issue issue) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        final Uri uri = Uri.parse(preferences.getString("server_address", context.getResources().getString(R.string.pref_default_server_address)));

        // http://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
        return uri.toString() + "/api/issues/" + Integer.parseInt(issue.rootDirectory.getName())
                + "?app_version=" + BuildConfig.VERSION_CODE;
    }

    /**
     * @return download percentage, bytes downloaded, total bytes
     */
    public static int[] getDownloadProgress(Context context, Issue issue) {
        final String issueDownloadUrl = getIssueDownloadUrl(context, issue);

        DownloadManager.Query query = new DownloadManager.Query();

        Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
        final int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI);

        int progress = 0, bytes_downloaded = 0, bytes_total = 0;

        // TODO: consider when there may be more than one query result for a single URI
        while (cursor.moveToNext()) {
            final String cursorUrl = cursor.getString(uriIndex);
            if (cursorUrl.equals(issueDownloadUrl)) {
                bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                progress = bytes_total != 0 ? (int) ((double) bytes_downloaded / (double) bytes_total * 100.0) : 0;
                break;
            }
        }

        cursor.close();

        return new int[]{progress, bytes_downloaded, bytes_total};
    }

    /**
     * @return download status (if there is any) or -1
     */
    public static int getDownloadStatus(Context context, Issue issue) {
        final String issueDownloadUrl = getIssueDownloadUrl(context, issue);

        DownloadManager.Query query = new DownloadManager.Query();

        Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
        final int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI);

        int status = -1;

        // TODO: consider when there may be more than one query result for a single URI
        while (cursor.moveToNext()) {
            final String cursorUrl = cursor.getString(uriIndex);
            if (cursorUrl.equals(issueDownloadUrl)) {
                status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    if (!new File(issue.rootDirectory, Issue.downloadedFileName).exists()) {
//                        if (getIssueLocalDownloadUri(context, issue).exists())
                        status = -3; // custom value indicating that the issue is being extracted.
//                        else
//                            status = -1;
                    }
                }
                break;
            }
        }

        cursor.close();
        return status;
    }

    /**
     * returns {@link Issue} from {@link File} of zip archive or issue directory.
     *
     * @param context application context
     * @param file    zip archive or issue directory
     * @return corresponding {@link Issue}
     * @throws IOException if file is not present
     */
    public static Issue getIssueFromFile(Context context, File file) throws IOException {
        final String issueId = file.getName().split("\\.(?=[^\\.]+$)")[0];
        return Magazines.getIssue(context, new File(context.getExternalFilesDir(null), issueId));
    }

    /**
     * @return download status (if there is any) or -1
     */
    public static long getDownloadReference(Context context, Issue issue) {
        final String issueDownloadUrl = getIssueDownloadUrl(context, issue);

        DownloadManager.Query query = new DownloadManager.Query();

        Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
        final int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI);

        long result = -1;
        // TODO: what if more than one query row is found?
        while (cursor.moveToNext()) {
            final String cursorUrl = cursor.getString(uriIndex);
            if (cursorUrl.equals(issueDownloadUrl)) {
                result = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
                break;
            }
        }

        cursor.close();
        return result;
    }

    private static Set<OnDataSetChangedListener> listeners = new HashSet<>();

    public interface OnDataSetChangedListener {
        void onIssueAdded(Issue issue);
    }

    public static void addOnDataSetChangedListener(OnDataSetChangedListener listener) {
        listeners.add(listener);
    }

    public static void removeOnDataSetChangedListener(OnDataSetChangedListener listener) {
        listeners.remove(listener);
    }

    /**
     * update status yourself after calling this.
     *
     * @param issue issue to cancel download of.
     */
    public static void cancelDownload(Context context, Issue issue) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(DownloadCompleteBroadcastReceiver.ISSUE_DOWNLOADED_NOTIFICATION_ID + issue.id);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> newSavedIssues = preferences.getStringSet("new_saved_issues", new HashSet<String>(0));
        final String id = Integer.toString(issue.id);
        if (newSavedIssues.contains(id)) {
            newSavedIssues = new HashSet<>(newSavedIssues);
            newSavedIssues.remove(id);
            preferences.edit().putStringSet("new_saved_issues", newSavedIssues).apply();
        }

        // delete download if it is downloading
        final long downloadReference = getDownloadReference(context, issue);
        if (downloadReference != -1) {
            final DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            downloadManager.remove(downloadReference);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    downloadManager.remove(downloadReference); // http://stackoverflow.com/a/34797980
                }
            }, 1000);
        }
    }

    /**
     * deletes issue. it remains in available issues, and need to be downloaded again by user.
     *
     * @param context   activity context
     * @param issue     issue to delete content and make available for download
     * @param permanent if true, deletes root folder completely, so it won't be visible in available issues list.
     */
    public static void deleteIssue(Context context, Issue issue, boolean permanent) {
        cancelDownload(context, issue);

        if (!permanent) {
            File[] files = issue.rootDirectory.listFiles();
            if (files != null) {
                for (File g : files) {
                    if (g.isDirectory()) { // delete item folders only
                        FileHelper.deleteRecursive(g);
                    }
                }
            }
            FileHelper.delete(new File(issue.rootDirectory, Magazines.Issue.downloadedFileName));
            FileHelper.delete(new File(issue.rootDirectory, Issue.signaturePaidFileName));
            FileHelper.delete(new File(issue.rootDirectory, Issue.signatureFreeFileName));

            computeIssueStatus(context, issue);
            issue.setStatus(issue.status);

        } else {
            FileHelper.deleteRecursive(issue.rootDirectory);
            issue.setStatus(Issue.Status.deleted);
            file2issue.remove(issue.rootDirectory);
        }
    }

    /**
     * marks issue as complete.
     *
     * @param issue issue to be marked as complete.
     */
    public static void markCompleted(Issue issue) {
        File f = new File(issue.rootDirectory, Issue.completedFileName);
        try {
            f.createNewFile();
            issue.setStatus(Issue.Status.completed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * un-completes an issue.
     *
     * @param context application or activity context
     * @param issue   issue to be marked as incomplete
     */
    public static void reopen(Context context, Issue issue) {
        FileHelper.delete(new File(issue.rootDirectory, Issue.completedFileName));
        computeIssueStatus(context, issue);
        issue.setStatus(issue.status);
    }

    /**
     * returns SKU of products (issues in this case).
     *
     * @param issue issue for which SKU is needed.
     * @return SKU of the specified issue
     */
    public static String getSku(Issue issue) {
        return "HEM_" + issue.id;
    }
}
