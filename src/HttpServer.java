import java.io.*;
import java.net.*;

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

                HttpResponse response = new HttpResponse(out);
                response.setKeepAlive(keepAlive);

                if (request.getPath().equals("/")) {
                    response.sendSuccess("<html><body><h1>Hello, World!</h1></body></html>");
                } else {
                    response.sendError(404, "Not Found");
                }
            }

        } catch (SocketTimeoutException e) {
            System.out.println("客户端连接超时");
        } catch (IOException e) {
            System.out.println("处理客户端连接时发生错误：" + e.getMessage());
        }
    }
}