package me.ali.coolenglishmagazine.data;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.ali.coolenglishmagazine.util.LogHelper;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p/>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class MagazineContent {

    private static final String TAG = LogHelper.makeLogTag(MagazineContent.class);

    /**
     * An array of sample (dummy) items.
     */
    public List<Item> ITEMS = new ArrayList<>();

    public void loadItems(String magazineRootDirectory) {
        File f = new File(magazineRootDirectory);
        File[] files = f.listFiles();
        for (File g : files) {
            if (g.isDirectory()) {
                try {
                    addItem(getItem(g.getAbsolutePath() + "/"));
                } catch (IOException e) {
                    LogHelper.e(TAG, e.getMessage());
                }
            }
        }
    }

    public static Item getItem(String itemRootDirectory) throws IOException {
        Item item = new Item();

        File input = new File(itemRootDirectory, "manifest.xml");
        final Document doc = Jsoup.parse(input, "UTF-8", "");

        Element e = doc.getElementsByTag("item").first();
        if (e == null)
            throw new IOException();

        item.rootDirectory = itemRootDirectory;
        item.title = e.attr("title");
        item.subtitle = e.attr("subtitle");
        item.transcriptFileName = e.attr("content");
        item.audioFileName = e.attr("audio");
        item.posterFileName = e.attr("poster");

        return item;
    }

    private void addItem(Item item) {
        ITEMS.add(item);
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class Item {
        public String title;
        public String subtitle;

        public String audioFileName;
        public String transcriptFileName;
        public String posterFileName;

        public String rootDirectory;

        @Override
        public String toString() {
            return title;
        }
    }
}
