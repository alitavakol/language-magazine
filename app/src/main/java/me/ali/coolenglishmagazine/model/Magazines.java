package me.ali.coolenglishmagazine.model;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import me.ali.coolenglishmagazine.R;
import me.ali.coolenglishmagazine.util.LogHelper;

public class Magazines {

    private static final String TAG = LogHelper.makeLogTag(Magazines.class);

    /**
     * An array of magazine issues.
     */
    public final List<Issue> ISSUES = new ArrayList<>();

    /**
     * populates list of {@code ISSUES} from the specified root directory of device's local storage
     */
    public void loadIssues(Context context, String issuesRootDirectory) {
        ISSUES.clear();

        File f = new File(issuesRootDirectory);
        File[] files = f.listFiles();
        if (files != null) {
            for (File g : files) {
                if (g.isDirectory()) {
                    try {
                        Issue issue = getIssue(context, g);
                        addIssue(issue);

                    } catch (IOException e) {
                        LogHelper.e(TAG, e.getMessage());
                    }
                }
            }
        }

        // sorting
        Collections.sort(ISSUES, new Comparator<Issue>() {
            @Override
            public int compare(Issue issue1, Issue issue2) {
                return issue1.id - issue2.id;
            }
        });
    }

    /**
     * a mapping from issue root directories to {@link Issue}, to prevent duplicate objects of the same issue, and have the {@link Issue.OnStatusChangedListener} instance value stable.
     */
    static HashMap<File, Issue> file2issue = new HashMap<>();

    public static Issue getIssue(Context context, File issueRootDirectory) throws IOException {
        Issue issue = file2issue.get(issueRootDirectory);
        if (issue == null) {
            issue = new Issue();
        }

        File input = new File(issueRootDirectory, Issue.manifestFileName);
        final Document doc = Jsoup.parse(input, "UTF-8", "");

        Element e = doc.getElementsByTag("issue").first();
        if (e == null) {
            throw new IOException("Invalid manifest file.");
        }

        issue.rootDirectory = issueRootDirectory;
        issue.title = e.attr("title");
        issue.id = Integer.parseInt(issueRootDirectory.getName());
        if (issue.poster == null)
            issue.poster = BitmapFactory.decodeFile(new File(issue.rootDirectory, Magazines.Issue.posterFileName).getAbsolutePath());

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

        file2issue.put(issueRootDirectory, issue);
        return issue;
    }

    private void addIssue(Issue issue) {
        ISSUES.add(issue);
    }

    /**
     * Properties of a magazine issue
     */
    public static class Issue {
        /**
         * file containing issue properties
         */
        protected static final String manifestFileName = "manifest.xml";

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
        public String title;

        public File rootDirectory;

        /**
         * unique identifier which is also magazine root folder's name
         */
        public int id;

        public enum Status {
            header_active,
            active,
            header_other_saved,
            other_saved,
            header_downloading,
            downloading, // downloading or failed downloading
            header_available,
            available,
            completed_header, // not used, because completed issues are listed in their own tab
            completed,
        }

        private Status status;

        public Bitmap poster;

        public int getStatusValue() {
            return status.ordinal();
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
            for (OnStatusChangedListener listener : listeners)
                listener.onIssueStatusChanged(this);
        }

        @Override
        public String toString() {
            return title;
        }

        public void addOnStatusChangedListener(OnStatusChangedListener listener) {
            if (!listeners.contains(listener))
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

        protected ArrayList<OnStatusChangedListener> listeners = new ArrayList<>();
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
     * @return download reference number
     */
    public static long download(Context context, Issue issue) throws IOException {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(getIssueDownloadUrl(context, issue)))
                .setDescription(issue.title)
                .setTitle(context.getResources().getString(R.string.app_name))
                .setDestinationUri(Uri.fromFile(getIssueLocalDownloadUri(context, issue)))
                .setVisibleInDownloadsUi(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);

        final DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        issue.setStatus(Issue.Status.downloading);
        return downloadManager.enqueue(request);
    }

    /**
     * @return URL from which the zip archive of the given issue can be downloaded.
     */
    public static String getIssueDownloadUrl(Context context, Issue issue) {
        final Uri uri = Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString("server_address", context.getResources().getString(R.string.pref_default_server_address)));
        // http://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
        return uri.toString() + "/api/issues/" + Integer.parseInt(issue.rootDirectory.getName());
    }

    /**
     * @return download percentage
     */
    public static int getDownloadProgress(Context context, Issue issue) {
        final String issueDownloadUrl = getIssueDownloadUrl(context, issue);

        DownloadManager.Query query = new DownloadManager.Query();

        Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
        final int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI);

        int progress = 0;

        // TODO: consider when there may be more than one query result for a single URI
        while (cursor.moveToNext()) {
            final String cursorUrl = cursor.getString(uriIndex);
            if (cursorUrl.equals(issueDownloadUrl)) {
                int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                progress = (bytes_downloaded * 100 / bytes_total);
                break;
            }
        }

        cursor.close();
        return progress;
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
                    if (getIssueLocalDownloadUri(context, issue).exists()) {
                        status = -3; // custom value indicating that the issue is being extracted.
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

}
