package com.greedy.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class DynamicMarkov {
    private Map<String, int[]> transition2 = new HashMap<>();
    private int[][] transition1 = new int[8][8];
    private SharedPreferences prefs;

    public DynamicMarkov(Context context) {
        prefs = context.getSharedPreferences("greedy_bot", Context.MODE_PRIVATE);
        loadBaseModel(context);
    }

    private void loadBaseModel(Context context) {
        try {
            InputStream is = context.getAssets().open("shared_data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject root = new JSONObject(jsonStr);

            // transitionCounts1
            JSONArray tc1 = root.getJSONArray("transitionCounts1");
            for (int i = 0; i < tc1.length() && i < 8; i++) {
                JSONArray row = tc1.getJSONArray(i);
                for (int j = 0; j < row.length() && j < 8; j++) {
                    transition1[i][j] = row.getInt(j);
                }
            }

            // transitionCounts2
            JSONObject tc2 = root.getJSONObject("transitionCounts2");
            java.util.Iterator<String> keys = tc2.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String[] parts = key.split(",");
                if (parts.length == 2) {
                    int a = Integer.parseInt(parts[0]);
                    int b = Integer.parseInt(parts[1]);
                    JSONArray arr = tc2.getJSONArray(key);
                    int[] counts = new int[8];
                    for (int i = 0; i < arr.length() && i < 8; i++) counts[i] = arr.getInt(i);
                    transition2.put(a + "," + b, counts);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int predict(int prevPrev, int prev) {
        String key = prevPrev + "," + prev;
        int[] t2 = transition2.get(key);
        if (t2 != null && sum(t2) > 0) {
            return argMax(t2);
        }
        if (prev >= 0 && prev < 8) {
            return argMax(transition1[prev]);
        }
        return 0;
    }

    public void update(int prevPrev, int prev, int correct) {
        String key = prevPrev + "," + prev;
        int[] t2 = transition2.get(key);
        if (t2 == null) {
            t2 = new int[8];
            transition2.put(key, t2);
        }
        t2[correct]++;
        if (prev >= 0 && prev < 8) {
            transition1[prev][correct]++;
        }
        saveUpdates();
    }

    private int sum(int[] arr) {
        int s = 0;
        for (int v : arr) s += v;
        return s;
    }

    private int argMax(int[] arr) {
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) if (arr[i] > arr[maxIdx]) maxIdx = i;
        return maxIdx;
    }

    private void saveUpdates() {
        // يمكن حفظ transition2 هنا إلى SharedPreferences إذا أردت الاحتفاظ بالتحديثات
    }
}