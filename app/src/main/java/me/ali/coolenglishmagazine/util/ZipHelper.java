package me.ali.coolenglishmagazine.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Zip file extractor
 * Created by Ali on 1/11/16.
 */
public class ZipHelper {

    /**
     * This code is based on <a href="http://stackoverflow.com/a/27050680">this link</a>.
     *
     * @param zipFile         file to extract
     * @param targetDirectory destination
     * @param deleteOnError   delete root file of extraction on failure
     * @return first encountered file/folder or null on failure
     */
    public static File unzip(File zipFile, File targetDirectory, boolean deleteOnError) {
        File rootFile = null;

        try {
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));

            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];

            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();

                if (!dir.mkdirs() && !dir.isDirectory())
                    throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());

                if (rootFile == null)
                    rootFile = dir;

                if (ze.isDirectory())
                    continue;

                FileOutputStream outputStream = new FileOutputStream(file);
                while ((count = zis.read(buffer)) != -1)
                    outputStream.write(buffer, 0, count);
                outputStream.close();
            }

            zis.close();

        } catch (Exception e) {
            if (deleteOnError && rootFile != null)
                FileHelper.delete(rootFile);
            rootFile = null;
        }

        return rootFile;
    }

}
