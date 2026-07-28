package za.co.thrive11.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 4107;
    private static final String START_URL = "file:///android_asset/index.html";

    private WebView webView;
    private ValueCallback<Uri[]> pendingFileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(15, 23, 42));
        getWindow().setNavigationBarColor(Color.rgb(15, 23, 42));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(248, 250, 252));
        setContentView(webView);

        configureWebView();
        if (savedInstanceState == null) {
            webView.loadUrl(START_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setTextZoom(100);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        CookieManager.getInstance().setAcceptCookie(true);
        webView.setWebViewClient(new ThriveWebViewClient());
        webView.setWebChromeClient(new ThriveWebChromeClient());
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> openExternal(url));
    }

    private final class ThriveWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleNavigation(request.getUrl());
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleNavigation(Uri.parse(url));
        }
    }

    private boolean handleNavigation(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null || "file".equalsIgnoreCase(scheme) || "about".equalsIgnoreCase(scheme)) {
            return false;
        }
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) ||
                "mailto".equalsIgnoreCase(scheme) || "tel".equalsIgnoreCase(scheme)) {
            openExternal(uri.toString());
            return true;
        }
        return false;
    }

    private final class ThriveWebChromeClient extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            if (pendingFileCallback != null) {
                pendingFileCallback.onReceiveValue(null);
            }
            pendingFileCallback = filePathCallback;

            Intent intent;
            try {
                intent = fileChooserParams.createIntent();
            } catch (Exception ignored) {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
            }

            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "image/*", "application/pdf", "video/*",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            });
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);

            try {
                startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_file)), FILE_CHOOSER_REQUEST);
                return true;
            } catch (ActivityNotFoundException exception) {
                pendingFileCallback = null;
                Toast.makeText(MainActivity.this, R.string.no_file_picker, Toast.LENGTH_LONG).show();
                return false;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || pendingFileCallback == null) {
            return;
        }

        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) {
                    results[i] = data.getClipData().getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
        }

        pendingFileCallback.onReceiveValue(results);
        pendingFileCallback = null;
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            webView.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
