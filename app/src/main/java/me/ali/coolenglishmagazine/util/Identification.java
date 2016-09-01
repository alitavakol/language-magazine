package me.ali.coolenglishmagazine.util;

import android.content.Context;
import android.provider.Settings;

import java.security.MessageDigest;

import me.ali.coolenglishmagazine.BuildConfig;
import me.ali.coolenglishmagazine.R;

/**
 * Created by hamed on 8/21/16.
 * http://stackoverflow.com/a/17625641/1994239
 */
public class Identification {
    /**
     * Return pseudo unique ID prepended by its MD5 checksum for validation
     *
     * @return ID
     * @throws Exception if ANDROID_ID cannot be found
     */
    public static String getUniqueDeviceID(Context context) throws Exception {
        String serial = android.os.Build.class.getField("SERIAL").get(null).toString();
        if (!BuildConfig.DEBUG && serial.contains("unknown"))
            throw new Exception("Serial is not defined.");

        final String android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (android_id == null)
            throw new Exception("ANDROID_ID is not defined.");
        serial += android_id;

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(serial.getBytes());
        byte[] hash = md5.digest();
        StringBuilder sb = new StringBuilder(32 + 1);
        for (byte b : hash)
            sb.append(String.format("%02x", b));

        // Go ahead and return the serial for api => 9
        return sb.substring(0, 14) + serial;
    }

    public static String getGemUrl(Context context) throws Exception {
        return "http://" + context.getString(R.string.gem_host) + context.getString(R.string.gem_path_prefix) + "?id=" + getUniqueDeviceID(context);
    }
}
