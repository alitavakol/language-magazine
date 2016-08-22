package me.ali.coolenglishmagazine.util;

import android.content.Context;
import android.provider.Settings;

import java.security.MessageDigest;

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
     */
    public static String getUniqueDeviceID(Context context) {
        // Thanks to @Roman SL!
        // http://stackoverflow.com/a/4789483/950427
        // Only devices with API >= 9 have android.os.Build.SERIAL
        // http://developer.android.com/reference/android/os/Build.html#SERIAL
        // If a user upgrades software or roots their device, there will be a duplicate entry
        try {
            String serial = android.os.Build.class.getField("SERIAL").get(null).toString();

            final String android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (android_id != null)
                serial += android_id;

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(serial.getBytes());
            byte[] hash = md5.digest();
            StringBuilder sb = new StringBuilder(32 + 1);
            for (byte b : hash)
                sb.append(String.format("%02x", b));

            // Go ahead and return the serial for api => 9
            return sb.substring(24) + serial;

        } catch (Exception e) {
            return null;
        }
    }

    public static String getGemUrl(Context context) {
        return "http://" + context.getString(R.string.gem_host) + context.getString(R.string.gem_path_prefix) + "?id=" + getUniqueDeviceID(context);
    }
}
