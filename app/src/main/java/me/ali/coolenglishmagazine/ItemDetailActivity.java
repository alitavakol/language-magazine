package me.ali.coolenglishmagazine;

import android.app.NotificationManager;
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.view.MenuItem;
import android.view.View;

import java.io.File;

/**
 * An activity representing a single Item detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link ItemListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link ItemDetailFragment}.
 */
public class ItemDetailActivity extends AppCompatActivity {

    /**
     * The activity argument representing the item ID that this activity
     * represents.
     */
    public static final String ARG_ROOT_DIRECTORY = "item_root_directory";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ItemDetailActivity.this, ReadAndListenActivity.class);

                final String itemRootDirectory = getIntent().getStringExtra(ARG_ROOT_DIRECTORY);
                intent.putExtra(ARG_ROOT_DIRECTORY, itemRootDirectory);

                startActivity(intent);
                finish(); // remove this activity from back stack
            }
        });

        // Show the Up button in the action bar.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

            final String itemRootDirectory = getIntent().getStringExtra(ARG_ROOT_DIRECTORY);
            arguments.putString(ARG_ROOT_DIRECTORY, itemRootDirectory);

            ItemDetailFragment fragment = new ItemDetailFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.item_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();

            Intent intent = new Intent(this, ItemListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            final String issueRootDirectory = new File(getIntent().getStringExtra(ARG_ROOT_DIRECTORY)).getParent();
            intent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, issueRootDirectory);

            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
