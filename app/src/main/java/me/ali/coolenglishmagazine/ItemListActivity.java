package me.ali.coolenglishmagazine;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import me.ali.coolenglishmagazine.model.MagazineContent;
import me.ali.coolenglishmagazine.util.Account;
import me.ali.coolenglishmagazine.util.LogHelper;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


/**
 * An activity representing a list of Magazine. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * TODO: fix the following line
 * lead to a ???? activity representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ItemListFragment} and the item details
 * TODO: fix the following line
 * (if present) is an item detail fragment ????.
 * <p/>
 * This activity also implements the required
 * {@link ItemListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ItemListActivity extends AppCompatActivity implements
        ItemListFragment.Callbacks,
        Account.Callbacks {

    private static final String TAG = LogHelper.makeLogTag(ItemListActivity.class);

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_app_bar);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        if (!getResources().getBoolean(R.bool.isTablet) && getResources().getBoolean(R.bool.isLandscape)) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
            if (params != null) {
                params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
                toolbar.setLayoutParams(params);
            }
        }

        TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getTitle());

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true); // Enable the Up button
            ab.setDisplayShowTitleEnabled(false);
            ab.setHomeAsUpIndicator(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_arrow_back).sizeDp(24).paddingDp(4).colorRes(R.color.md_light_appbar));
        }

        Bundle arguments = new Bundle();

        final String issueRootDirectory = getIntent().getStringExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY);
        arguments.putString(IssueDetailActivity.ARG_ROOT_DIRECTORY, issueRootDirectory);

        itemListFragment = new ItemListFragment();
        itemListFragment.setArguments(arguments);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, itemListFragment)
                .commit();

        account = new Account(this);
    }

    ItemListFragment itemListFragment;

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        recreate();
    }

    /**
     * Callback method from {@link ItemListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(MagazineContent.Item item) {
        // In single-pane mode, simply start the detail activity
        // for the selected item ID.
        Intent intent = new Intent(this, ReadAndListenActivity.class);
        intent.putExtra(ReadAndListenActivity.ARG_ROOT_DIRECTORY, item.rootDirectory.getAbsolutePath());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.common, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();

                // when this activity is launched from the notification, back button goes to home screen.
                // I could not find any solution except manually creating parent.
                Intent intent = new Intent(this, RootActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);

                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Google APIs account sign-in helper
     */
    public Account account;

    @Override
    public void onStart() {
        super.onStart();
        account.silentSignIn();
    }

    public void showProgressDialog() {
    }

    public void hideProgressDialog() {
    }

    public void updateProfileInfo(String personPhoto, String displayName, String email, String userId, boolean signedOut) {
        if (itemListFragment != null && !account.silentlySigningIn)
            itemListFragment.signatureChanged(this);
    }

    public void signingIn(boolean signingIn) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        account.onActivityResult(requestCode, resultCode, data);
    }
}
