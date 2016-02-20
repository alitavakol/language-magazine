package me.ali.coolenglishmagazine;

import android.app.Application;

import me.ali.coolenglishmagazine.util.FontManager;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath(FontManager.ROBOTO)
                .setFontAttrId(R.attr.fontPath)
                .build()
        );
    }
}
