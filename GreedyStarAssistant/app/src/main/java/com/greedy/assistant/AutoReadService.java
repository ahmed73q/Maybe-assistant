package com.greedy.assistant;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoReadService extends AccessibilityService {
    public static OnNewResultListener onNewResult = null;
    public static OnTimerChangeListener onTimerChange = null;
    public static OnRoundStartListener onRoundStart = null;

    public interface OnNewResultListener { void onNewResult(int winnerId); }
    public interface OnTimerChangeListener { void onTimerChange(int seconds); }
    public interface OnRoundStartListener { void onRoundStart(); }

    private int lastWinner = -1;
    private List<Integer> lastResults = new ArrayList<>();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        String allText = getAllText(root);

        // استخراج العداد
        Integer timer = extractTimer(allText);
        if (timer != null && onTimerChange != null) onTimerChange.onTimerChange(timer);

        // استخراج النتائج
        List<Integer> results = extractRecentResultsFromNodes(root);
        if (!results.isEmpty() && !results.equals(lastResults)) {
            lastResults = results;
            saveResults(results);
        }

        // استخراج الفائز
        Integer winner = extractWinner(allText);
        if (winner != null && winner != lastWinner) {
            lastWinner = winner;
            if (onNewResult != null) onNewResult.onNewResult(winner);
        }

        if (timer != null && timer == 29 && onRoundStart != null) onRoundStart.onRoundStart();
    }

    private String getAllText(AccessibilityNodeInfo node) {
        StringBuilder sb = new StringBuilder();
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if (text != null) sb.append(text).append(" ");
        else if (desc != null) sb.append(desc).append(" ");
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) sb.append(getAllText(child));
        }
        return sb.toString();
    }

    private Integer extractTimer(String text) {
        Pattern p = Pattern.compile("Select Time\\s+(\\d+)\\s*S", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                int sec = Integer.parseInt(m.group(1));
                if (sec == 29 || sec == 5 || (sec >= 0 && sec <= 60)) return sec;
            } catch (NumberFormatException e) { }
        }
        return null;
    }

    private List<Integer> extractRecentResultsFromNodes(AccessibilityNodeInfo root) {
        List<Integer> res = new ArrayList<>();
        AccessibilityNodeInfo resultsNode = findNodeByText(root, "Results", true);
        if (resultsNode != null) {
            AccessibilityNodeInfo parent = resultsNode.getParent();
            if (parent != null) {
                List<AccessibilityNodeInfo> nodesWithDesc = new ArrayList<>();
                findNodesWithContentDescription(parent, nodesWithDesc);
                for (AccessibilityNodeInfo node : nodesWithDesc) {
                    String desc = node.getContentDescription() != null ? node.getContentDescription().toString() : (node.getText() != null ? node.getText().toString() : null);
                    if (desc != null) {
                        int id = mapNameToId(desc);
                        if (id != -1) res.add(id);
                    }
                }
            }
        }
        // take last 6
        if (res.size() > 6) res = res.subList(res.size()-6, res.size());
        return res;
    }

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo node, String text, boolean exact) {
        CharSequence nodeText = node.getText();
        CharSequence nodeDesc = node.getContentDescription();
        String str = nodeText != null ? nodeText.toString() : (nodeDesc != null ? nodeDesc.toString() : null);
        if (str != null) {
            if (exact && str.equalsIgnoreCase(text)) return node;
            if (!exact && str.toLowerCase().contains(text.toLowerCase())) return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo found = findNodeByText(child, text, exact);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void findNodesWithContentDescription(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> out) {
        if ((node.getContentDescription() != null && node.getContentDescription().length() > 0) ||
            (node.getText() != null && node.getText().length() > 0)) {
            out.add(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) findNodesWithContentDescription(child, out);
        }
    }

    private int mapNameToId(String name) {
        String n = name.trim().toLowerCase();
        switch (n) {
            case "جزر": case "carrot": case "0": return 0;
            case "ذرة": case "corn": case "1": return 1;
            case "طماطم": case "tomato": case "2": return 2;
            case "بروكلي": case "broccoli": case "3": return 3;
            case "روبيان": case "shrimp": case "4": return 4;
            case "دجاج": case "chicken": case "5": return 5;
            case "ستيك": case "steak": case "6": return 6;
            case "سمك": case "fish": case "7": return 7;
            default: return -1;
        }
    }

    private Integer extractWinner(String text) {
        Pattern p = Pattern.compile("(?:Winner|فائز|Win|فاز)\\s*:?\\s*\\b([0-7])\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { }
        }
        return null;
    }

    private void saveResults(List<Integer> results) {
        SharedPreferences prefs = getSharedPreferences("greedy_bot", MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(results.get(i));
        }
        prefs.edit().putString("lastResults", sb.toString()).apply();
    }

    @Override
    public void onInterrupt() {}
}