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
    private String body;

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
    public void setStatus(int statusCode) {
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
    public void setHeader(String key, String value) {
        headers.put(key, value);
    }

    /**
     * 设置响应体
     */
    public void setBody(String body) {
        this.body = body;
        setHeader("Content-Length", String.valueOf(body.length()));
    }

    /**
     * 设置Content-Type
     */
    public void setContentType(String contentType) {
        setHeader("Content-Type", contentType);
    }

    /**
     * 设置重定向
     */
    public void setRedirect(String location) {
        if (statusCode == 301 || statusCode == 302) {
            setHeader("Location", location);
        }
    }

    /**
     * 设置长连接
     */
    public void setKeepAlive(boolean keepAlive) {
        setHeader("Connection", keepAlive ? "keep-alive" : "close");
    }

    /**
     * 发送响应
     */
    public void send() throws IOException {
        StringBuilder response = new StringBuilder(version + " " + statusCode + " " + statusText + "\r\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            response.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }

        response.append("\r\n");

        if (body != null) {
            response.append(body);
        }

        outputStream.write(response.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    /**
     * 设置常用MIME类型
     */
    public void setMimeType(String fileExtension) {
        switch (fileExtension.toLowerCase()) {
            case "html":
            case "htm":
                setContentType("text/html; charset=utf-8");
                break;
            case "css":
                setContentType("text/css");
                break;
            case "js":
                setContentType("application/javascript");
                break;
            case "json":
                setContentType("application/json");
                break;
            case "jpg":
            case "jpeg":
                setContentType("image/jpeg");
                break;
            case "png":
                setContentType("image/png");
                break;
            case "gif":
                setContentType("image/gif");
                break;
            default:
                setContentType("text/plain; charset=utf-8");
        }
    }

    /**
     * 发送错误响应
     */
    public void sendError(int code, String message) throws IOException {
        setStatus(code);
        setContentType("text/html; charset=utf-8");

        String errorPage = "<html><head><title>Error " + code + "</title></head>" +
                "<body><h1>Error " + code + ": " + statusText + "</h1>" +
                "<p>" + message + "</p></body></html>";

        setBody(errorPage);
        send();
    }

    /**
     * 发送重定向响应
     */
    public void sendRedirect(String location) throws IOException {
        setStatus(302);
        setHeader("Location", location);
        setBody("<html><body>Redirecting to <a href=\"" + location + "\">" + location + "</a></body></html>");
        send();
    }

    /**
     * 发送成功响应
     */
    public void sendSuccess(String content) throws IOException {
        setStatus(200);
        setContentType("text/html; charset=utf-8");
        setBody(content);
        send();
    }
}
