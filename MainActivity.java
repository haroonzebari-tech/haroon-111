package com.harontv;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends Activity {
    private static final String M3U_URL = "https://m3umaker.com/view/1550/haron1971";
    private static final String PREFS = "harontv";
    private static final String FAVS = "favorites";

    private PlayerView playerView;
    private ExoPlayer player;
    private ArrayAdapter<String> adapter;
    private TextView status;
    private EditText search;
    private Spinner groupSpinner;
    private final ArrayList<String> names = new ArrayList<>(), urls = new ArrayList<>();
    private final ArrayList<String> allNames = new ArrayList<>(), allUrls = new ArrayList<>(), allGroups = new ArrayList<>();
    private final ArrayList<String> shownGroups = new ArrayList<>();
    private boolean favoritesOnly = false;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle b) {
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        playerView = findViewById(R.id.player);
        status = findViewById(R.id.status);
        search = findViewById(R.id.search);
        groupSpinner = findViewById(R.id.groupSpinner);
        ListView list = findViewById(R.id.channelList);
        Button refresh = findViewById(R.id.refresh);
        Button favorites = findViewById(R.id.favorites);
        Button fullscreen = findViewById(R.id.fullscreen);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names) {
            @Override public View getView(int p, View c, ViewGroup parent) {
                TextView v = (TextView) super.getView(p, c, parent);
                v.setTextColor(Color.WHITE); v.setTextSize(16); v.setPadding(22, 15, 18, 15); v.setBackgroundColor(Color.rgb(18,25,34));
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((a,v,pos,id) -> play(urls.get(pos), names.get(pos)));
        list.setOnItemLongClickListener((a,v,pos,id) -> { toggleFavorite(urls.get(pos)); return true; });

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { applyFilters(); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        groupSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { applyFilters(); }
        });
        refresh.setOnClickListener(v -> loadM3U());
        favorites.setOnClickListener(v -> { favoritesOnly = !favoritesOnly; favorites.setText(favoritesOnly ? "كل القنوات" : "المفضلة ★"); applyFilters(); });
        fullscreen.setOnClickListener(v -> toggleFullscreen());
        loadM3U();
    }

    private void loadM3U() {
        status.setText("جاري تحديث القنوات...");
        Executors.newSingleThreadExecutor().execute(() -> {
            final String data = download(M3U_URL);
            runOnUiThread(() -> parseAndShow(data));
        });
    }

    private String download(String address) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(address).openConnection();
            c.setConnectTimeout(15000); c.setReadTimeout(25000);
            c.setRequestProperty("User-Agent", "HaronTV/1.0");
            c.setRequestProperty("Accept", "application/vnd.apple.mpegurl,application/x-mpegURL,text/plain,*/*");
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder s = new StringBuilder(); String line;
            while ((line = r.readLine()) != null) s.append(line).append('\n');
            r.close(); return s.toString();
        } catch (Exception e) { return null; }
        finally { if (c != null) c.disconnect(); }
    }

    private void parseAndShow(String data) {
        allNames.clear(); allUrls.clear(); allGroups.clear();
        if (data == null || data.trim().isEmpty()) {
            status.setText("تعذر تحميل قائمة القنوات. تحقق من الإنترنت والرابط."); adapter.notifyDataSetChanged(); return;
        }
        String current = "قناة", group = "عام";
        for (String raw : data.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.startsWith("#EXTINF")) {
                int k = line.indexOf(',');
                current = k >= 0 ? line.substring(k + 1).trim() : "قناة";
                if (current.isEmpty()) current = "قناة";
                int g = line.indexOf("group-title=\"");
                if (g >= 0) { int start = g + 13, end = line.indexOf('"', start); if (end > start) group = line.substring(start, end); }
            } else if (line.startsWith("#EXTGRP:")) {
                group = line.substring(8).trim();
            } else if (!line.startsWith("#") && (line.startsWith("http://") || line.startsWith("https://"))) {
                allNames.add(current); allUrls.add(line); allGroups.add(group.isEmpty() ? "عام" : group);
            }
        }
        rebuildGroups(); applyFilters();
        status.setText("تم تحميل " + allNames.size() + " قناة");
    }

    private void rebuildGroups() {
        shownGroups.clear(); shownGroups.add("كل الأقسام");
        LinkedHashSet<String> set = new LinkedHashSet<>(allGroups); shownGroups.addAll(set);
        ArrayAdapter<String> ga = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, shownGroups);
        groupSpinner.setAdapter(ga);
    }

    private void applyFilters() {
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        String group = groupSpinner.getSelectedItem() == null ? "كل الأقسام" : groupSpinner.getSelectedItem().toString();
        names.clear(); urls.clear();
        for (int i = 0; i < allNames.size(); i++) {
            boolean text = query.isEmpty() || allNames.get(i).toLowerCase(Locale.ROOT).contains(query);
            boolean grp = group.equals("كل الأقسام") || allGroups.get(i).equals(group);
            boolean fav = !favoritesOnly || isFavorite(allUrls.get(i));
            if (text && grp && fav) { names.add(allNames.get(i)); urls.add(allUrls.get(i)); }
        }
        adapter.notifyDataSetChanged();
        status.setText((favoritesOnly ? "المفضلة: " : "القنوات: ") + names.size());
    }

    private boolean isFavorite(String url) { return prefs.getStringSet(FAVS, new HashSet<>()).contains(url); }
    private void toggleFavorite(String url) {
        HashSet<String> set = new HashSet<>(prefs.getStringSet(FAVS, new HashSet<>()));
        if (set.contains(url)) set.remove(url); else set.add(url);
        prefs.edit().putStringSet(FAVS, set).apply();
        Toast.makeText(this, set.contains(url) ? "تمت الإضافة إلى المفضلة ★" : "تمت الإزالة من المفضلة", Toast.LENGTH_SHORT).show();
        applyFilters();
    }

    private void play(String url, String name) {
        status.setText("▶ " + name);
        player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.play();
    }

    private void toggleFullscreen() {
        int flags = getWindow().getDecorView().getSystemUiVisibility();
        boolean full = (flags & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
        getWindow().getDecorView().setSystemUiVisibility(full ? 0 : View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        playerView.getLayoutParams().height = full ? (int)(220 * getResources().getDisplayMetrics().density) : -1;
        playerView.requestLayout();
    }

    @Override protected void onDestroy() { if (player != null) player.release(); super.onDestroy(); }
}
