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
    public Youtube() {
    }
//    public  static void main(String[] args) throws IOException, InterruptedException {
//        getUrl();
//
//    }
    public String getUrl(String id) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ytstream-download-youtube-videos.p.rapidapi.com/dl?id="+id))
                .header("X-RapidAPI-Key", "97a6b739d4mshb22011e57b5efb1p189fa9jsn1f746cfc6bae")
                .header("X-RapidAPI-Host", "ytstream-download-youtube-videos.p.rapidapi.com")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        JSONArray formats = new JSONObject(response.body()).getJSONArray("formats");
        String link = formats.getJSONObject(formats.length()-1).getString("url");
        return link;

    }
    public String getShortsId(String link){
        Pattern pattern = Pattern.compile("/shorts/([A-Za-z0-9_-]+)");
        Matcher matcher = pattern.matcher(link);
        if (matcher.find()) {
            return matcher.group(1);
        }else return "SXHMnicI6Pg";

    }
    public String getVidId(String link){
        Pattern pattern = Pattern.compile("(?:v=|youtu\\.be\\/)([A-Za-z0-9_-]+)");
        Matcher matcher = pattern.matcher(link);
        if (matcher.find()) {
            return matcher.group(1);
        }else return "SXHMnicI6Pg";

    }


}
