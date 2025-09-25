package server;

import lombok.Getter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Getter
public class HttpRequest {
    private String method;
    private String path;
    private String protocol;
    private final Map<String, String> headers;
    private byte[] body;

    public HttpRequest(InputStream inputStream) throws IOException {
        this.headers = new HashMap<>();
        parseRequest(inputStream);
    }

    private void parseRequest(InputStream inputStream) throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

        //解析请求行
        String requestLine = readLine(inputStream, lineBuffer);
        if (requestLine != null) {
            String[] parts = requestLine.split(" ");
            if (parts.length >= 3) {
                this.method = parts[0];
                this.path = parts[1];
                this.protocol = parts[2];
            }
        }

        //解析请求头
        String line;
        while ((line = readLine(inputStream, lineBuffer)) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(":");
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
        }

        //解析请求体
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

    /**
     * 判断长连接
     */
    public boolean isKeepAlive() {
        String connection = headers.getOrDefault("Connection", "");
        return "keep-alive".equalsIgnoreCase(connection) ||
                ("HTTP/1.1".equals(protocol) && !"close".equalsIgnoreCase(connection));
    }

    @Override
    public String toString() {
        return method + " " + path + " " + protocol + "\n" +
                "headers:" + headers + "\n" +
                (body == null ? "" : "body:" + (
                        headers.containsKey("Content-Type") && !headers.get("Content-Type").startsWith("text") ?
                                "[二进制数据 - " + body.length + "字节]" : new String(body, StandardCharsets.UTF_8)));
    }
}
