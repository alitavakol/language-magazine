package me.ali.coolenglishmagazine;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.farsitel.bazaar.IUpdateCheckService;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.OnPostBindViewListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.Locale;

import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.model.WaitingItems;
import me.ali.coolenglishmagazine.util.Account;
import me.ali.coolenglishmagazine.util.Blinker;
import me.ali.coolenglishmagazine.util.DividerDrawerItem;
import me.ali.coolenglishmagazine.util.FontManager;
import me.ali.coolenglishmagazine.util.LogHelper;
import me.ali.coolenglishmagazine.util.NetworkHelper;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class RootActivity extends AppCompatActivity implements
        Account.Callbacks,
        GalleryOfIssuesFragment.OnFragmentInteractionListener,
        CoolEnglishTimesFragment.OnFragmentInteractionListener,
        ReadmeFragment.OnFragmentInteractionListener,
        AboutFragment.OnFragmentInteractionListener {

    private static final String TAG = LogHelper.makeLogTag(RootActivity.class);

    /**
     * intent action that activates available issues tab inside gallery of issues fragment, to show downloads in progress
     */
    public static final String ACTION_SHOW_DOWNLOADS = "me.ali.coolenglishmagazine.ACTION_SHOW_DOWNLOADS";

    /**
     * intent action that activates readme fragment
     */
    public static final String ACTION_SHOW_README = "me.ali.coolenglishmagazine.ACTION_SHOW_README";

    /**
     * intent action that activates hot english times fragment
     */
    public static final String ACTION_SHOW_TIMES = "me.ali.coolenglishmagazine.ACTION_SHOW_TIMES";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean mTwoPane;

    GalleryOfIssuesFragment galleryOfIssuesFragment;
    CoolEnglishTimesFragment coolEnglishTimesFragment;
    Fragment aboutFragment, readmeFragment;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // force change locale based on value of "locale" preference
        String languageToLoad = preferences.getString("locale", "fa");
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration(); // http://stackoverflow.com/a/24908330
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);

        preferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

        setContentView(R.layout.activity_root);

        if (savedInstanceState != null) {
            drawerSelection = savedInstanceState.getInt("drawerSelection");
            signingIn = savedInstanceState.getBoolean("signing_in");
            newIssuesAvailableWarningShown = savedInstanceState.getBoolean("newIssuesAvailableWarningShown");

        } else {
            final String action = getIntent().getAction();
            Fragment fragment;
            String tag;

            if (ACTION_SHOW_README.equals(action)) {
                readmeFragment = ReadmeFragment.newInstance();
                fragment = readmeFragment;
                tag = ReadmeFragment.FRAGMENT_TAG;
                drawerSelection = 3;

            } else if (ACTION_SHOW_TIMES.equals(action)) {
                coolEnglishTimesFragment = CoolEnglishTimesFragment.newInstance(0);
                fragment = coolEnglishTimesFragment;
                tag = CoolEnglishTimesFragment.FRAGMENT_TAG;
                drawerSelection = 2;

            } else {
                // show the gallery of issues fragment by default
                galleryOfIssuesFragment = GalleryOfIssuesFragment.newInstance(ACTION_SHOW_DOWNLOADS.equals(action) ? 1 : 0);
                fragment = galleryOfIssuesFragment;
                tag = GalleryOfIssuesFragment.FRAGMENT_TAG;
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.root_fragment, fragment, tag)
                    .commit();
        }

        setupNavigationDrawer();

