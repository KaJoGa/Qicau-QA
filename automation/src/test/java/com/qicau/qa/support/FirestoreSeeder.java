package com.qicau.qa.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Emulator-only data seeding for the "back-dated data" L4 cases (HIST-03, HIST-05, MON-08, MON-11).
 * Field names and types are NOT guessed from source: they were observed by reading what the app
 * itself wrote to the emulator after creating a transaction through the UI (REST, "Bearer owner").
 * Observed 2026-10-04 (project id = emulator.app.project.id):
 *   transactions/{autoId}: kategori, platform, detail, payment_method, confidence, raw_transcript,
 *     user_id (stringValue); harga (integerValue); created_at (integerValue = epoch milliseconds)
 *   daily_summaries/{uid}_{yyyyMMdd local date}: user_id, day (stringValue yyyyMMdd), total (integerValue),
 *     by_category (mapValue category -> integerValue)
 * Only ever pointed at the local emulator; never production.
 */
public final class FirestoreSeeder {

  private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  private FirestoreSeeder() {
  }

  public static String projectId() {
    return Config.get("emulator.app.project.id");
  }

  private static String docsBase() {
    return String.format("http://%s:%d/v1/projects/%s/databases/(default)/documents",
        Config.emulatorFirestoreHost(), Config.emulatorFirestorePort(), projectId());
  }

  private static String send(String method, String url, String body) {
    try {
      HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer owner")
          .header("Content-Type", "application/json");
      b = body == null ? b.method(method, HttpRequest.BodyPublishers.noBody())
          : b.method(method, HttpRequest.BodyPublishers.ofString(body));
      HttpResponse<String> r = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
      if (r.statusCode() >= 300) {
        throw new IllegalStateException(method + " " + url + " -> " + r.statusCode() + ": " + r.body());
      }
      return r.body();
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException("Firestore emulator call failed: " + method + " " + url, e);
    }
  }

  /** uid of the user document whose email matches (the app writes users/{uid} with an email field at sign-in). */
  public static String uidForEmail(String email) {
    String raw = send("GET", docsBase() + "/users?pageSize=300", null);
    Matcher m = Pattern.compile("\"name\":\\s*\"[^\"]*/users/([^\"]+)\"").matcher(raw);
    List<Integer> starts = new ArrayList<>();
    List<String> ids = new ArrayList<>();
    while (m.find()) {
      starts.add(m.start());
      ids.add(m.group(1));
    }
    for (int i = 0; i < ids.size(); i++) {
      int end = i + 1 < starts.size() ? starts.get(i + 1) : raw.length();
      if (raw.substring(starts.get(i), end).contains("\"" + email + "\"")) {
        return ids.get(i);
      }
    }
    throw new IllegalStateException("No users document for " + email + " in project " + projectId());
  }

  /** Local-time epoch millis of the given day offset at hh:mm (timezone = JVM/browser default). */
  public static long epochMillis(int daysAgo, int hour, int minute) {
    ZonedDateTime z = ZonedDateTime.now(ZoneId.systemDefault()).minusDays(daysAgo)
        .withHour(hour).withMinute(minute).withSecond(0).withNano(0);
    return z.toInstant().toEpochMilli();
  }

  public static long epochMillis(LocalDate date, int hour, int minute) {
    return date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  public static String dayKey(long epochMillis) {
    return java.time.Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
  }

  private static String str(String s) {
    return "{\"stringValue\":\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
  }

  /** Creates a transaction exactly like the app does (high confidence, QRIS, manual-input transcript). */
  public static void createTransaction(String uid, long createdAtMillis, String kategori, long harga, String platform) {
    String body = "{\"fields\":{"
        + "\"kategori\":" + str(kategori) + ","
        + "\"platform\":" + str(platform) + ","
        + "\"harga\":{\"integerValue\":\"" + harga + "\"},"
        + "\"detail\":" + str(kategori) + ","
        + "\"payment_method\":" + str("QRIS") + ","
        + "\"confidence\":" + str("high") + ","
        + "\"raw_transcript\":" + str("Input Manual: seeded") + ","
        + "\"user_id\":" + str(uid) + ","
        + "\"created_at\":{\"integerValue\":\"" + createdAtMillis + "\"}}}";
    send("POST", docsBase() + "/transactions", body);
  }

  /** Full replace of daily_summaries/{uid}_{day}. */
  public static void setDailySummary(String uid, String day, long total, Map<String, Long> byCategory) {
    StringBuilder cats = new StringBuilder();
    for (Map.Entry<String, Long> e : byCategory.entrySet()) {
      if (cats.length() > 0) {
        cats.append(',');
      }
      cats.append('"').append(e.getKey()).append("\":{\"integerValue\":\"").append(e.getValue()).append("\"}");
    }
    String body = "{\"fields\":{"
        + "\"user_id\":" + str(uid) + ","
        + "\"day\":" + str(day) + ","
        + "\"total\":{\"integerValue\":\"" + total + "\"},"
        + "\"by_category\":{\"mapValue\":{\"fields\":{" + cats + "}}}}}";
    send("PATCH", docsBase() + "/daily_summaries/" + uid + "_" + day, body);
  }

  public static void setDailySummary(String uid, String day, long total, String category, long amount) {
    Map<String, Long> m = new LinkedHashMap<>();
    m.put(category, amount);
    setDailySummary(uid, day, total, m);
  }

  /** Raw REST JSON of daily_summaries/{uid}_{day}. */
  public static String getDailySummaryRaw(String uid, String day) {
    return send("GET", docsBase() + "/daily_summaries/" + uid + "_" + day, null);
  }

  public static long summaryTotal(String raw) {
    Matcher m = Pattern.compile("\"total\":\\s*\\{\\s*\"integerValue\":\\s*\"(-?\\d+)\"").matcher(raw);
    return m.find() ? Long.parseLong(m.group(1)) : -1;
  }

  /** by_category parsed from the raw doc, e.g. {Makan=25000}. */
  public static Map<String, Long> summaryByCategory(String raw) {
    Map<String, Long> out = new LinkedHashMap<>();
    int i = raw.indexOf("\"by_category\"");
    if (i < 0) {
      return out;
    }
    String tail = raw.substring(i);
    int end = tail.indexOf("\"createTime\"");
    String sec = tail.substring(0, end < 0 ? tail.length() : end);
    Matcher m = Pattern.compile("\"([^\"]+)\":\\s*\\{\\s*\"integerValue\":\\s*\"(-?\\d+)\"").matcher(sec);
    while (m.find()) {
      out.put(m.group(1), Long.parseLong(m.group(2)));
    }
    return out;
  }

  public static String updateTime(String raw) {
    Matcher m = Pattern.compile("\"updateTime\":\\s*\"([^\"]+)\"").matcher(raw);
    return m.find() ? m.group(1) : "";
  }
}
