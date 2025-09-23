import lombok.Getter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Getter
public class HttpRequest {
    private String method;
    private String path;
    private String fullPath;
    private final Map<String, String> formParams;
    private String version;
    private final Map<String, String> headers;
    private String body;
    private final Map<String, String> queryParams;

    public HttpRequest(InputStream inputStream) throws IOException {
        this.headers = new HashMap<>();
        this.formParams = new HashMap<>();
        this.queryParams = new HashMap<>();
        parseRequest(inputStream);
    }

    private void parseRequest(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        //解析请求行
        String requestLine = bufferedReader.readLine();
        if (requestLine != null) {
            String[] parts = requestLine.split(" ");
            if (parts.length >= 3) {
                this.method = parts[0];
                this.fullPath = parts[1];
                this.version = parts[2];

                String[] pathParts = parts[1].split("\\?");
                this.path = pathParts[0];
                if (pathParts.length > 1) {
                    parseParams(pathParts[1], queryParams);
                }
            }
        }

        //解析请求头
        String line;
        while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(":");
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
        }

        //解析请求体
        if (headers.containsKey("Content-Length")) {
            int contentLength = Integer.parseInt(headers.get("Content-Length"));
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                if (bufferedReader.read(bodyChars, 0, contentLength) > 0) {
                    this.body = new String(bodyChars);

                    if (headers.getOrDefault("Content-Type", "").contains("application/x-www-form-urlencoded")) {
                        parseParams(this.body, formParams);
                    }
                }
            }
        }
    }

    /**
     * 解析查询参数/表单数据
     */
    private void parseParams(String string, Map<String, String> params) {
        String[] pairs = string.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            } else {
                params.put(keyValue[0], "");
            }
        }
    }

    /**
     * 判断长连接
     */
    public boolean isKeepAlive() {
        String connection = headers.getOrDefault("Connection", "");
        return "keep-alive".equalsIgnoreCase(connection) ||
                ("HTTP/1.1".equals(version) && !"close".equalsIgnoreCase(connection));
    }

    @Override
    public String toString() {
        return method + " " + fullPath + " " + version + "\n" +
                "headers:" + headers + "\n" +
                "body:" + body;
    }
}
