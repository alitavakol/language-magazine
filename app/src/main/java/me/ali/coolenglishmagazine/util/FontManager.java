package me.ali.coolenglishmagazine.util;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Taken from <a href="http://code.tutsplus.com/tutorials/how-to-use-fontawesome-in-an-android-app--cms-24167">How to Use FontAwesome in an Android App?</a>
 */
public class FontManager {

    public static final String UBUNTU = "Ubuntu-Regular.ttf";
    public static final String UBUNTU_BOLD = "Ubuntu-Bold.ttf";
    public static final String UBUNTU_LIGHT = "Ubuntu-Light.ttf";
    public static final String UBUNTU_ITALIC = "Ubuntu-Italic.ttf";
    public static final String UBUNTU_MEDIUM = "Ubuntu-Medium.ttf";
//    public static final String ADOBE_ARABIC_REGULAR = "AdobeArabic-Bold.ttf";

    public static Typeface getTypeface(Context context, String font) {
        return Typeface.createFromAsset(context.getAssets(), font);
    }

    public static void markAsIconContainer(View v, Typeface typeface) {
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                markAsIconContainer(child, typeface);
            }
        } else if (v instanceof TextView) {
            ((TextView) v).setTypeface(typeface);
        }
    }

}