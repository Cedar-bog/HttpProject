package server;

import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private final OutputStream outputStream;
    @Setter
    private String version;
    private int statusCode;
    private String statusText;
    private final Map<String, String> headers;
    private byte[] body;

    public HttpResponse(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.headers = new HashMap<>();

        //设置默认
        setVersion("HTTP/1.1");
        setStatus(200);
        setHeader("Server", "HttpServer/1.0");
        setHeader("Connection", "close");
    }

    /**
     * 设置状态码和描述
     */
    void setStatus(int statusCode) {
        this.statusCode = statusCode;
        this.statusText = switch (statusCode) {
            case 200 -> "OK";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            default -> "Unknown Status";
        };
    }

    /**
     * 设置响应头
     */
    void setHeader(String key, String value) {
        headers.put(key, value);
    }

    /**
     * 设置响应体
     */
    void setBody(byte[] body) {
        this.body = body;
        setHeader("Content-Length", String.valueOf(body.length));
    }

    /**
     * 设置Content-Type
     */
    void setContentType(String contentType) {
        setHeader("Content-Type", contentType);
    }

    /**
     * 设置重定向
     */
    void setRedirect(String location) {
        if (statusCode == 301 || statusCode == 302) {
            setHeader("Location", location);
        }
    }

    /**
     * 设置长连接
     */
    void setKeepAlive(boolean keepAlive) {
        setHeader("Connection", keepAlive ? "keep-alive" : "close");
    }

    /**
     * 设置常用MIME类型
     */
    void setMimeType(String fileExtension) {
        switch (fileExtension.toLowerCase()) {
            case "html":
                setContentType("text/html; charset=utf-8");
                break;
            case "png":
                setContentType("image/png");
                break;
            default:
                setContentType("text/plain; charset=utf-8");
        }
    }

    void send() {
        try {
            StringBuilder response = new StringBuilder(version + " " + statusCode + " " + statusText + "\r\n");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                response.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
            }

            response.append("\r\n");

            outputStream.write(response.toString().getBytes(StandardCharsets.UTF_8));

            if (body != null) {
                outputStream.write(body);
            }

            outputStream.flush();

            System.out.println("已发送响应：\n" + response + (body == null ? "" : (
                    headers.containsKey("Content-Type") && !headers.get("Content-Type").startsWith("text") ?
                            "[二进制数据 - " + body.length + "字节]" : new String(body, StandardCharsets.UTF_8))));
        } catch (IOException e) {
            System.out.println("发送响应失败：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    void sendOK(String content) {
        setStatus(200);
        setContentType("text/html; charset=utf-8");
        setBody(content.getBytes());
        send();
    }

    void sendRedirect(String location, boolean permanent) {
        setStatus(permanent ? 301 : 302);
        setRedirect(location);
        setContentType("text/html; charset=utf-8");
        setBody(("<html><body><h1>" + statusCode + " " + statusText + "</h1><p>Redirect to <a href=\"" +
                location + "\">" + location + "</a></p></body></html>").getBytes());
        send();
    }

    void sendNotModified() {
        setStatus(304);
        send();
    }

    void sendNotFound() {
        setStatus(404);
        setContentType("text/html; charset=utf-8");
        setBody(("<html><body><h1>404 Not Found</h1><p>The requested resource was not found on this server.</p></body></html>").getBytes());
        send();
    }

    void sendMethodNotAllowed() {
        setStatus(405);
        setContentType("text/html; charset=utf-8");
        setBody(("<html><body><h1>405 Method Not Allowed</h1><p>The request method is not supported for the requested resource.</p></body></html>").getBytes());
        send();
    }

    void sendInternalServerError() {
        setStatus(500);
        setContentType("text/html; charset=utf-8");
        setBody(("<html><body><h1>500 Internal Server Error</h1><p>Something went wrong on the server.</p></body></html>").getBytes());
        send();
    }
}
