package org.tolking;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import static org.tolking.Bot.config;

public class TextAnalysis {
    private static final String OCR_API_URL = "https://api.ocr.space/parse/image";
    private static final String OCR_API_KEY = config.getProperty("OCRKey");

    public static String imageToText(String imagePath) throws Exception {
        Path path = Path.of(imagePath);
        byte[] imageBytes = Files.readAllBytes(path);
        String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);
        String requestBody = "data:image/jpeg;base64," + base64Image;
        try {
            Files.deleteIfExists(path);
            System.out.println("File deleted: " + imagePath);
        } catch (IOException e) {
            System.out.println("Can't delete");
        }
        HttpsURLConnection con = prepareConnection();

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("apikey", OCR_API_KEY);
        postDataParams.put("base64Image", requestBody);

        sendPostRequest(con, postDataParams);

        String response = readResponse(con);
        return parseResponse(response);
    }

    private static HttpsURLConnection prepareConnection() throws IOException {
        URL obj = new URL(OCR_API_URL);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setDoOutput(true);
        return con;
    }

    private static void sendPostRequest(HttpsURLConnection con, JSONObject postDataParams) throws Exception {
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(getPostDataString(postDataParams));
            wr.flush();
        }
    }

    private static String readResponse(HttpsURLConnection con) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response.toString();
        }
    }

    private static String parseResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray parsedResultsArray = jsonObject.getJSONArray("ParsedResults");
            JSONObject firstObject = parsedResultsArray.getJSONObject(0);
            return firstObject.getString("ParsedText");
        } catch (Exception e) {
            return response; // Return the full response in case of an error
        }
    }

    private static String getPostDataString(JSONObject params) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        Iterator<String> itr = params.keys();

        while (itr.hasNext()) {
            String key = itr.next();
            Object value = params.get(key);

            if (first) first = false;
            else result.append("&");

            result.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
        }
        return result.toString();
    }
}
