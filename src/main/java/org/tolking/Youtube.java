package org.tolking;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Youtube {
    public static final String apiKey = ""; // TODO: Replace with your API key from https://rapidapi.com/ytjar/api/ytstream-download-youtube-videos
    public String getUrl(String id) throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder()
                        .uri(URI.create("https://ytstream-download-youtube-videos.p.rapidapi.com/dl?id=" + id))
                        .header("X-RapidAPI-Key", apiKey)
                        .header("X-RapidAPI-Host", "ytstream-download-youtube-videos.p.rapidapi.com")
                        .method("GET", HttpRequest.BodyPublishers.noBody())
                        .build(), HttpResponse.BodyHandlers.ofString());

        JSONArray formats = new JSONObject(response.body()).getJSONArray("formats");
        return formats.getJSONObject(formats.length() - 1).getString("url");
    }

    public String getShortsId(String link) {
        Matcher matcher = Pattern.compile("/shorts/([A-Za-z0-9_-]+)").matcher(link);
        return matcher.find() ? matcher.group(1) : "SXHMnicI6Pg";
    }

    public String getVidId(String link) {
        Matcher matcher = Pattern.compile("(?:v=|youtu\\.be\\/)([A-Za-z0-9_-]+)").matcher(link);
        return matcher.find() ? matcher.group(1) : "SXHMnicI6Pg";
    }
}
