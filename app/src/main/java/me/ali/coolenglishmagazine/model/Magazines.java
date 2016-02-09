package me.ali.coolenglishmagazine.model;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
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
    public void loadIssues(String issuesRootDirectory) {
        ISSUES.clear();

        File f = new File(issuesRootDirectory);
        File[] files = f.listFiles();
        if (files != null) {
            for (File g : files) {
                if (g.isDirectory()) {
                    try {
                        addIssue(getIssue(g));
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

    public static Issue getIssue(File issueRootDirectory) throws IOException {
        File input = new File(issueRootDirectory, Issue.manifestFileName);
        final Document doc = Jsoup.parse(input, "UTF-8", "");

        Element e = doc.getElementsByTag("issue").first();
        if (e == null)
            throw new IOException("Invalid manifest file.");

        Issue issue = new Issue();

        issue.rootDirectory = issueRootDirectory;
        issue.title = e.attr("title");
        issue.id = Integer.parseInt(issueRootDirectory.getName());

        issue.status = issue.id == 1 ? Issue.Status.active : Issue.Status.other_saved;

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
            header_completed,
            completed,
            header_downloading,
            downloading,
            header_available,
            available,
        }

        public Status status;

        @Override
        public String toString() {
            return title;
        }
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
     * @return download status (if there is any) or -1
     */
    public static int getDownloadStatus(Context context, Issue issue) {
        final String issueDownloadUrl = getIssueDownloadUrl(context, issue);

        DownloadManager.Query query = new DownloadManager.Query();

        Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
        final int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI);

        while (cursor.moveToNext()) {
            final String cursorUrl = cursor.getString(uriIndex);
            if (cursorUrl.equals(issueDownloadUrl)) {
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    if (getIssueLocalDownloadUri(context, issue).exists()) {
                        status = -3; // custom value indicating that the issue is being extracted.
                    }
                }
                return status;
            }
        }

        cursor.close();
        return -1;
    }

    /**
     * @return download status (if there is any) or -1
     */
    public static long getDownloadReference(Context context, Issue issue) {
        final String issueDownloadUrl = getIssueDownloadUrl(context, issue);

        DownloadManager.Query query = new DownloadManager.Query();

        Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
        final int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI);

        while (cursor.moveToNext()) {
            final String cursorUrl = cursor.getString(uriIndex);
            if (cursorUrl.equals(issueDownloadUrl)) {
                return cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
            }
        }

        cursor.close();
        return -1;
    }

}
