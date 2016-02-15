package me.ali.coolenglishmagazine;

import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.mikepenz.materialdrawer.DrawerBuilder;

import me.ali.coolenglishmagazine.util.LogHelper;

public class RootActivity extends AppCompatActivity {

    private static final String TAG = LogHelper.makeLogTag(RootActivity.class);

    protected Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        toolbar.setTitle(getTitle());

        new DrawerBuilder().withActivity(this).withToolbar(toolbar).build();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.root_fragment, GalleryOfIssuesFragment.newInstance(null, null))
                    .commit();
        }
    }

}
