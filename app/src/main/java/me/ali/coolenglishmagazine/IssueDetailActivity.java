package me.ali.coolenglishmagazine;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.farsitel.bazaar.ILoginCheckService;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import me.ali.coolenglishmagazine.broadcast_receivers.DownloadCompleteBroadcastReceiver;
import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.IabHelper;
import me.ali.coolenglishmagazine.util.IabResult;
import me.ali.coolenglishmagazine.util.Inventory;
import me.ali.coolenglishmagazine.util.LogHelper;
import me.ali.coolenglishmagazine.util.NetworkHelper;
import me.ali.coolenglishmagazine.util.Purchase;
import me.ali.coolenglishmagazine.util.SkuDetails;
import me.ali.coolenglishmagazine.widget.ObservableScrollView;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * An activity representing a single Issue detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link RootActivity}.
 */
public class IssueDetailActivity extends AppCompatActivity implements ObservableScrollView.Callbacks, Magazines.Issue.OnStatusChangedListener {

    private static final String TAG = LogHelper.makeLogTag(IssueDetailActivity.class);

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ROOT_DIRECTORY = "issue_root_directory";

    Magazines.Issue issue;

    Button buttonDownload, buttonPurchase, buttonOpen;
    ImageButton buttonCancel; //, buttonComplete, buttonDelete, buttonIncomplete;
    ProgressBar progressBar;
    ViewGroup progressContainer, buttonContainer;
    private ObservableScrollView mScrollView;
    private View mPhotoViewContainer;
    private LinearLayout mHeaderSession;
    private FrameLayout issueDetailsContainer;
    TextView priceTextView;
    TextView tapToRefreshButton;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_issue_detail);
        toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        downloadManager = (DownloadManager) IssueDetailActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);

        buttonCancel = (ImageButton) findViewById(R.id.buttonCancel);
        buttonDownload = (Button) findViewById(R.id.buttonDownload);
        buttonPurchase = (Button) findViewById(R.id.buttonPurchase);
        buttonOpen = (Button) findViewById(R.id.buttonOpen);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        buttonContainer = (ViewGroup) findViewById(R.id.button_container);
        progressContainer = (ViewGroup) findViewById(R.id.progress_container);

        try {
            issue = Magazines.getIssue(this, new File(getIntent().getStringExtra(ARG_ROOT_DIRECTORY)));

            issue.addOnStatusChangedListener(this);
            downloadReference = Magazines.getDownloadReference(this, issue);

            ((TextView) findViewById(R.id.session_title)).setText(issue.description);
            ((TextView) findViewById(R.id.toolbar_title)).setText(issue.subtitle);
            priceTextView = (TextView) findViewById(R.id.session_price);

            buttonOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish(); // remove this activity from back stack

                    Intent intent = new Intent(IssueDetailActivity.this, ItemListActivity.class);
                    intent.putExtra(ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());
                    startActivity(intent);
                }
            });

            buttonDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!issue.purchased) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(IssueDetailActivity.this, R.style.AppTheme));
                        builder.setMessage(R.string.free_download_warning_single)
                                .setTitle(R.string.free_download_warning_title)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startDownload();
                                    }
                                })
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .setCancelable(true)
                                .show();

                    } else {
                        startDownload();
                    }
                }

                protected void startDownload() {
                    try {
                        downloadReference = Magazines.download(IssueDetailActivity.this, issue);
                        updateFab();
                    } catch (IOException e) {
                        LogHelper.e(TAG, e.getMessage());
                    }
                }
            });

            buttonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    downloadManager.remove(downloadReference);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            downloadManager.remove(downloadReference); // http://stackoverflow.com/a/34797980
                        }
                    }, 1000);
                    issue.setStatus(Magazines.Issue.Status.available);
                    updateFab();
                }
            });
            buttonCancel.setImageDrawable(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_clear).sizeRes(R.dimen.tw__login_btn_text_size).colorRes(R.color.accent));

            // Show the Up button in the action bar.
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(false); // hide action bar title
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeAsUpIndicator(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_arrow_back).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_appbar));
            }

            mScrollView = (ObservableScrollView) findViewById(R.id.scroll_view);
            mPhotoViewContainer = findViewById(R.id.session_photo_container);
            mHeaderSession = (LinearLayout) findViewById(R.id.header_session);
            issueDetailsContainer = (FrameLayout) findViewById(R.id.issue_detail_container);

            mScrollView.addCallbacks(this);
            setOnScrollViewLayoutChangedListener();

            // savedInstanceState is non-null when there is fragment state
            // saved from previous configurations of this activity
            // (e.g. when rotating the screen from portrait to landscape).
            // In this case, the fragment will automatically be re-added
            // to its container so we don't need to manually add it.
            // For more information, see the Fragments API guide at:
            //
            // http://developer.android.com/guide/components/fragments.html
            //
            if (savedInstanceState == null) {
                // Create the detail fragment and add it to the activity
                // using a fragment transaction.
                Bundle arguments = new Bundle();

                final String issueRootDirectory = getIntent().getStringExtra(ARG_ROOT_DIRECTORY);
                arguments.putString(ARG_ROOT_DIRECTORY, issueRootDirectory);

                IssueDetailFragment fragment = new IssueDetailFragment();
                fragment.setArguments(arguments);

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.issue_detail_container, fragment)
                        .commit();
            }

            tapToRefreshButton = (TextView) findViewById(R.id.tap_to_refresh);

            if (iabHelper == null) {
                // the Base64-encoded RSA public key of the application
                final String base64EncodedPublicKey = IabHelper.getPublicKey();

                // compute your public key and store it in base64EncodedPublicKey
                iabHelper = new IabHelper(getApplicationContext(), base64EncodedPublicKey);

                iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                    public void onIabSetupFinished(IabResult result) {
                        if (!result.isSuccess()) {
                            // Oh noes, there was a problem.
                            LogHelper.d(TAG, "Problem setting up In-app Billing: " + result);

                            iabHelper.dispose();
                            iabHelper = null;
                        }
                        // Hooray, IAB is fully set up!

                        tapToRefreshButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (isLoggedIn) {
                                    if (NetworkHelper.isOnline(IssueDetailActivity.this))
                                        updatePrice();
                                    else
                                        Toast.makeText(IssueDetailActivity.this, R.string.check_connection, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(IssueDetailActivity.this, R.string.login_required, Toast.LENGTH_LONG).show();
                                }
                            }
                        });

                        buttonPurchase.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                buttonPurchase.setEnabled(false);
                                buttonPurchase.setClickable(false);
                                buttonPurchase.setTextColor(getResources().getColor(R.color.linkColorDisabled));
                                tapToRefreshButton.setClickable(false);
                                tapToRefreshButton.setEnabled(false);
                                tapToRefreshButton.setText(R.string.tap_to_refresh_disabled);
                                tapToRefreshButton.setTextColor(getResources().getColor(R.color.linkColorDisabled));
                                iabHelper.flagEndAsync();
                                iabHelper.launchPurchaseFlow(IssueDetailActivity.this, Magazines.getSku(issue), issue.id, iabPurchaseFinishedListener, "developer-payload");
                            }
                        });
                    }
                });
            }

            updatePriceGui();

        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (issue != null)
            issue.removeOnStatusChangedListener(this);

        if (iabHelper != null) {
            iabHelper.dispose();
            iabHelper = null;
        }
    }

    public void setOnScrollViewLayoutChangedListener() {
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
        }
    }

    Toolbar toolbar;

    MenuItem deleteMenuItem, completeMenuItem, incompleteMenuItem;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.issue_details_activity_menu, menu);
        getMenuInflater().inflate(R.menu.common, menu);

        completeMenuItem = menu.findItem(R.id.action_mark_complete);
        incompleteMenuItem = menu.findItem(R.id.action_mark_incomplete);
        deleteMenuItem = menu.findItem(R.id.action_delete);

        // update visibility of complete and incomplete buttons
        onIssueStatusChanged(issue);
        updateFab();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();

                Intent intent = new Intent(this, RootActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                startActivity(intent);
                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme));
                builder.setMessage(R.string.issue_delete_confirmation_message)
                        .setTitle(R.string.issue_delete_confirmation_title)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Magazines.deleteIssue(IssueDetailActivity.this, issue, false);
                                updateFab();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setCancelable(true)
                        .show();
                break;

            case R.id.action_mark_complete:
                Magazines.markCompleted(issue);
                break;

            case R.id.action_mark_incomplete:
                Magazines.reopen(this, issue);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private long downloadReference;
    private BroadcastReceiver receiverDownloadExtracted;

    @Override
    protected void onResume() {
        super.onResume();

        receiverDownloadExtracted = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateFab();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverDownloadExtracted, new IntentFilter(DownloadCompleteBroadcastReceiver.ACTION_DOWNLOAD_EXTRACTED));

        updateFab();

        // check if user is logged in to their bazaar account
        loginCheckServiceConnection = new LoginCheckServiceConnection();
        Intent i = new Intent("com.farsitel.bazaar.service.LoginCheckService.BIND");
        i.setPackage("com.farsitel.bazaar");
        boolean ret = bindService(i, loginCheckServiceConnection, Context.BIND_AUTO_CREATE);
        LogHelper.i(TAG, "initService() bound value: " + ret);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (timer != null) {
            timer.cancel();
            timer = null;
            LogHelper.i(TAG, "timer cancelled.");
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverDownloadExtracted);

        if (loginCheckServiceConnection != null) {
            unbindService(loginCheckServiceConnection);
            loginCheckServiceConnection = null;
        }
    }

    DownloadManager downloadManager;

    /**
     * progress bar update timer
     */
    Timer timer;

    /**
     * progress bar value
     */
    int dl_progress;

    /**
     * updates visibility of buttons and progress bar, depending on current issue download status
     */
    void updateFab() {
        final int status = Magazines.getDownloadStatus(this, issue);

        switch (status) {
            case DownloadManager.STATUS_PENDING:
            case DownloadManager.STATUS_PAUSED:
                buttonContainer.setVisibility(View.INVISIBLE);
                progressContainer.setVisibility(View.VISIBLE);
                buttonCancel.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
                break;

            case DownloadManager.STATUS_RUNNING:
                buttonContainer.setVisibility(View.INVISIBLE);
                progressContainer.setVisibility(View.VISIBLE);
                buttonCancel.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(false);
                progressBar.setProgress(dl_progress);
                break;

            case -3: // the issue is being extracted
                buttonContainer.setVisibility(View.INVISIBLE);
                progressContainer.setVisibility(View.VISIBLE);
                buttonCancel.setVisibility(View.GONE);
                progressBar.setIndeterminate(true);
                break;

            default:
                buttonContainer.setVisibility(View.VISIBLE);
                progressContainer.setVisibility(View.INVISIBLE);

                final boolean purchasedButNotDownloaded = issue.purchased && !new File(issue.rootDirectory, Magazines.Issue.proFileName).exists();

                if (new File(issue.rootDirectory, Magazines.Issue.downloadedFileName).exists()) {
                    buttonDownload.setVisibility(purchasedButNotDownloaded ? View.VISIBLE : View.GONE);
                    buttonOpen.setVisibility(View.VISIBLE);
                    if (deleteMenuItem != null)
                        deleteMenuItem.setVisible(true);

                } else {
                    buttonDownload.setContentDescription(getString(status == DownloadManager.STATUS_FAILED ? R.string.retry_download : R.string.download));
                    buttonDownload.setVisibility(View.VISIBLE);
                    buttonOpen.setVisibility(View.GONE);
                    if (deleteMenuItem != null)
                        deleteMenuItem.setVisible(false);
                }

                if (purchasedButNotDownloaded)
                    buttonDownload.setText(R.string.download_full_text);

                if (timer != null) {
                    timer.cancel();
                    timer = null;
                    LogHelper.i(TAG, "timer cancelled.");
                }
                return;
        }

        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    dl_progress = Magazines.getDownloadProgress(IssueDetailActivity.this, issue);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateFab();
                            LogHelper.i(TAG, "download progress: " + dl_progress);
                        }
                    });
                }
            }, 0, 1000);
            LogHelper.i(TAG, "timer created.");
        }
    }

    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        int scrollY = mScrollView.getScrollY();
        mPhotoViewContainer.setTranslationY(scrollY * 0.5f);
        mHeaderSession.setTranslationY(Math.max(headerTranslation, scrollY));
