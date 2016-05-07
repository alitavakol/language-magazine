package me.ali.coolenglishmagazine.model;

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

import me.ali.coolenglishmagazine.util.LogHelper;

public class MagazineContent {

    private static final String TAG = LogHelper.makeLogTag(MagazineContent.class);

    /**
     * maximum number of items in one issue
     */
    public static final int MAX_ITEMS = 10;

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
                    addItem(getItem(g));
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
     * Loads item from its root directory into an {@link Item} class.
     *
     * @param itemRootDirectory should end with a slash '/' character
     * @return the corresponding {@link Item} instance
     * @throws IOException if {@code manifest.xml} file with {@code <item>} root node could not be found
     */
    public static Item getItem(File itemRootDirectory) throws IOException {
        Item item = file2item.get(itemRootDirectory);

        // verify issue integrity,
        // and delete issue on error
        if (!new File(itemRootDirectory, Item.contentFileName).exists()) {
            throw new IOException("Cannot find item content file.");
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

            file2item.put(itemRootDirectory, item);
        }

        if (item.audioFileName.length() > 0 && !new File(itemRootDirectory, item.audioFileName).exists())
            throw new IOException("Cannot not find item audio file.");

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
         * UID of an item is unique across all available items of all issues. this is calculated
         * using values of {@code MagazineContent.MAX_ITEMS} and {@code Magazines.MAX_ISSUES}.
         *
         * @return uid of an item, which is unique across all available items.
         */
        public int getUid() {
            return MAX_ITEMS * Integer.parseInt(rootDirectory.getParentFile().getName()) + id;
        }
    }
}
