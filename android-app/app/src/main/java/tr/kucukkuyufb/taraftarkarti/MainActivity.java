package tr.kucukkuyufb.taraftarkarti;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

public class MainActivity extends Activity {
    private static final String BASE = "https://kucukkuyufb.github.io/kucukkuyu-kart/";
    private static final String MEMBERS = BASE + "members.json";
    private static final String PREFS = "taraftar_karti";
    private static final String KEY_CODE = "card_code";
    private WebView webView;

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String code = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_CODE, "");
        if (code != null && !code.isEmpty()) {
            openCard(code);
        } else {
            showMemberLogin();
        }
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private void showMemberLogin() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(26), dp(40), dp(26), dp(30));
        root.setBackgroundColor(Color.rgb(7, 29, 73));

        ImageView logo = new ImageView(this);
        logo.setImageResource(tr.kucukkuyufb.taraftarkarti.R.drawable.app_icon);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(170), dp(170));
        lpLogo.bottomMargin = dp(18);
        root.addView(logo, lpLogo);

        TextView title = text("KÜÇÜKKUYU FENERBAHÇE\nGÖNÜLLÜLERİ", 21, Color.WHITE, true);
        LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(-1, -2);
        lpTitle.bottomMargin = dp(8);
        root.addView(title, lpTitle);

        TextView sub = text("DİJİTAL TARAFTAR KARTI", 13, Color.rgb(255,213,0), true);
        LinearLayout.LayoutParams lpSub = new LinearLayout.LayoutParams(-1, -2);
        lpSub.bottomMargin = dp(34);
        root.addView(sub, lpSub);

        EditText memberNo = new EditText(this);
        memberNo.setHint("Üye Numaranız (ör. 10006)");
        memberNo.setSingleLine(true);
        memberNo.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        memberNo.setTextSize(18);
        memberNo.setPadding(dp(16), 0, dp(16), 0);
        memberNo.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lpInput = new LinearLayout.LayoutParams(-1, dp(58));
        lpInput.bottomMargin = dp(14);
        root.addView(memberNo, lpInput);

        Button open = new Button(this);
        open.setText("KARTIMI AÇ");
        open.setTextSize(16);
        open.setTextColor(Color.rgb(7,29,73));
        open.setBackgroundColor(Color.rgb(255,213,0));
        LinearLayout.LayoutParams lpButton = new LinearLayout.LayoutParams(-1, dp(56));
        root.addView(open, lpButton);

        ProgressBar progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpProgress = new LinearLayout.LayoutParams(dp(44), dp(44));
        lpProgress.topMargin = dp(18);
        root.addView(progress, lpProgress);

        TextView note = text("Üye numaranızı bir kez girmeniz yeterli.\nSonraki açılışlarda kartınız doğrudan açılır.", 12, Color.rgb(220,229,246), false);
        LinearLayout.LayoutParams lpNote = new LinearLayout.LayoutParams(-1, -2);
        lpNote.topMargin = dp(18);
        root.addView(note, lpNote);

        open.setOnClickListener(v -> {
            String no = memberNo.getText().toString().trim();
            if (no.isEmpty()) {
                Toast.makeText(this, "Üye numaranızı girin.", Toast.LENGTH_SHORT).show();
                return;
            }
            open.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            findMember(no, open, progress);
        });

        setContentView(root);
    }

    private void findMember(String memberNo, Button open, ProgressBar progress) {
        new Thread(() -> {
            String found = null;
            String error = null;
            HttpURLConnection conn = null;
            try {
                URL u = new URL(MEMBERS + "?ts=" + System.currentTimeMillis());
                conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Cache-Control", "no-cache");
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject all = new JSONObject(sb.toString());
                Iterator<String> keys = all.keys();
                while (keys.hasNext()) {
                    String code = keys.next();
                    JSONObject m = all.optJSONObject(code);
                    if (m != null && memberNo.equals(m.optString("uyeNo").trim())) {
                        found = code;
                        break;
                    }
                }
                if (found == null) error = "Bu üye numarası bulunamadı.";
            } catch (Exception ex) {
                error = "Üyelik bilgileri alınamadı. İnternet bağlantınızı kontrol edin.";
            } finally {
                if (conn != null) conn.disconnect();
            }

            final String code = found;
            final String message = error;
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                open.setEnabled(true);
                if (code != null) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_CODE, code).apply();
                    openCard(code);
                } else {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void openCard(String code) {
        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("kucukkuyufb.github.io".equalsIgnoreCase(uri.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {}
                return true;
            }
        });

        webView.loadUrl(BASE + "?k=" + Uri.encode(code) + "&app=1");
        setContentView(webView);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
