package com.example.travelapp.Api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatApi {
    private static final String CHAT_API_URL = "https://9f25-1-54-208-208.ngrok-free.app/chat";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public interface ChatCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public static void sendChatMessage(String message, ChatCallback callback) {
        new Thread(() -> {
            try {
                JSONObject jsonRequest = new JSONObject();
                jsonRequest.put("query", message);
                
                RequestBody requestBody = RequestBody.create(jsonRequest.toString(), JSON);
                
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();
                
                Request request = new Request.Builder()
                        .url(CHAT_API_URL)
                        .post(requestBody)
                        .header("Content-Type", "application/json")
                        .build();
                
                Response response = client.newCall(request).execute();
                
                if (!response.isSuccessful()) {
                    callback.onError("Error: " + response.code() + " " + response.message());
                } else {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String chatResponse = jsonResponse.getString("response");
                    callback.onSuccess(chatResponse);
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
} 