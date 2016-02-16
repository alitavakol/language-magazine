package me.ali.coolenglishmagazine;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.mikepenz.materialdrawer.DrawerBuilder;

import java.io.File;

import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.LogHelper;

public class RootActivity extends AppCompatActivity implements GalleryOfIssuesFragment.OnFragmentInteractionListener {

    private static final String TAG = LogHelper.makeLogTag(RootActivity.class);

    protected Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.root_fragment, GalleryOfIssuesFragment.newInstance(0), GalleryOfIssuesFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void onToolbarCreated(Toolbar toolbar) {
        setSupportActionBar(toolbar);
        new DrawerBuilder().withActivity(this).withToolbar(toolbar).build();
    }

    public void onIssueSelected(Magazines.Issue issue) {
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
