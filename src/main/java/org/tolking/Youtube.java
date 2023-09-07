package org.tolking;

import it.tdlight.jni.TdApi;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.tolking.Bot.*;

public class Youtube {
    public static final String apiKey = config.getProperty("youtubeKey");
    public static int width;
    public static int height;

    public static void checkYoutubeURL(TdApi.UpdateNewMessage update) {
        if (update.message.content instanceof TdApi.MessageText messageText) {
            String linkRegex = "^(?:(?:https|http):\\/\\/)?(?:www\\.)?(youtu.*be.*)\\/(watch\\?v=|embed\\/|v|shorts|)(.*?((?=[&#?])|$))";
            String shortsRegex = "^(?:(?:https|http):\\/\\/)?(?:www\\.)?(youtube.*com.*)\\/(shorts)(.*?((?=[&#?])|$))";
            String text = messageText.text.text;
            TdApi.Message message = update.message;
            long msgId = update.message.id;
            long chatId = update.message.chatId;
            long senderUserId = update.message.senderId instanceof TdApi.MessageSenderUser ? ((TdApi.MessageSenderUser) update.message.senderId).userId : 0;

            String id = matchesPattern(text, shortsRegex) ? getShortsId(text) : getVidId(text);
            if (matchesPattern(text, linkRegex) && senderUserId == USER_ID) {
                try {
                    // Create a CompletableFuture<Boolean> to control the loop
                    CompletableFuture<Boolean> isEnd = new CompletableFuture<>();

                    // Asynchronously send the video message
                    CompletableFuture<Void> videoCompletionFuture = CompletableFuture.runAsync(() -> {
                        try {
                            sendVideoMessage(message, getUrl(id));
                            // Set the result of the CompletableFuture to true when sendVideoMessage is completed
                            isEnd.complete(true);
                        } catch (IOException | InterruptedException e) {
                            isEnd.complete(false);
                            throw new RuntimeException(e);
                        }
                    });

                    // While loop controlled by the isEnd CompletableFuture
                    TdApi.InputMessageText editMessage = new TdApi.InputMessageText();
                    while (!isEnd.isDone()) {
                        // Update the "Please wait" message with different dots
                        editMessage.text = new TdApi.FormattedText("Please wait\uD83D\uDD52", null);
                        editMessage(chatId, msgId, editMessage);
                        TimeUnit.MILLISECONDS.sleep(400);
                        if (isEnd.isDone()) {
                            break;
                        }

                        editMessage.text = new TdApi.FormattedText("Please wait\uD83D\uDD55", null);
                        editMessage(chatId, msgId, editMessage);
                        TimeUnit.MILLISECONDS.sleep(400);
                        if (isEnd.isDone()) {
                            break;
                        }

                        editMessage.text = new TdApi.FormattedText("Please wait\uD83D\uDD58", null);
                        editMessage(chatId, msgId, editMessage);
                        TimeUnit.MILLISECONDS.sleep(400);
                        if (isEnd.isDone()) {
                            break;
                        }

                        editMessage.text = new TdApi.FormattedText("Please wait\uD83D\uDD5B", null);
                        editMessage(chatId, msgId, editMessage);
                        TimeUnit.MILLISECONDS.sleep(400);
                    }

                    // Delete the "Please wait" message
                    deleteMessage(chatId, msgId);

                    // Wait for the videoCompletionFuture to complete
                    videoCompletionFuture.join();

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

    public static String getUrl(String id) throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create("https://ytstream-download-youtube-videos.p.rapidapi.com/dl?id=" + id)).header("X-RapidAPI-Key", apiKey).header("X-RapidAPI-Host", "ytstream-download-youtube-videos.p.rapidapi.com").method("GET", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());

        JSONArray formats = new JSONObject(response.body()).getJSONArray("formats");
        var highQuality = formats.getJSONObject(formats.length() - 1);
        width = highQuality.getInt("width");
        height = highQuality.getInt("height");
        return highQuality.getString("url");
    }

    private static void sendVideoMessage(TdApi.Message message, String videoUrl) throws IOException {
        Path downloadedVideoPath = downloadVideo(videoUrl);
        sendFileMessage(message, downloadedVideoPath);
    }

    private static Path downloadVideo(String videoUrl) throws IOException {
        String filename = UUID.randomUUID() + ".mp4";
        Path downloadDirectory = Paths.get("downloaded_videos");
        if (!Files.exists(downloadDirectory)) {
            Files.createDirectories(downloadDirectory);
        }
        Path downloadedVideoPath = downloadDirectory.resolve(filename);
        try (InputStream in = new URL(videoUrl).openStream()) {
            Files.copy(in, downloadedVideoPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return downloadedVideoPath;
    }

    private static void sendFileMessage(TdApi.Message message, Path filePath) {

        // Now send the actual video
        TdApi.InputMessageVideo inputVideo = new TdApi.InputMessageVideo();
        inputVideo.video = new TdApi.InputFileLocal(filePath.toString());
        inputVideo.width = width;
        inputVideo.height = height;
        inputVideo.supportsStreaming = true;

        TdApi.SendMessage videoMessageRequest = new TdApi.SendMessage();
        videoMessageRequest.replyToMessageId = message.replyToMessageId;
        videoMessageRequest.chatId = message.chatId;
        videoMessageRequest.inputMessageContent = inputVideo;
        client.send(videoMessageRequest, videoResponse -> {
            // Handle the response if needed
        });

        // Schedule the file deletion
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.schedule(() -> {
            try {
                Files.deleteIfExists(filePath);
                System.out.println("File deleted: " + filePath);
            } catch (IOException e) {
                System.out.println("Can't delete");
            }
        }, 60, TimeUnit.SECONDS);

    }


    public static String getShortsId(String link) {
        Matcher matcher = Pattern.compile("/shorts/([A-Za-z0-9_-]+)").matcher(link);
        return matcher.find() ? matcher.group(1) : "SXHMnicI6Pg";
    }

    public static String getVidId(String link) {
        Matcher matcher = Pattern.compile("(?:v=|youtu\\.be/)([A-Za-z0-9_-]+)").matcher(link);
        return matcher.find() ? matcher.group(1) : "SXHMnicI6Pg";
    }

    public static boolean matchesPattern(String text, String pattern) {
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(text);
        return matcher.matches();
    }


}
