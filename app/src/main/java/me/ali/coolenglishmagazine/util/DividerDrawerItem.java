package me.ali.coolenglishmagazine.util;

import android.support.annotation.LayoutRes;

import me.ali.coolenglishmagazine.R;

/**
 * Created by hamed on 6/25/16.
 */
public class DividerDrawerItem extends com.mikepenz.materialdrawer.model.DividerDrawerItem {
    @Override
    @LayoutRes
    public int getLayoutRes() {
        return R.layout.material_drawer_item_divider;
    }
}
