package client;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HttpClient {
    static final String host = "localhost";
    static final int port = 8080;
    private static final int MAX_REDIRECTS = 5;
    private static int redirectCount = 0;
    static final Map<String, CachedResponse> responseCache = new HashMap<> ();

    static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("txt", "text/plain");
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("png", "image/png");
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("正在连接 " + host + ":" + port);

        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {
            System.out.println("连接成功！");
            System.out.println("请求格式： <方法> <路径> [请求体/文件地址]");
            System.out.println("示例： GET /index.html");
            System.out.println("示例： POST /image.png C:\\picture\\image.png");
            while (true) {
                String input = scanner.nextLine();
                String[] parts = input.split(" ", 3);
                String method = parts[0].toUpperCase();
                String path = parts.length > 1 ? parts[1] : "/";
                String body = parts.length > 2 ? parts[2] : null;
                executeRequest(out, in, method, path, body);
            }
        } catch (IOException e) {
            System.out.println("连接失败：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void executeRequest(OutputStream out, InputStream in, String method, String path, String body) {

        HttpRequest request = new HttpRequest(method, path, body == null ? null : body.getBytes());
        request.send(out);
        HttpResponse response = new HttpResponse(in);

        System.out.println("服务器响应：\n" + response);

        if (response.statusCode == 301 || response.statusCode == 302) {
            String location = response.headers.get("Location");
            if (redirectCount < MAX_REDIRECTS) {
                //由于仅针对本地固定服务器，忽略了重定向至其他主机或端口时需新建Socket的情况
                executeRequest(out, in, method, location, body);
                redirectCount++;
            } else {
                System.out.println("重定向次数过多，已停止重定向。");
            }
        } else if (response.statusCode == 304) {
            String cacheKey = method + ":" + path;
            CachedResponse cached = responseCache.get(cacheKey);
            if (cached != null) {
                response.body = cached.body;
                response.savedFilePath = cached.savedFilePath;
            } else {
                System.out.println("收到304响应，但未找到缓存内容");
            }
        } else if (response.statusCode == 200 && "GET".equals(method)) {
            String cacheKey = method + ":" + path;
            CachedResponse cached = new CachedResponse();
            cached.body = response.body;
            cached.savedFilePath = response.savedFilePath;
            cached.lastModified = response.headers.get("Last-Modified");
            cached.eTag = response.headers.get("ETag");

            responseCache.put(cacheKey, cached);
            System.out.println("已缓存响应：" + cacheKey);
        }
    }

    static class CachedResponse {
        byte[] body;
        String savedFilePath;
        String lastModified;
        String eTag;
    }

}