//        mHeaderSession.setElevation(scrollY > headerTranslation ? 10 : 0);
    }

    int headerTranslation;

    /**
     * adjust top margin and translationY of views contained in the FrameLayout, which depends on measured height of them
     */
    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // render cover image with its real aspect ratio; but if its height exceeds 5/7 of available height, centerCrop it.
            File coverImageFile = new File(issue.rootDirectory, Magazines.Issue.posterFileName);
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(coverImageFile.getAbsolutePath(), options);
            float aspectRatio = (float) options.outHeight / (float) options.outWidth;

            final ImageView coverImageView = (ImageView) findViewById(R.id.cover);
            int targetWidth = mScrollView.getMeasuredWidth();
            int targetHeight = Math.min((int) (targetWidth * aspectRatio), 5 * mScrollView.getMeasuredHeight() / 7);

            Picasso
                    .with(IssueDetailActivity.this)
                    .load(coverImageFile)
                    .resize(targetWidth, targetHeight)
                    .centerCrop()
                    .into(coverImageView);

            headerTranslation = targetHeight;
            int headerHeight = mHeaderSession.getHeight();

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) issueDetailsContainer.getLayoutParams();
            lp.bottomMargin = headerTranslation + headerHeight;
            issueDetailsContainer.setLayoutParams(lp);

            issueDetailsContainer.setTranslationY(headerTranslation + headerHeight);

            mScrollView.fullScroll(View.FOCUS_UP);
            onScrollChanged(0, 0);

            mScrollView.getViewTreeObserver().removeGlobalOnLayoutListener(mGlobalLayoutListener);
        }
    };

    public void onIssueStatusChanged(Magazines.Issue issue) {
        final Magazines.Issue.Status status = issue.getStatus();

        if (status == Magazines.Issue.Status.downloading || status == Magazines.Issue.Status.available) {
            completeMenuItem.setVisible(false);
            incompleteMenuItem.setVisible(false);

        } else {
            incompleteMenuItem.setVisible(status == Magazines.Issue.Status.completed);
            completeMenuItem.setVisible(status != Magazines.Issue.Status.completed);
        }
    }

    protected IabHelper iabHelper;
