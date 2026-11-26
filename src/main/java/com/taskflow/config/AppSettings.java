package com.taskflow.config;
import java.util.prefs.Preferences;
public final class AppSettings {
    private final Preferences prefs;
    public AppSettings() { this(Preferences.userRoot().node("com/taskflow")); }
    public AppSettings(Preferences preferences) { this.prefs=preferences; }
    public boolean dark() { return prefs.getBoolean("dark", false); }
    public void dark(boolean value) { prefs.putBoolean("dark", value); }
    public boolean notifications() { return prefs.getBoolean("notifications", true); }
    public void notifications(boolean value) { prefs.putBoolean("notifications", value); }
    public int reminderHours() { return prefs.getInt("reminderHours", 24); }
    public void reminderHours(int value) { prefs.putInt("reminderHours", value); }
    public String token() { return prefs.get("sessionToken", ""); }
    public void token(String value) { if (value == null || value.isBlank()) prefs.remove("sessionToken"); else prefs.put("sessionToken", value); }
}
