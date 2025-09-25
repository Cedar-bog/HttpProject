package client;

import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Getter
class HttpRequest {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private byte[] body;

    public HttpRequest(String method, String path, byte[] body) {
        this.headers = new HashMap<>();

        this.method = method;
        this.path = path;

        if ("POST".equalsIgnoreCase(method) && path.equals("/image")) {
            try {
                String filePath = new String(body, StandardCharsets.UTF_8);
                body = Files.readAllBytes(Paths.get(filePath));
                setContentType("image/png");
            } catch (IOException e) {
                System.out.println("读取文件失败：" + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        if (body != null) {
            this.body = body;
            setHeader("Content-Length", String.valueOf(body.length));
        }

        setHost(HttpClient.host, HttpClient.port);
        setUserAgent("HttpClient/1.0");
        setKeepAlive(true);

        addConditionalHeaders();
    }

    public void setHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setHost(String host, int port) {
        setHeader("Host", host + ":" + port);
    }

    public void setUserAgent(String userAgent) {
        setHeader("User-Agent", userAgent);
    }

    public void setContentType(String contentType) {
        setHeader("Content-Type", contentType);
    }

    public void setKeepAlive(boolean keepAlive) {
        setHeader("Connection", keepAlive ? "keep-alive" : "close");
    }

    private void addConditionalHeaders() {
        if (!"GET".equals(method) || !"/image".equals(path)) {
            return;
        }

        String cacheKey = method + ":" + path;
        HttpClient.CachedResponse cached = HttpClient.responseCache.get(cacheKey);

        if (cached != null) {
            if (cached.lastModified != null) {
                setHeader("If-Modified-Since", cached.lastModified);
            }

            if (cached.eTag != null) {
                setHeader("If-None-Match", cached.eTag);
            }
        }
    }

    public String toString() {
        StringBuilder request = new StringBuilder(method + " " + path + " HTTP/1.1\r\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }

        request.append("\r\n");

        return request.toString();
    }

    public void send(OutputStream out) {
        try {
            out.write(toString().getBytes(StandardCharsets.UTF_8));

            if (body != null) {
                out.write(body);
            }

            out.flush();
            System.out.println("\nHTTP 请求已发送：\n" + this + (body == null ? "" : (
                    headers.containsKey("Content-Type") && !headers.get("Content-Type").startsWith("text") ?
                            "[二进制数据 - " + body.length + "字节]" : new String(body, StandardCharsets.UTF_8))));
        } catch (IOException e) {
            System.out.println("发送请求失败：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
