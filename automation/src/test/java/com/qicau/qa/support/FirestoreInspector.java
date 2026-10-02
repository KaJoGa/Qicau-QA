package com.qicau.qa.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Read-only helper for verifying a collection actually got written to, for things the UI itself
 * never displays (e.g. low_confidence_logs - LOWC-04/SEC-07..09). Uses the Firestore emulator's
 * standard REST API with the documented emulator-only "Bearer owner" token, which bypasses
 * security rules for admin/test purposes - the same category of emulator-specific tooling
 * knowledge already used for L3 ({@code @firebase/rules-unit-testing}) and Hooks' own
 * clear-data call, not anything read from the app's source.
 */
public final class FirestoreInspector {

  private FirestoreInspector() {
  }

  /** Raw JSON response of GET .../documents/{collection} - a "documents" array of Firestore
   * REST-format documents, or no "documents" key at all if the collection is empty. */
  public static String getCollectionRaw(String collection) {
    String url = String.format(
        "http://%s:%d/v1/projects/%s/databases/(default)/documents/%s",
        Config.emulatorFirestoreHost(), Config.emulatorFirestorePort(), Config.emulatorProjectId(), collection);
    try {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
          .header("Authorization", "Bearer owner")
          .GET()
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return response.body();
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException("Could not query the Firestore emulator collection " + collection, e);
    }
  }

  public static int countDocumentsContaining(String collection, String fragment) {
    String raw = getCollectionRaw(collection);
    int count = 0;
    int idx = 0;
    while ((idx = raw.indexOf("\"name\":", idx)) != -1) {
      int next = raw.indexOf("\"name\":", idx + 1);
      String docChunk = next == -1 ? raw.substring(idx) : raw.substring(idx, next);
      if (docChunk.contains(fragment)) {
        count++;
      }
      idx = (next == -1) ? raw.length() : next;
    }
    return count;
  }
}
