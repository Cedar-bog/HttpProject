import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HttpServer {
    private static final int port = 8080;
    private static final int SO_TIMEOUT = 5000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("服务器在端口" + port + "启动……");
            serverSocket.setReuseAddress(true);

            while (true) {
                System.out.println("等待客户端连接……");
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(SO_TIMEOUT);
                System.out.println("接收到来自" + clientSocket.getInetAddress() + "的连接");
                handleRequest(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("服务器启动时发生错误：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try (clientSocket;
             InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            boolean keepAlive = true;

            while (keepAlive) {
                HttpRequest request = new HttpRequest(in);
                System.out.println("接收到请求：\n" + request);
                keepAlive = request.isKeepAlive();

                if (request.getPath().equals("/")) {
                    sendResponse(out, 200, "text/html", "<html><body><h1>Hello, World!</h1></body></html>", keepAlive);
                } else {
                    sendErrorResponse(out, 404, "Not Found", keepAlive);
                }
            }

        } catch (SocketTimeoutException e) {
            System.out.println("客户端连接超时");
        } catch (IOException e) {
            System.out.println("处理客户端连接时发生错误：" + e.getMessage());
        }
    }

    private static void sendResponse(OutputStream out, int statusCode, String contentType, String content, boolean keepAlive) {
        try {
            String statusText = getStatusText(statusCode);
            String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "Content-Length: " + content.length() + "\r\n" +
                    "Connection: " + (keepAlive ? "keep-alive" : "close") +
                    "\r\n" + content;

            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            System.out.println("发送响应时发生错误：" + e.getMessage());
        }
    }

    private static void sendErrorResponse(OutputStream out, int statusCode, String message, boolean keepAlive) {
        String statusText = getStatusText(statusCode);
        String content = "<html><body><h1>" + statusCode + " " + statusText + "</h1><p>" + message + "</p></body></html>";
        sendResponse(out, statusCode, "text/html", content, keepAlive);
    }

    private static String getStatusText(int statusCode) {
        return switch (statusCode) {
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
}