import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HttpClient {
    private static final String host = "localhost";
    private static final int port = 8080;
    private static final int MAX_REDIRECTS = 5;
    private static int redirectCount = 0;

    public static void main(String[] args) {
        System.out.println("正在连接 " + host + ":" + port);

        try (Socket socket = new Socket(host, port)) {
            System.out.println("连接成功！");
            sendRequest(socket, "/");
            readResponse(socket);
        } catch (IOException e) {
            System.out.println("连接失败：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void sendRequest(Socket socket, String path) {
        try {
            OutputStream out = socket.getOutputStream();

            String request = "GET" + " " + path + " " + "HTTP/1.1\r\n" +
                    "Host: " + host + ":" + port + "\r\n" +
                    "Connection: keep-alive\r\n" +
                    "User-Agent: SimpleJavaHttpClient/1.0\r\n" +
                    "\r\n";

            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();
            System.out.println("HTTP 请求已发送：\n" + request);
        } catch (IOException e) {
            System.out.println("发送请求失败：" + e.getMessage());
        }
    }

    private static void readResponse(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in));

            System.out.println("服务器响应:");

            String statusLine = reader.readLine();
            System.out.println(statusLine);

            String[] statusParts = statusLine.split(" ");
            int statusCode = Integer.parseInt(statusParts[1]);

            String line;
            String location = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);

                if (line.isEmpty()) {
                    break;
                }

                if (line.toLowerCase().startsWith("location:")) {
                    location = line.substring(line.indexOf(':') + 1).trim();
                }
            }

            if ((statusCode == 301 || statusCode == 302) && location != null) {
                if (redirectCount >= MAX_REDIRECTS) {
                    System.out.println("达到最大重定向次数(" + MAX_REDIRECTS + ")");
                    return;
                }
                redirectCount++;

                URI redirectUri = URI.create(location);
                String newHost = redirectUri.getHost();
                int newPort = redirectUri.getPort() != -1 ? redirectUri.getPort() : (redirectUri.getScheme().equals("https") ? 443 : 80);
                String newPath = redirectUri.getPath().isEmpty() ? "/" : redirectUri.getPath();

                socket.close();

                try (Socket newSocket = new Socket(newHost, newPort)) {
                    System.out.println("重定向连接到 " + newHost + ":" + newPort);
                    sendRequest(newSocket, newPath);
                    readResponse(newSocket);
                }

            } else {
                String responseLine;
                while ((responseLine = reader.readLine()) != null) {
                    System.out.println(responseLine);
                }
            }

        } catch (IOException e) {
            System.out.println("读取响应失败：" + e.getMessage());
        }
    }
}