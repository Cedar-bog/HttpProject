package client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

class HttpResponse {
    private String version;
    int statusCode;
    private String statusText;
    final Map<String, String> headers;
    byte[] body;
    String savedFilePath;

    public HttpResponse(InputStream inputStream) {
        this.headers = new HashMap<>();
        try {
            parseResponse(inputStream);
        } catch (IOException e) {
            System.out.println("解析响应失败：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void parseResponse(InputStream inputStream) throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

        // 解析响应行
        String responseLine = readLine(inputStream, lineBuffer);
        if (responseLine != null) {
            String[] parts = responseLine.split(" ", 3);
            if (parts.length >= 3) {
                this.version = parts[0];
                this.statusCode = Integer.parseInt(parts[1]);
                this.statusText = parts[2];
            }
        }

        // 解析响应头
        String line;
        while ((line = readLine(inputStream, lineBuffer)) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(":");
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
        }

        if (statusCode == 304) {
            return;
        }

        // 解析响应正文
        if (headers.containsKey("Content-Length")) {
            int contentLength = Integer.parseInt(headers.get("Content-Length"));
            if (contentLength > 0) {
                byte[] bodyBytes = new byte[contentLength];
                int bytesRead = 0;
                while (bytesRead < contentLength) {
                    int n = inputStream.read(bodyBytes, bytesRead, contentLength - bytesRead);
                    if (n == -1) break;
                    bytesRead += n;
                }
                this.body = bodyBytes;

                handleBody();
            }
        }
    }

    private String readLine(InputStream inputStream, ByteArrayOutputStream lineBuffer) throws IOException {
        lineBuffer.reset();
        int b;
        while ((b = inputStream.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                lineBuffer.write(b);
            }
        }

        if (lineBuffer.size() == 0 && b == -1) {
            return null;
        }

        return lineBuffer.toString(StandardCharsets.UTF_8);
    }

    private void handleBody() throws IOException {
        String contentType = headers.get("Content-Type");
        if (contentType == null) return;

        if (contentType.startsWith("image/png")) {
            savePngFile("client/" + System.currentTimeMillis() + ".png");
        }
    }

    private void savePngFile(String filename) throws IOException {
        Path filePath = Paths.get(filename);
        Files.write(filePath, body);
        this.savedFilePath = filePath.toAbsolutePath().toString();
    }

    public String toString() {
        StringBuilder response = new StringBuilder(version + " " + statusCode + " " + statusText + "\r\n");

        if (statusCode == 304) {
            response.append("\r\n[资源未修改]\r\n");
            return response.toString();
        }

        for (Map.Entry<String, String> header : headers.entrySet()) {
            response.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }

        response.append("\r\n");

        String contentType = headers.get("Content-Type");

        if (contentType != null && !contentType.startsWith("image/") && body != null) {
            response.append(new String(body, StandardCharsets.UTF_8));
        } else if (savedFilePath != null) {
            response.append("[文件已保存到: ").append(savedFilePath).append("]");
        }

        return response.toString();
    }
}
