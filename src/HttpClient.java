import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HttpClient {
    private static final String host = "localhost";
    private static final int port = 8080;

    public static void main(String[] args) {
        System.out.println("正在连接 " + host + ":" + port);

        try (Socket socket = new Socket(host, port)) {
            System.out.println("连接成功！");
            sendRequest(socket);
            readResponse(socket);
        } catch (IOException e) {
            System.out.println("连接失败：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void sendRequest(Socket socket) {
        try {
            OutputStream out = socket.getOutputStream();

            String request = "GET / HTTP/1.1\r\n" +
                    "Host: " + host + ":" + port + "\r\n" +
                    "Connection: close\r\n" +
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
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            System.out.println("读取响应失败：" + e.getMessage());
        }
    }
}