//        account = new Account(this);

        if (savedInstanceState == null) {
            initUpdateCheckService();
            newIssuesAvailableWarningShown = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        releaseUpdateCheckService();
    }

    protected SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("locale"))
                recreate();

            else if (key.equals("new_saved_issues") && galleryOfIssuesFragment != null) {
                galleryOfIssuesFragment.updateBlinker(RootActivity.this, IssuesTabFragment.SAVED_ISSUES);
                if (coolEnglishTimesFragment != null)
                    coolEnglishTimesFragment.updateBlinker(RootActivity.this, 0);
            }
        }
    };

    ImageView profilePicture;
    TextView userName, userEmail;
    CircularProgressView circularProgressView;

    static int[] drawerIconBlinkPriorities = new int[4];

    /**
     * index of drawer item which needs user attention; and drawer indicator is blinking for it.
     */
    int attentionIndex;

    class OnPostBindDrawerIconListener implements OnPostBindViewListener {
        int index;
        Blinker blinker = new Blinker();

        public OnPostBindDrawerIconListener(int drawerIndex) {
            index = drawerIndex;
        }

        @Override
        public void onBindView(IDrawerItem drawerItem, View view) {
            int priority;

            if (blinker != null)
                blinker.stop();

            switch (index) {
                case 1: // gallery of issues
                    if (galleryOfIssuesFragment == null || galleryOfIssuesFragment.adapter == null)
                        return;

                    boolean blink = false;
                    for (Boolean b : galleryOfIssuesFragment.adapter.userAttention)
                        blink = blink || b;
                    if (!blink)
                        return;

                    priority = 10;
                    break;

                case 2: // hot english times
                    WaitingItems.importWaitingItems(RootActivity.this);
                    AlarmsTabFragment.importAlarms(RootActivity.this);
                    if (WaitingItems.waitingItems.size() == 0 || AlarmsTabFragment.alarms.size() > 0)
                        return;

                    priority = 8;
                    break;

                case 3: // readme
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(RootActivity.this);
                    if (preferences.getBoolean("readme_seen", false))
                        return;

                    priority = 9;
                    break;

                default:
                    return;
            }

            drawerIconBlinkPriorities[index] = priority;

            if (view != null)
                blinker.setBlinkingView(((ViewGroup) view).getChildAt(0));

            int j = 0;
            for (int i = 1; i < drawerIconBlinkPriorities.length; i++) {
                if (drawerIconBlinkPriorities[i] > drawerIconBlinkPriorities[j])
                    j = i;
            }
            if (j == index)
                blinker.start();
        }
    }

    OnPostBindDrawerIconListener[] drawerIconListeners = new OnPostBindDrawerIconListener[4];

    public void updateIconBlinkers() {
        attentionIndex = -1;
        for (int i = 0; i < drawerIconBlinkPriorities.length; i++)
            drawerIconBlinkPriorities[i] = 0;

        for (OnPostBindDrawerIconListener listener : drawerIconListeners)
            if (listener != null)
                listener.onBindView(null, null);

        // again after updating priorities
        for (OnPostBindDrawerIconListener listener : drawerIconListeners)
            if (listener != null)
                listener.onBindView(null, null);

        attentionIndex = -1;
        int j = 0;
        for (int i = 1; i < drawerIconBlinkPriorities.length; i++) {
            if (drawerIconBlinkPriorities[i] > drawerIconBlinkPriorities[j]) {
                attentionIndex = i;
                j = i;
            }
        }

        if (attentionIndex != -1 && drawerSelection != attentionIndex) {
            if (drawerIndicatorBlinker == null) {
                drawerIndicatorBlinker = new Blinker();
                drawerIndicatorBlinker.setOnTimerShot(onBlinkerTimerShot);
            }
            drawerIndicatorBlinker.start();
        }

        if (drawerIndicatorBlinker != null
                && (attentionIndex == -1 || drawerSelection == attentionIndex)) {
            drawerIndicatorBlinker.stop();
        }
    }

    Blinker drawerIndicatorBlinker;
    Blinker.OnTimerShot onBlinkerTimerShot = new Blinker.OnTimerShot() {
        BitmapDrawable dummy = new BitmapDrawable();

        @Override
        public void onTimerShot(Blinker blinker) {
            drawer.getActionBarDrawerToggle().setHomeAsUpIndicator(dummy);
            drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(blinker.visible || blinker.stopped);
        }

        @Override
        public void onStop() {
            drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
        }
    };

    protected void setupNavigationDrawer() {
        // manually load drawer header, and apply custom typeface to it.
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        ViewGroup headerView = (ViewGroup) inflater.inflate(R.layout.drawer_header, null);

        profilePicture = (ImageView) headerView.findViewById(R.id.profile_image);
        userName = (TextView) headerView.findViewById(R.id.user_name);
        userEmail = (TextView) headerView.findViewById(R.id.user_email);
        circularProgressView = (CircularProgressView) headerView.findViewById(R.id.progress_bar);

//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
//        updateProfileInfo(preferences.getString("user_image", null), preferences.getString("user_name", null), preferences.getString("user_email", null), preferences.getString("user_id_token", null));

        if (signingIn)
            showProgressDialog();

        ((TextView) headerView.findViewById(R.id.app_name)).setTypeface(FontManager.getTypeface(getApplicationContext(), FontManager.UBUNTU_BOLD));

        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (!hasSavedLogIn)
//                    account.signIn();
            }
        });
        headerView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
