import lombok.Getter;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class HttpClient {
    private static final String host = "localhost";
    private static final int port = 8080;
    private static final int MAX_REDIRECTS = 5;
    private static int redirectCount = 0;

    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("txt", "text/plain");
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("png", "image/png");
    }

    public static void main(String[] args) {
        System.out.println("正在连接 " + host + ":" + port);

        try (Socket socket = new Socket(host, port)) {
            System.out.println("连接成功！");

            executeRequest(socket, "GET", "/");

        } catch (IOException e) {
            System.out.println("连接失败：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void executeRequest(Socket socket, String method, String path) {
        executeRequest(socket, method, path, null);
    }

    private static void executeRequest(Socket socket, String method, String path, String body) {
        try (OutputStream out = socket.getOutputStream()) {

            HttpRequest request = new HttpRequest(method, path, body);
            request.send(out);
            HttpResponse response = new HttpResponse(request, socket);

            System.out.println("服务器响应：\n" + response);

        } catch (IOException e) {
            System.out.println("发送请求失败：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Getter
    private static class HttpRequest {
        private final String method;
        private final String path;
        private final Map<String, String> headers;
        private String body;

        public HttpRequest (String method, String path, String body) {
            this.headers = new HashMap<>();

            this.method = method;
            this.path = path;
            if (body != null) {
                this.body = body;
                setHeader("Content-Length", String.valueOf(body.length()));
            }

            setHost(host, port);
            setUserAgent("HttpClient/1.0");
            setKeepAlive(true);

            setContentTypeBasedOnPath(path);
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

        private void setContentTypeBasedOnPath(String path) {
            String extension = getFileExtension(path);
            String contentType = MIME_TYPES.getOrDefault(extension, "application/octet-stream");
            setContentType(contentType);
        }

        private String getFileExtension(String path) {
            int lastDotIndex = path.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < path.length() - 1) {
                return path.substring(lastDotIndex + 1).toLowerCase();
            }
            return "html";
        }

        public String toString() {
            StringBuilder request = new StringBuilder(method + " " + path + " HTTP/1.1\r\n");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                request.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
            }

            request.append("\r\n");

            if (body != null) {
                request.append(body);
            }

            return request.toString();
        }

        public void send(OutputStream out) {
            try {
                out.write(toString().getBytes(StandardCharsets.UTF_8));
                out.flush();
                System.out.println("HTTP 请求已发送：\n" + this);
            } catch (IOException e) {
                System.out.println("发送请求失败：" + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    private static class HttpResponse {
        private String version;
        private int statusCode;
        private String statusText;
        private final Map<String, String> headers;
        private byte[] body;
        private String savedFilePath;

        public HttpResponse(HttpRequest request, Socket socket) {
            this.headers = new HashMap<>();
            try (InputStream inputStream = socket.getInputStream()) {
                parseResponse(inputStream);

                if (statusCode == 301 || statusCode == 302) {
                    String location = headers.get("Location");
                    if (redirectCount < MAX_REDIRECTS) {
                        //由于仅针对本地固定服务器，忽略了重定向至其他主机或端口时需新建Socket的情况
                        executeRequest(socket, request.method, location);
                        redirectCount++;
                    } else {
                        System.out.println("重定向次数过多，已停止重定向。");
                    }
                }

            } catch (IOException e) {
                System.out.println("解析响应失败：" + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        private void parseResponse(InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            //解析响应行
            String responseLine = bufferedReader.readLine();
            if (responseLine != null) {
                String[] parts = responseLine.split(" ");
                if (parts.length >= 3) {
                    this.version = parts[0];
                    this.statusCode = Integer.parseInt(parts[1]);
                    this.statusText = parts[2];
                }
            }

            //解析响应头
            String line;
            while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
                int colonIndex = line.indexOf(":");
                if (colonIndex > 0) {
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();
                    headers.put(key, value);
                }
            }

            //解析响应正文
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

                    handleBodyBasedOnContentType();
                }
            }
        }

        private void handleBodyBasedOnContentType() throws IOException {
            String contentType = headers.get("Content-Type");
            if (contentType == null) return;

            if (contentType.startsWith("image/png")) {
                savePngFile(System.currentTimeMillis() + ".png");
            }
        }

        private void savePngFile(String filename) throws IOException {
            Path filePath = Paths.get(filename);
            Files.write(filePath, body);
            this.savedFilePath = filePath.toAbsolutePath().toString();
            System.out.println("文件已保存到：" + savedFilePath);
        }

        public String toString() {
            StringBuilder response = new StringBuilder(version + " " + statusCode + " " + statusText + "\r\n");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                response.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
            }

            response.append("\r\n");

            String contentType = headers.get("Content-Type");
            if (contentType != null && contentType.startsWith("text/") && body != null) {
                response.append(new String(body, StandardCharsets.UTF_8));
            } else if (savedFilePath != null) {
                response.append("[文件已保存到: ").append(savedFilePath).append("]");
            }

            return response.toString();
        }
    }
}