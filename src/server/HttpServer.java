package server;

import java.io.*;
import java.net.*;

public class HttpServer {
    private static final int port = 8080;
    private static final int SO_TIMEOUT = 5000000;
    private static final HttpRouter httpRouter = new HttpRouter();

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
                if (request.getMethod() == null || request.getPath() == null) {
                    System.out.println("客户端已关闭");
                    break;
                }
                System.out.println("\n接收到请求：\n" + request + "\n");
                keepAlive = request.isKeepAlive();

                HttpResponse response = new HttpResponse(out);

                try {
                    httpRouter.route(request, response);
                } catch (Exception e) {
                    System.out.println("路由处理错误: " + e.getMessage());
                    response.sendInternalServerError();
                }
            }

        } catch (SocketTimeoutException e) {
            System.out.println("客户端连接超时");
        } catch (IOException e) {
            System.out.println("处理客户端连接时发生错误：" + e.getMessage());
        }
    }
}