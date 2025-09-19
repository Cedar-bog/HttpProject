import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HttpServer {
    private static final int port = 8080;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("服务器在端口" + port + "启动……");

            while (true) {
                System.out.println("等待客户端连接……");
                Socket clientSocket = serverSocket.accept();
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
             BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null) return;
            System.out.println("请求行：" + requestLine);

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];

            System.out.println("请求头: ");
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                System.out.println(headerLine);
            }

            if (path.equals("/")) {
                sendResponse(out, 200, "text/html", "<html><body><h1>Hello, World!</h1></body></html>");
            } else {
                sendErrorResponse(out, 404, "Not Found");
            }

        } catch (IOException e) {
            System.out.println("处理客户端连接时发生错误：" + e.getMessage());
        }
    }

    private static void sendResponse(OutputStream out, int statusCode, String contentType, String content) {
        try {
            String statusText = getStatusText(statusCode);
            String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "Content-Length: " + content.length() + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" + content;

            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            System.out.println("发送响应时发生错误：" + e.getMessage());
        }
    }

    private static void sendErrorResponse(OutputStream out, int statusCode, String message) {
        String statusText = getStatusText(statusCode);
        String content = "<html><body><h1>" + statusCode + " " + statusText + "</h1><p>" + message + "</p></body></html>";
        sendResponse(out, statusCode, "text/html", content);
    }

    private static String getStatusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            default -> "Unknown Status";
        };
    }
}