//                if (!hasSavedLogIn)
//                    return false;
//
//                AlertDialog.Builder builder = new AlertDialog.Builder(RootActivity.this);
//                builder.setMessage(R.string.sign_out_warning)
//                        .setTitle(R.string.sign_out_warning_title)
//                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                account.signOut();
//                            }
//                        })
//                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                            }
//                        })
//                        .setCancelable(true)
//                        .show();
//                return true;
                return false;
            }
        });

        drawerIconListeners[1] = new OnPostBindDrawerIconListener(1);
        final PrimaryDrawerItem galleryOfIssues = new PrimaryDrawerItem().withName(R.string.gallery_of_issues).withIcon(GoogleMaterial.Icon.gmd_playlist_play).withSelectedColorRes(R.color.primary).withPostOnBindViewListener(drawerIconListeners[1]).withIdentifier(1);

        drawerIconListeners[2] = new OnPostBindDrawerIconListener(2);
        final PrimaryDrawerItem englishTimes = new PrimaryDrawerItem().withName(R.string.cool_english_times).withIcon(GoogleMaterial.Icon.gmd_alarm).withSelectedColorRes(R.color.primary).withPostOnBindViewListener(drawerIconListeners[2]).withIdentifier(2);

        drawerIconListeners[3] = new OnPostBindDrawerIconListener(3);
        final PrimaryDrawerItem readme = new PrimaryDrawerItem().withName(R.string.readme).withIcon(GoogleMaterial.Icon.gmd_sentiment_satisfied).withSelectedColorRes(R.color.primary).withPostOnBindViewListener(drawerIconListeners[3]).withIdentifier(3);

        final PrimaryDrawerItem about = new PrimaryDrawerItem().withName(R.string.about).withIcon(GoogleMaterial.Icon.gmd_info_outline).withSelectedColorRes(R.color.primary).withIdentifier(5);

        drawer = new DrawerBuilder()
                .withSliderBackgroundColorRes(R.color.accent)
                .withHeaderDivider(false)
                .withActivity(this)
                .withHasStableIds(true)
                .withHeader(headerView)
                .withShowDrawerOnFirstLaunch(true)
                .addDrawerItems(
                        galleryOfIssues,
                        englishTimes,
                        readme,
                        new DividerDrawerItem(),
                        about
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerSelection == position)
                            return false;

                        Fragment fragment = null;
                        String tag = null;

                        switch (position) {
                            case 1:
                                tag = GalleryOfIssuesFragment.FRAGMENT_TAG;
                                galleryOfIssuesFragment = (GalleryOfIssuesFragment) getSupportFragmentManager().findFragmentByTag(tag);
                                if (galleryOfIssuesFragment == null)
                                    galleryOfIssuesFragment = GalleryOfIssuesFragment.newInstance(0);
                                fragment = galleryOfIssuesFragment;
                                break;

                            case 2:
                                tag = CoolEnglishTimesFragment.FRAGMENT_TAG;
                                coolEnglishTimesFragment = (CoolEnglishTimesFragment) getSupportFragmentManager().findFragmentByTag(tag);
                                if (coolEnglishTimesFragment == null)
                                    coolEnglishTimesFragment = CoolEnglishTimesFragment.newInstance(0);
                                fragment = coolEnglishTimesFragment;
                                break;

                            case 3:
                                tag = ReadmeFragment.FRAGMENT_TAG;
                                readmeFragment = getSupportFragmentManager().findFragmentByTag(tag);
                                if (readmeFragment == null)
                                    readmeFragment = ReadmeFragment.newInstance();
                                fragment = readmeFragment;
                                break;

                            case 5:
                                tag = AboutFragment.FRAGMENT_TAG;
                                aboutFragment = getSupportFragmentManager().findFragmentByTag(tag);
                                if (aboutFragment == null)
                                    aboutFragment = AboutFragment.newInstance();
                                fragment = aboutFragment;
                                break;
                        }

                        if (fragment != null) {
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.root_fragment, fragment, tag)
                                    .commit();
                        } else {
                            Toast.makeText(RootActivity.this, "item clicked: " + position, Toast.LENGTH_SHORT).show();
                        }

                        drawerSelection = position;
                        return false;
                    }
                })
                .withSelectedItemByPosition(drawerSelection)
                .build();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (drawer != null) {
            outState.putInt("drawerSelection", drawer.getCurrentSelectedPosition());
            outState.putBoolean("signing_in", signingIn);
            outState.putBoolean("newIssuesAvailableWarningShown", newIssuesAvailableWarningShown);
        }
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
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * navigation drawer
     */
    Drawer drawer;

    /**
     * selected drawer item position
     */
    int drawerSelection = 1;

    public void onToolbarCreated(Toolbar toolbar, int titleRes) {
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayShowTitleEnabled(false);

        TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(titleRes);

        drawer.setToolbar(this, toolbar, true);
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();

        } else if (drawerSelection != 1 && drawer != null) {
            // when gallery of issues fragment is not active, navigate to it instead of exiting
            drawer.setSelectionAtPosition(1, true);

        } else {
            super.onBackPressed();
        }
    }

    public void onIssueSelected(Magazines.Issue issue) {
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putString(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());
            IssueDetailFragment fragment = new IssueDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.issue_detail_container, fragment)
                    .commit();

        } else {
            final File downloaded = new File(issue.rootDirectory, Magazines.Issue.downloadedFileName);
            if (downloaded.exists()) {
                // jump straight into issue's table of contents if it is downloaded
                Intent intent = new Intent(this, ItemListActivity.class);
                intent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());
                startActivity(intent);

            } else {
                // show intro and advertise the issue if it is not downloaded yet
                Intent intent = new Intent(this, IssueDetailActivity.class);
                intent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());
                startActivity(intent);
            }
        }
    }

    /**
     * Google APIs account sign-in helper
     */
    protected Account account;

    /**
     * shows whether or not user is signed in to their android account. user was logged-in before,
     * but the log-in status may not be up to date. it means that user's name, email and profile photo
     * is stored in preferences as a result of their past log-in.
     */
    boolean hasSavedLogIn;

    /**
     * if true, we are in the process of signing in. show progress indicator.
     */
    protected boolean signingIn;

    public void showProgressDialog() {
        circularProgressView.setVisibility(View.VISIBLE);
        profilePicture.setVisibility(View.GONE);
        circularProgressView.startAnimation();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (account != null)
            account.onActivityResult(requestCode, resultCode, data);
    }

    public void hideProgressDialog() {
        circularProgressView.setVisibility(View.GONE);
        profilePicture.setVisibility(View.VISIBLE);
        circularProgressView.clearAnimation();
    }

    /**
     * updates user profile info on drawer.
     */
    public void updateProfileInfo(String personPhoto, String displayName, String email, String userId, boolean signedOut) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        hasSavedLogIn = !signedOut && (userId != null || preferences.contains("user_email"));

        displayName = preferences.getString("user_name", getString(R.string.sign_in_name));
        email = preferences.getString("user_email", getString(R.string.sign_in_email));
        personPhoto = preferences.getString("user_image", null);

        if (personPhoto != null) {
            int w = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, getResources().getDisplayMetrics());
            Picasso
                    .with(this)
                    .load(Uri.parse(personPhoto))
                    .resize(w, w)
                    .centerCrop()
                    .into(profilePicture);
        } else {
            profilePicture.setImageDrawable(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_account_circle).sizeDp(36).colorRes(android.R.color.secondary_text_dark));
        }

        userName.setText(displayName);
        userEmail.setText(email);
    }

    @Override
    public void onStart() {
        // force change locale based on value of "locale" preference
        String languageToLoad = PreferenceManager.getDefaultSharedPreferences(this).getString("locale", "fa");
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration(); // http://stackoverflow.com/a/24908330
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        super.onStart();

        if (account != null)
            account.silentSignIn();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateIconBlinkers();
        drawer.getActionBarDrawerToggle().setHomeAsUpIndicator(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_language).sizeDp(24).paddingDp(4).colorRes(R.color.primary));
    }

    @Override
    public void onPause() {
        super.onPause();

        for (OnPostBindDrawerIconListener listener : drawerIconListeners) {
            if (listener != null && listener.blinker != null)
                listener.blinker.stop();
        }

        if (drawerIndicatorBlinker != null)
            drawerIndicatorBlinker.stop();
    }

    public void signingIn(boolean signingIn) {
        this.signingIn = signingIn;
    }

    IUpdateCheckService updateCheckService;
    UpdateServiceConnection updateServiceConnection;

    /**
     * @see <a href="https://cafebazaar.ir/developers/docs/bazaar-services/update-check/?l=fa">Bazaar documentation</a>
     */
    class UpdateServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder boundService) {
            updateCheckService = IUpdateCheckService.Stub.asInterface(boundService);
            try {
                long vCode = updateCheckService.getVersionCode(getPackageName());
                if (vCode > getPackageManager().getPackageInfo(getPackageName(), 0).versionCode) {
                    Snackbar
                            .make(findViewById(R.id.root_fragment), R.string.update_available, Snackbar.LENGTH_LONG)
                            .setAction(R.string.update, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    NetworkHelper.launchAppStoreUpdate(RootActivity.this);
                                }
                            }).setActionTextColor(getResources().getColor(R.color.primary_light))
                            .show();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            releaseUpdateCheckService();
        }

        public void onServiceDisconnected(ComponentName name) {
            updateCheckService = null;
            LogHelper.e(TAG, "UpdateServiceConnection disconnected");
        }
    }

    private void initUpdateCheckService() {
        updateServiceConnection = new UpdateServiceConnection();
        Intent i = new Intent("com.farsitel.bazaar.service.UpdateCheckService.BIND").setPackage("com.farsitel.bazaar");
        if (!bindService(i, updateServiceConnection, Context.BIND_AUTO_CREATE))
            updateServiceConnection = null;
    }

    /**
     * Un-binds this activity from Bazaar service.
     */
    private void releaseUpdateCheckService() {
        if (updateServiceConnection != null)
            unbindService(updateServiceConnection);
        updateServiceConnection = null;
    }

    public boolean newIssuesAvailableWarningShown;

}
