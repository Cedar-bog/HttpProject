package server;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpRouter {
    private static final Map<String, String> users = new HashMap<>();

    static {
        new File("images/").mkdirs();
    }

    public void route(HttpRequest request, HttpResponse response) {
        String method = request.getMethod();
        String path = request.getPath();

        //设置长连接
        response.setKeepAlive(request.isKeepAlive());

        //路由分发
        if ("/".equals(path) || "/index.html".equals(path)) {
            handleIndex(response);
        } else if ("/register".equals(path)) {
            if ("POST".equalsIgnoreCase(method)) {
                handleRegister(request, response);
            } else {
                response.sendMethodNotAllowed();
            }
        } else if ("/login".equals(path)) {
            if ("POST".equalsIgnoreCase(method)) {
                handleLogin(request, response);
            } else {
                response.sendMethodNotAllowed();
            }
        } else if (path.startsWith("/image/")) {
            if ("GET".equalsIgnoreCase(method)) {
                //handleDownload(request, response);
            } else if ("POST".equalsIgnoreCase(method)) {
                //handleUpload(request, response);
            } else {
                response.sendMethodNotAllowed();
            }
        } else {
            response.sendNotFound();
        }
    }

    /**
     * 处理主页
     */
    private void handleIndex(HttpResponse response) {
        String html = "<html><body>" +
                "<h1>HTTP Server 首页</h1>" +
                "<p>可用接口：</p>" +
                "<ul>" +
                "<li>POST /register username=123&password=456 - 用户注册</li>" +
                "<li>POST /login username=123&password=456 - 用户登录</li>" +
                "<li>GET /image - 获取图片</li>" +
                "<li>POST /image path - 上传图片</li>" +
                "</ul>" +
                "</body></html>";
        response.sendOK(html);
    }

    /**
     * 处理注册
     */
    private void handleRegister(HttpRequest request, HttpResponse response) {
        try {
            if (request.getBody() == null) {
                response.sendOK("{\"success\": false, \"message\": \"用户名和密码不能为空\"}");
                return;
            }

            String body = new String(request.getBody(), StandardCharsets.UTF_8);
            Map<String, String> params = parseFormData(body);

            String username = params.get("username");
            String password = params.get("password");

            if (username == null || password == null) {
                response.sendOK("{\"success\": false, \"message\": \"用户名和密码不能为空\"}");
                return;
            }

            synchronized (users) {
                if (users.containsKey(username)) {
                    response.sendOK("{\"success\": false, \"message\": \"用户名已存在\"}");
                } else {
                    users.put(username, password);
                    response.sendOK("{\"success\": true, \"message\": \"注册成功\"}");
                    System.out.println("用户注册：" + username);
                }
            }
        } catch (Exception e) {
            System.out.println("注册处理错误: " + e.getMessage());
            response.sendInternalServerError();
        }
    }

    /**
     * 处理登录
     */
    private void handleLogin(HttpRequest request, HttpResponse response) {
        try {
            if (request.getBody() == null) {
                response.sendOK("{\"success\": false, \"message\": \"用户名和密码不能为空\"}");
                return;
            }

            String body = new String(request.getBody(), StandardCharsets.UTF_8);
            Map<String, String> params = parseFormData(body);

            String username = params.get("username");
            String password = params.get("password");

            if (username == null || password == null) {
                response.sendOK("{\"success\": false, \"message\": \"用户名和密码不能为空\"}");
                return;
            }

            synchronized (users) {
                String storedPassword = users.get(username);
                if (storedPassword != null && storedPassword.equals(password)) {
                    response.sendOK("{\"success\": true, \"message\": \"登录成功\"}");
                    System.out.println("用户登录：" + username);
                } else {
                    response.sendOK("{\"success\": false, \"message\": \"用户名或密码错误\"}");
                }
            }
        } catch (Exception e) {
            System.out.println("登录处理错误: " + e.getMessage());
            response.sendInternalServerError();
        }
    }

    /**
     * 解析表单数据
     */
    private static Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        if (formData != null && !formData.isEmpty()) {
            String[] pairs = formData.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }

    //todo png处理
    //todo 304处理
}
