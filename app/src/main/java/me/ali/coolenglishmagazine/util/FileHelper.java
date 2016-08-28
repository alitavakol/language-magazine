package me.ali.coolenglishmagazine.util;

import java.io.File;

public class FileHelper {

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        delete(fileOrDirectory);
    }

    /**
     * delete file and kill references.
     * see <a href="http://stackoverflow.com/a/11776458">here</a> for more information.
     *
     * @param file file to delete
     */
    public static void delete(File file) {
        if (file != null) {
            final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
            if (file.renameTo(to))
                to.delete();
        }
    }

}