//    protected boolean queryingPurchases;

    private IabHelper.QueryInventoryFinishedListener iabQueryProductDetailsFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (result.isFailure()) {
                // handle error
                Toast.makeText(IssueDetailActivity.this, R.string.iab_query_error, Toast.LENGTH_SHORT).show();

            } else {
                final String sku = Magazines.getSku(issue);

//                if (!queryingPurchases) {
                    final SkuDetails details = inventory.getSkuDetails(sku);
                    final boolean purchased = details == null || inventory.hasPurchase(sku);
                    final String price = details != null ? details.getPrice() : getString(R.string.free);

//                    if (!purchased) {
//                        // get purchases from inventory
//                        queryingPurchases = true;
//                        ArrayList<String> skuList = new ArrayList<>();
//                        skuList.add(Magazines.getSku(issue));
//                        iabHelper.queryInventoryAsync(false, skuList, iabQueryProductDetailsFinishedListener);
//                    }

                    updateBillingInfo(price, purchased);
//                    return;

//                } else {
//                    updateBillingInfo(issue.price, inventory.hasPurchase(sku));
//                }

//                Toast.makeText(IssueDetailActivity.this, R.string.sync_complete, Toast.LENGTH_SHORT).show();
            }

            tapToRefreshButton.setText(R.string.tap_to_refresh);
            tapToRefreshButton.setClickable(true);
            tapToRefreshButton.setEnabled(true);
            tapToRefreshButton.setText(R.string.tap_to_refresh);
            tapToRefreshButton.setTextColor(getResources().getColor(R.color.linkColor));
