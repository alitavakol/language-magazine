package me.ali.coolenglishmagazine.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
     * @return first encountered file/folder
     * @throws IOException
     */
    public static File unzip(File zipFile, File targetDirectory) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        File rootFile = null;

//        try {
        ZipEntry ze;
        int count;
        byte[] buffer = new byte[8192];

        while ((ze = zis.getNextEntry()) != null) {
            File file = new File(targetDirectory, ze.getName());
            File dir = ze.isDirectory() ? file : file.getParentFile();

            if (rootFile == null)
                rootFile = file;

            if (!dir.isDirectory() && !dir.mkdirs())
                throw new FileNotFoundException("Failed to ensure directory: " +
                        dir.getAbsolutePath());

            if (ze.isDirectory())
                continue;

            FileOutputStream fout = new FileOutputStream(file);

//                try {
            while ((count = zis.read(buffer)) != -1)
                fout.write(buffer, 0, count);
//                } finally {
            fout.close();
//                }

            // if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
        }

//        } finally {
        zis.close();
//        }

        return rootFile;
    }

}
