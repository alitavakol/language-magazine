package me.ali.coolenglishmagazine.model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.ali.coolenglishmagazine.util.LogHelper;

public class MagazineContent {

    private static final String TAG = LogHelper.makeLogTag(MagazineContent.class);

    /**
     * An array of available magazine items.
     */
    public List<Item> ITEMS = new ArrayList<>();

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
        File input = new File(itemRootDirectory, Item.manifestFileName);
        final Document doc = Jsoup.parse(input, "UTF-8", "");

        Element e = doc.getElementsByTag("item").first();
        if (e == null)
            throw new IOException("Invalid manifest file.");

        Item item = new Item();

        item.rootDirectory = itemRootDirectory;
        item.id = Integer.parseInt(itemRootDirectory.getName());
        item.title = e.attr("title");
        item.subtitle = e.attr("subtitle");
        item.audioFileName = e.attr("audio");
        item.posterFileName = e.attr("poster");
        item.flagFileName = e.attr("flag");
        item.type = e.attr("type");
        item.level = Integer.parseInt(e.attr("level"));

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
    }
}