//            buttonPurchase.setEnabled(true);
//            buttonPurchase.setClickable(true);
//            buttonPurchase.setTextColor(getResources().getColor(R.color.primary_light));
        }
    };

    /**
     * saves billing information into issue's manifest file
     *
     * @param price     new product price
     * @param purchased new value for {@link me.ali.coolenglishmagazine.model.Magazines.Issue#purchased}
     */
    protected void updateBillingInfo(String price, boolean purchased) {
        if (!price.equals(issue.price) || purchased != issue.purchased) {
            issue.price = price;
            issue.purchased = purchased;

            // save price and purchase status in issue's manifest file
            try {
                File input = new File(issue.rootDirectory, Magazines.Issue.manifestFileName);
                final Document doc = Jsoup.parse(input, "UTF-8", "");

                Element e = doc.getElementsByTag("issue").first();
                e.attr("price", price);
                e.attr("purchased", purchased ? "true" : "false");

                FileOutputStream output = new FileOutputStream(input);
                output.write(doc.body().html().getBytes());
                output.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            updatePriceGui();
            updateFab();
        }
    }

    private IabHelper.OnIabPurchaseFinishedListener iabPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()) {
                LogHelper.d(TAG, "Error purchasing: " + result);

            } else if (purchase.getSku().equals(Magazines.getSku(issue))) {
                // update the UI
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == issue.id && resultCode == RESULT_OK) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            try {
                JSONObject jo = new JSONObject(purchaseData);
                String sku = jo.getString("productId");
                int purchaseState = jo.getInt("purchaseState");

                // https://cafebazaar.ir/developers/docs/iab/reference/
                if (sku.equals(Magazines.getSku(issue)) && purchaseState == 0) {
                    updateBillingInfo(issue.price, true);
                }

            } catch (JSONException e) {
                LogHelper.e(TAG, "Failed to parse purchase data.");
                e.printStackTrace();
            }
        }

        buttonPurchase.setEnabled(true);
        buttonPurchase.setClickable(true);
        buttonPurchase.setTextColor(getResources().getColor(R.color.primary_light));
        tapToRefreshButton.setClickable(true);
        tapToRefreshButton.setEnabled(true);
        tapToRefreshButton.setText(R.string.tap_to_refresh);
        tapToRefreshButton.setTextColor(getResources().getColor(R.color.linkColor));
    }

    protected void updatePrice() {
        if (iabHelper == null)
            return;

        tapToRefreshButton.setClickable(false);
        tapToRefreshButton.setEnabled(false);
        tapToRefreshButton.setText(R.string.refreshing);
        tapToRefreshButton.setTextColor(getResources().getColor(R.color.linkColorDisabled));
//        buttonPurchase.setEnabled(false);
//        buttonPurchase.setClickable(false);
//        buttonPurchase.setTextColor(getResources().getColor(R.color.linkColorDisabled));

        // get price and purchase state from inventory
//        queryingPurchases = false;
        ArrayList<String> skuList = new ArrayList<>();
        skuList.add(Magazines.getSku(issue));
        iabHelper.flagEndAsync();
        iabHelper.queryInventoryAsync(true, skuList, iabQueryProductDetailsFinishedListener);
    }

    /**
     * updates GUI to reflect product price and purchased information
     */
    protected void updatePriceGui() {
        priceTextView.setText(issue.price.length() > 0 ? issue.price : getString(R.string.unknown_price));

//        tapToRefreshButton.setVisibility(iabHelper == null ? View.GONE : View.VISIBLE);
//        if (iabHelper == null) {
//            tapToRefreshButton.setClickable(false);
//            tapToRefreshButton.setEnabled(false);
//            tapToRefreshButton.setText(R.string.tap_to_refresh_disabled);
//            tapToRefreshButton.setTextColor(getResources().getColor(R.color.linkColorDisabled));
//        }

        buttonPurchase.setVisibility(issue.purchased ? View.GONE : View.VISIBLE);
//        buttonPurchase.setClickable(iabHelper != null);
//        buttonPurchase.setEnabled(iabHelper != null);
//        buttonPurchase.setTextColor(getResources().getColor(iabHelper != null ? R.color.primary_light : R.color.linkColorDisabled));
    }

    private ILoginCheckService loginCheckService;

    private LoginCheckServiceConnection loginCheckServiceConnection;

    /**
     * shows whether or not user is logged in to their bazaar account
     */
    boolean isLoggedIn;

    public class LoginCheckServiceConnection implements ServiceConnection {
        private final String TAG = LogHelper.makeLogTag(LoginCheckServiceConnection.class);

        public void onServiceConnected(ComponentName name, IBinder boundService) {
            loginCheckService = ILoginCheckService.Stub.asInterface(boundService);

            try {
                isLoggedIn = loginCheckService.isLoggedIn();

                unbindService(loginCheckServiceConnection);
                loginCheckServiceConnection = null;

                // fetch price information only if item is not purchased and price is unknown
                if (!issue.purchased && issue.price.length() == 0) {
                    if (isLoggedIn && NetworkHelper.isOnline(IssueDetailActivity.this)) {
                        updatePrice();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            LogHelper.i(TAG, "login check service connected");
        }

        public void onServiceDisconnected(ComponentName name) {
            loginCheckService = null;
            LogHelper.i(TAG, "login check service disconnected");
        }
    }
}
