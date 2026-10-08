package br.com.canticos.outubromissionario;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;

public class MainActivity extends Activity {
    private static final int PICK_PDF = 4012;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PDFBoxResourceLoader.init(getApplicationContext());

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setTextZoom(100);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void pickPdf() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/pdf");
                startActivityForResult(intent, PICK_PDF);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) { }
            importPdf(uri);
        }
    }

    private String displayName(Uri uri) {
        String name = "PDF importado";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                int i = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0 && cursor.moveToFirst()) name = cursor.getString(i);
            } finally {
                cursor.close();
            }
        }
        return name;
    }

    private void importPdf(Uri uri) {
        Toast.makeText(this, "Importando PDF...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri); PDDocument doc = PDDocument.load(in)) {
                PDFTextStripper stripper = new PDFTextStripper();
                JSONArray pages = new JSONArray();
                for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                    stripper.setStartPage(p);
                    stripper.setEndPage(p);
                    String text = stripper.getText(doc);
                    pages.put(text == null ? "" : text.trim());
                }
                JSONObject payload = new JSONObject();
                payload.put("name", displayName(uri));
                payload.put("pages", pages);
                final String js = "window.onPdfImported(" + JSONObject.quote(payload.toString()) + ");";
                runOnUiThread(() -> webView.evaluateJavascript(js, null));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Não foi possível ler este PDF: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
