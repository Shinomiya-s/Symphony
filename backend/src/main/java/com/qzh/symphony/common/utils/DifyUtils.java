package com.qzh.symphony.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class DifyUtils {

    @Value("${dify.api.url}")
    private String difyApiUrl;
    @Value("${dify.api.key}")
    private String difyApiKey;

    private final ObjectMapper mapper = new ObjectMapper();
    //利用信号量限流,最大3个并发
    private final Semaphore semaphore = new Semaphore(3);

    //用于限流加重试,doCallDify用于发送请求
    public String callDify(Map<String, Object> inputs, String query, String userId) throws IOException {
        try {
            if (!semaphore.tryAcquire(15, TimeUnit.SECONDS)) {
                throw new IOException("Dify 请求队列繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("请求被中断");
        }

        try {
            int maxRetry = 3;
            IOException lastException = null;
            //重试机制,最多尝试3次
            for (int attempt = 1; attempt <= maxRetry; attempt++) {
                try {
                    return doCallDify(inputs, query, userId);
                } catch (IOException e) {
                    lastException = e;
                    System.err.println("Dify调用失败，第" + attempt + "次，原因: " + e.getMessage());
                    if (attempt < maxRetry) {
                        try {
                            Thread.sleep(2000L * attempt); // 2s/4s/6s
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("请求被中断");
                        }
                    }
                }
            }
            throw lastException;
        } finally {
            //释放信号量
            semaphore.release();
        }
    }

    //实际的HTTP请求与响应解析
    private String doCallDify(Map<String, Object> inputs, String query, String userId) throws IOException {
        Map<String, Object> body = new HashMap<>();
        //构造请求体
        body.put("inputs", inputs);
        body.put("query", query);
        body.put("response_mode", "streaming");
        body.put("user", userId);
        String bodyJson = mapper.writeValueAsString(body);
        //发送请求
        URL url = new URL(difyApiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + difyApiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(300_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyJson.getBytes());
        }

        StringBuilder result = new StringBuilder();
        //读取回复
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //逐行解析SSE流
                if (!line.startsWith("data: ")) continue;
                String jsonStr = line.substring(6).trim();
                if (jsonStr.isEmpty()) continue;
                JsonNode node = mapper.readTree(jsonStr);
                String event = node.path("event").asText();
                if ("node_finished".equals(event)) {
                    JsonNode data = node.path("data");
                    if ("llm".equals(data.path("node_type").asText())) {
                        String text = data.path("outputs").path("text").asText();
                        if (!text.isEmpty()) result.append(text);
                    }
                } else if ("workflow_finished".equals(event)) {
                    break;
                }
            }
        }
        return result.toString();
    }
}