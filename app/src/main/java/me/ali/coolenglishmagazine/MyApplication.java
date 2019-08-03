package me.ali.coolenglishmagazine;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import me.ali.coolenglishmagazine.util.FontManager;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath(FontManager.UBUNTU)
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

        // upgrade to pro version
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit()
                .putBoolean("upgrade_required", true)
                .putBoolean("gem_seen", true)
                .apply();

        // restart alarms if they have not started
        AlarmsTabFragment.startAllAlarms(this);
    }
}
