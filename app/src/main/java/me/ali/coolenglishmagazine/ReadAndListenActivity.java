package me.ali.coolenglishmagazine;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


public class ReadAndListenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_and_listen);

        // TODO read http://javarticles.com/2015/09/android-toolbar-example.html to add toolbar

        WebView webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///mnt/sdcard/cool-english-magazine/swimming-squirrel.html");

        Toast.makeText(this, Environment.getExternalStorageDirectory().toString(), Toast.LENGTH_LONG).show();
    }
}
