package me.ali.coolenglishmagazine.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import me.ali.coolenglishmagazine.util.LogHelper;

public class MagazineContent {

    private static final String TAG = LogHelper.makeLogTag(MagazineContent.class);

    /**
     * maximum number of items in one issue
     */
    public static final int MAX_ITEMS = 50;

    /**
     * An array of available magazine items in this issue.
     */
    public List<Item> ITEMS = new ArrayList<>();

    /**
     * holds items in memory to reference them efficiently.
     */
    public static HashMap<File, Item> file2item = new HashMap<>();

    public void loadItems(Magazines.Issue issue) {
        File[] files = issue.rootDirectory.listFiles();

        for (File g : files) {
            if (g.isDirectory()) {
                try {
                    final Item item = getItem(g);
                    addItem(item);

                } catch (IOException e) {
                    LogHelper.e(TAG, e.getMessage());
                }
            }
        }

        // sorting
        Collections.sort(ITEMS, new Comparator<Item>() {
            @Override
            public int compare(Item item1, Item item2) {
                return item1.id - item2.id;
            }
        });
    }

    /**
     * checks signatures to find if paid content and free content are valid
     */
    public void validateSignatures(Context context, Magazines.Issue issue) {
        byte[] manifestFileBytes = new byte[1000]; // bytes of the manifest.xml

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final byte[] userId = preferences.getString("user_id", "").getBytes();

        issue.freeContentIsValid = false;
        issue.paidContentIsValid = false;

        try {
            MessageDigest digest_free = MessageDigest.getInstance("MD5");
            digest_free.update(issue.title.getBytes());

            MessageDigest digest_paid = MessageDigest.getInstance("MD5");
            digest_paid.update(issue.title.getBytes());

            for (Item item : ITEMS) {
                // take bytes of this item's manifest.xml for hash computation
                final File file = new File(item.rootDirectory, Item.manifestFileName);
                BufferedInputStream manifestFileStream = new BufferedInputStream(new FileInputStream(file));
                final int length = (int) file.length();
                if (manifestFileBytes.length < length)
                    manifestFileBytes = new byte[length];
                manifestFileStream.read(manifestFileBytes, 0, length);

                // append length of content.html.
                // so, user cannot replace content with another one
                final File contentFile = new File(item.rootDirectory, Item.contentFileName);

                if (item.free) {
                    digest_free.update(manifestFileBytes, 0, length);
                    digest_free.update(Long.toString(contentFile.length()).getBytes());

                } else {
                    digest_paid.update(Long.toString(contentFile.length()).getBytes());
                    digest_paid.update(manifestFileBytes, 0, length);
                    digest_paid.update(userId);
                }
            }

            byte[] hash = digest_free.digest();
            StringBuilder sb = new StringBuilder(32 + 1);
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            final File signatureFreeFile = new File(issue.rootDirectory, Magazines.Issue.signatureFreeFileName);
            FileInputStream f = new FileInputStream(signatureFreeFile);
            byte[] t = new byte[(int) signatureFreeFile.length()];
            f.read(t);
            f.close();
            issue.freeContentIsValid = sb.toString().equals(new String(t));

            hash = digest_paid.digest();
            sb = new StringBuilder(32 + 1);
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            final File signaturePaidFile = new File(issue.rootDirectory, Magazines.Issue.signaturePaidFileName);
            if (signaturePaidFile.exists()) {
                f = new FileInputStream(signaturePaidFile);
                t = new byte[(int) signaturePaidFile.length()];
                f.read(t);
                f.close();
                issue.paidContentIsValid = sb.toString().equals(new String(t));
            }

        } catch (Exception e) {
            ITEMS.clear();
            e.printStackTrace();
        }
    }

    /**
     * Loads item from its root directory into an {@link Item} class.
     *
     * @param itemRootDirectory should end with a slash '/' character
     * @return the corresponding {@link Item} instance
     * @throws IOException if a valid {@link me.ali.coolenglishmagazine.model.Magazines.Issue#manifestFileName} with {@code <item>} root node could not be found
     */
    public static Item getItem(File itemRootDirectory) throws IOException {
        Item item = file2item.get(itemRootDirectory);

        // verify issue integrity,
        // and delete issue on error
        if (!new File(itemRootDirectory, Item.contentFileName).exists()) {
            throw new IOException("Could not find item content file: " + itemRootDirectory);
        }

        if (item == null) {
            File input = new File(itemRootDirectory, Item.manifestFileName);
            final Document doc = Jsoup.parse(input, "UTF-8", "");

            Element e = doc.getElementsByTag("item").first();
            if (e == null)
                throw new IOException("Invalid manifest file.");

            item = new Item();

            item.rootDirectory = itemRootDirectory;
            item.id = Integer.parseInt(itemRootDirectory.getName());
            item.title = e.attr("title");
            item.subtitle = e.attr("subtitle");
            item.audioFileName = e.attr("audio");
            item.posterFileName = e.attr("poster");
            item.flagFileName = e.attr("flag");
            item.type = e.attr("type");
            item.level = Integer.parseInt(e.attr("level"));
            item.free = Boolean.parseBoolean(e.attr("free"));
            item.version = Integer.parseInt(e.attr("version"));

            String duration = e.attr("duration");
            if (duration != null && duration.length() > 0)
                item.duration = Integer.parseInt(duration);

            file2item.put(itemRootDirectory, item);
        }

        if (item.audioFileName.length() > 0 && !new File(itemRootDirectory, item.audioFileName).exists())
            throw new IOException("Cannot find item audio file.");

        return item;
    }

    private void addItem(Item item) {
        ITEMS.add(item);
    }

    public static class Item {
        public File rootDirectory;

        /**
         * A dummy item representing a piece of content.
         */
        public String audioFileName;

        public String posterFileName;

        /**
         * accent shown by country flag
         */
        public String flagFileName;

        /**
         * file containing lesson item properties
         */
        protected static final String manifestFileName = "manifest.xml";

        /**
         * main item content
         */
        public static final String contentFileName = "content.html";

        public String title;
        public String subtitle;
        public String type;
        public int level;
        public int id;

        /**
         * if false, this item is available only if it is paid for.
         */
        public boolean free;

        /**
         * item version, which if greater than app version code, app should be updated.
         */
        public int version;

        /**
         * UID of an item is unique across all available items of all issues. this is calculated
         * using values of {@link MagazineContent#MAX_ITEMS} and {@link Magazines#MAX_ISSUES}.
         *
         * @return uid of an item, which is unique across all available items.
         */
        public int getUid() {
            return MAX_ITEMS * Integer.parseInt(rootDirectory.getParentFile().getName()) + id;
        }

        /**
         * @return the {@link me.ali.coolenglishmagazine.model.Magazines.Issue} to which this item belongs.
         */
        public Magazines.Issue getIssue(Context context) throws IOException {
            return Magazines.getIssue(context, rootDirectory.getParentFile());
        }

        /**
         * audio duration in milliseconds
         */
        public int duration;
    }
}
