package org.tolking;

import it.tdlight.client.*;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.client.TDLibSettings;
import it.tdlight.Init;
import it.tdlight.jni.TdApi;
import it.tdlight.jni.TdApi.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public final class Bot {
    private static final Properties config = loadConfig();

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream("config.properties")) {
            properties.load(fileInputStream);
        } catch (IOException e) {
            System.out.println("Config.properties file load error.");
            System.exit(1);
        }
        return properties;
    }

    private static final int API_ID = Integer.parseInt(config.getProperty("api_id"));
    private static final String HASH_CODE = config.getProperty("api_hash");
    private static final String PHONE_NUMBER = config.getProperty("phone_number");
    private static final long USER_ID = Long.parseLong(config.getProperty("user_id"));
    private static final long ADMIN_ID = Long.parseLong(config.getProperty("admin_id"));
    private static SimpleTelegramClient client;

    public static void main(String[] args) throws Exception {
        Init.init();
        try (SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory()) {
            APIToken apiToken = new APIToken(API_ID, HASH_CODE);
            TDLibSettings settings = TDLibSettings.create(apiToken);
            Path sessionPath = Paths.get("example-tdlight-session");
            settings.setDatabaseDirectoryPath(sessionPath.resolve("data"));
            settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));

            SimpleTelegramClientBuilder clientBuilder = clientFactory.builder(settings);
            SimpleAuthenticationSupplier<?> authenticationData = AuthenticationSupplier.user(PHONE_NUMBER);

            setupHandlers(clientBuilder);
            client = clientBuilder.build(authenticationData);
            client.waitForExit();
        }
    }

    private static void setupHandlers(SimpleTelegramClientBuilder clientBuilder) {
        clientBuilder.addUpdateHandler(UpdateAuthorizationState.class, Bot::onUpdateAuthorizationState);
        clientBuilder.addUpdateHandler(UpdateNewMessage.class, Bot::onUpdateNewMessage);
        clientBuilder.addCommandHandler("stop", new StopCommandHandler());
    }

    private static class StopCommandHandler implements CommandHandler {
        @Override
        public void onCommand(Chat chat, MessageSender commandSender, String arguments) {
            if (isAdmin(commandSender)) {
                System.out.println("Received stop command. Closing...");
                client.sendClose();
            }
        }
    }

    private static boolean isAdmin(MessageSender sender) {
        return sender instanceof MessageSenderUser && ((MessageSenderUser) sender).userId == ADMIN_ID;
    }

    private static void onUpdateAuthorizationState(UpdateAuthorizationState update) {
        AuthorizationState authorizationState = update.authorizationState;
        String state = "";
        if (authorizationState instanceof AuthorizationStateReady) state = "Logged in";
        else if (authorizationState instanceof AuthorizationStateClosing) state = "Closing...";
        else if (authorizationState instanceof AuthorizationStateClosed) state = "Closed";
        else if (authorizationState instanceof AuthorizationStateLoggingOut) state = "Logging out...";
        System.out.println(state);
    }

    private static void onUpdateNewMessage(TdApi.UpdateNewMessage update) {
        MessageContent messageContent = update.message.content;
        // If message is text
        if (messageContent instanceof TdApi.MessageText messageText) {
            //YouTube Link regex
            String linkRegex = "^(?:(?:https|http):\\/\\/)?(?:www\\.)?(youtu.*be.*)\\/(watch\\?v=|embed\\/|v|shorts|)(.*?((?=[&#?])|$))";
            String shortsRegex = "^(?:(?:https|http):\\/\\/)?(?:www\\.)?(youtube.*com.*)\\/(shorts)(.*?((?=[&#?])|$))";
            String text = messageText.text.text;
            long senderUserId = update.message.senderId instanceof TdApi.MessageSenderUser ? ((TdApi.MessageSenderUser) update.message.senderId).userId : 0;
            TdApi.Message message = update.message;
            long msgId = update.message.id;
            long chatId = update.message.chatId;
            long replyToMessageId = update.message.replyToMessageId;

            System.out.println(text);

            // If message start witch "check"
            if (replyToMessageId != 0 && text.trim().startsWith("check")) {
                final Photo[] photo = new Photo[1];
                // Retrieve the replied message by its replyMessageID
                TdApi.GetMessage getMessage = new TdApi.GetMessage(update.message.chatId, replyToMessageId);
                client.send(getMessage, repliedMessage -> {
                    if (repliedMessage.get().content instanceof TdApi.MessagePhoto messagePhoto && repliedMessage != null) {
                        photo[0] = messagePhoto.photo;
                        if (photo[0] != null) {
                            int id = getLargestPhotoSize(photo[0]).photo.id;
                            var request = new DownloadFile(id, 32, 0, 0, true);
                            client.send(request, result -> {
                                try {
                                    deleteMessage(chatId, msgId);
                                    String path = result.get().local.path;
                                    String resultText = TextAnalysis.imageToText(path);

                                    InputMessageText textContent = new InputMessageText();
                                    textContent.text = new TdApi.FormattedText((resultText.isEmpty() ? "\uD83E\uDD16: I couldn't detect image" : resultText), null);
                                    System.out.println(path);
                                    TdApi.SendMessage textReq = new TdApi.SendMessage();
                                    textReq.replyToMessageId = message.replyToMessageId;
                                    textReq.chatId = message.chatId;
                                    textReq.inputMessageContent = textContent;
                                    client.send(textReq, videoResponse -> {
                                        // Handle the response if needed
                                    });
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        }
                    }
                });


            }
            // If text starts with $
            else if (text.charAt(0) == '$') {
                try {
                    animText(text, msgId, chatId);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            //if text is YouTube link
            else if (matchesPattern(text, linkRegex) && senderUserId == USER_ID) {
                Youtube youtube = new Youtube();
                String id = matchesPattern(text, shortsRegex) ? youtube.getShortsId(text) : youtube.getVidId(text);
                try {
                    // Create a CompletableFuture<Boolean> to control the loop
                    CompletableFuture<Boolean> isEnd = new CompletableFuture<>();

                    // Asynchronously send the video message
                    CompletableFuture<Void> videoCompletionFuture = CompletableFuture.runAsync(() -> {
                        try {
                            sendVideoMessage(message, youtube.getUrl(id));
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
            //If you want to animate text

        }
    }


    //Video message
    private static void sendVideoMessage(TdApi.Message message, String videoUrl) throws IOException {
        Path downloadedVideoPath = downloadVideo(videoUrl);
        sendFileMessage(message, downloadedVideoPath);
    }

    //Delete message
    private static void deleteMessage(long chatId, long messageId) {
        TdApi.DeleteMessages request = new TdApi.DeleteMessages(chatId, new long[]{messageId}, true);
        client.send(request, response -> {
        });
    }

    //Edit message
    private static void editMessage(long chatId, long messageId, TdApi.InputMessageContent message) {
        TdApi.EditMessageText request = new TdApi.EditMessageText(chatId, messageId, null, message);
        client.send(request, response -> {
        });
    }

    //Animated text
    private static void animText(String text, long msgId, long chatId) throws InterruptedException {
        TdApi.InputMessageText editMessage = new TdApi.InputMessageText();
        String txt = text.substring(1).trim();
        int time = (txt.length() > 40) ? 500 : 200;
        String add;
        for (int i = 1; i <= txt.length(); i++) {
            if (i != txt.length()) {
                add = (i % 2 == 1) ? "▒" : "";
            } else add = "";
            editMessage.text = new TdApi.FormattedText(txt.substring(0, i) + add, null);
            editMessage(chatId, msgId, editMessage);
            TimeUnit.MILLISECONDS.sleep(time);
        }
    }

    private static Path downloadVideo(String videoUrl) throws IOException {
        String filename = UUID.randomUUID().toString() + ".mp4";
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

    private static void sendFileMessage(TdApi.Message message, Path filePath) throws IOException {

        // Now send the actual video
        TdApi.InputMessageVideo inputVideo = new TdApi.InputMessageVideo();
        inputVideo.video = new TdApi.InputFileLocal(filePath.toString());

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

    public static boolean matchesPattern(String text, String pattern) {
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(text);
        return matcher.matches();
    }

    private static TdApi.PhotoSize getLargestPhotoSize(TdApi.Photo photo) {
        TdApi.PhotoSize largestSize = null;
        for (TdApi.PhotoSize size : photo.sizes) {
            if (largestSize == null || size.width > largestSize.width) {
                largestSize = size;
            }
        }
        return largestSize;
    }
}
