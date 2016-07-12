package me.ali.coolenglishmagazine;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.IBinder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
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
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.Locale;

import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.Account;
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
     * intent action that activates cool english times fragment
     */
    public static final String ACTION_SHOW_TIMES = "me.ali.coolenglishmagazine.ACTION_SHOW_TIMES";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean mTwoPane;

    Fragment galleryOfIssuesFragment, coolEnglishTimesFragment, aboutFragment, readmeFragment;

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
            drawer_selection = savedInstanceState.getInt("drawer_selection");
            signingIn = savedInstanceState.getBoolean("signing_in");

        } else {
            // show the gallery of issues fragment by default
            galleryOfIssuesFragment = GalleryOfIssuesFragment.newInstance(ACTION_SHOW_DOWNLOADS.equals(getIntent().getAction()) ? 1 : 0);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.root_fragment, galleryOfIssuesFragment, GalleryOfIssuesFragment.FRAGMENT_TAG)
                    .commit();
        }

        setupNavigationDrawer();

        // show navigation drawer on first start
        if (!preferences.getBoolean("drawer_welcome_shown", false)) {
            drawer.openDrawer();
            preferences.edit().putBoolean("drawer_welcome_shown", true).apply();
        }

        account = new Account(this);

        if (!processWasRunning) { // check for updates if app's process has just started
            initUpdateCheckService();
            processWasRunning = false;
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
        }
    };

    ImageView profilePicture;
    TextView userName, userEmail;
    CircularProgressView circularProgressView;

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
                if (!hasSavedLogIn)
                    account.signIn();
            }
        });
        headerView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!hasSavedLogIn)
                    return false;

                AlertDialog.Builder builder = new AlertDialog.Builder(RootActivity.this);
                builder.setMessage(R.string.sign_out_warning)
                        .setTitle(R.string.sign_out_warning_title)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                account.signOut();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setCancelable(true)
                        .show();
                return true;
            }
        });

        final PrimaryDrawerItem galleryOfIssues = new PrimaryDrawerItem().withName(R.string.gallery_of_issues).withIcon(GoogleMaterial.Icon.gmd_playlist_play).withSelectedColorRes(R.color.primary);
        final PrimaryDrawerItem englishTimes = new PrimaryDrawerItem().withName(R.string.cool_english_times).withIcon(GoogleMaterial.Icon.gmd_alarm).withSelectedColorRes(R.color.primary);
        final PrimaryDrawerItem readme = new PrimaryDrawerItem().withName(R.string.readme).withIcon(GoogleMaterial.Icon.gmd_sentiment_satisfied).withSelectedColorRes(R.color.primary);
        final PrimaryDrawerItem about = new PrimaryDrawerItem().withName(R.string.about).withIcon(GoogleMaterial.Icon.gmd_info_outline).withSelectedColorRes(R.color.primary);

        drawer = new DrawerBuilder()
                .withSliderBackgroundColorRes(R.color.accent)
                .withHeaderDivider(true)
                .withActivity(this)
                .withHeader(headerView)
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
                        if (drawer_selection == position)
                            return false;

                        Fragment fragment = null;
                        String tag = null;
                        switch (position) {
                            case 1:
                                if (galleryOfIssuesFragment == null)
                                    galleryOfIssuesFragment = GalleryOfIssuesFragment.newInstance(0);
                                fragment = galleryOfIssuesFragment;
                                tag = GalleryOfIssuesFragment.FRAGMENT_TAG;
                                break;
                            case 2:
                                if (coolEnglishTimesFragment == null)
                                    coolEnglishTimesFragment = CoolEnglishTimesFragment.newInstance(0);
                                fragment = coolEnglishTimesFragment;
                                tag = CoolEnglishTimesFragment.FRAGMENT_TAG;
                                break;
                            case 3:
                                if (readmeFragment == null)
                                    readmeFragment = ReadmeFragment.newInstance();
                                fragment = readmeFragment;
                                tag = ReadmeFragment.FRAGMENT_TAG;
                                break;
                            case 5:
                                if (aboutFragment == null)
                                    aboutFragment = AboutFragment.newInstance();
                                fragment = aboutFragment;
                                tag = AboutFragment.FRAGMENT_TAG;
                                break;
                        }

                        if (fragment != null) {
                            getSupportFragmentManager().beginTransaction()
//                            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                                    .replace(R.id.root_fragment, fragment, tag)
                                    .commit();
                        } else {
                            Toast.makeText(RootActivity.this, "item clicked: " + position, Toast.LENGTH_SHORT).show();
                        }

                        drawer_selection = position;
                        return false;
                    }
                })
                .withSelectedItemByPosition(drawer_selection)
                .build();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (drawer != null) {
            outState.putInt("drawer_selection", drawer.getCurrentSelectedPosition());
            outState.putBoolean("signing_in", signingIn);
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
    int drawer_selection = 1;

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

        } else if (drawer_selection != 1 && drawer != null) {
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

        if (signedOut) { // user explicitly signed out
            hasSavedLogIn = false;

        } else {
            if (userId != null) {
                hasSavedLogIn = true;

            } else {
                hasSavedLogIn = preferences.contains("user_id");
            }
        }

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
        super.onStart();
        account.silentSignIn();
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
            updateCheckService = IUpdateCheckService.Stub.asInterface((IBinder) boundService);
            try {
                long vCode = updateCheckService.getVersionCode(getPackageName());
                if (vCode > BuildConfig.VERSION_CODE) {
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
        bindService(i, updateServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Un-binds this activity from Bazaar service.
     */
    private void releaseUpdateCheckService() {
        if (updateServiceConnection != null)
            unbindService(updateServiceConnection);
        updateServiceConnection = null;
    }

    /**
     * determines whether app's process has just run from scratch.
     */
    public static boolean processWasRunning;
}
