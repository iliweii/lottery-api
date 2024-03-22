package top.lucki.lottery.proxy.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import top.lucki.lottery.common.exception.BaseException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProxyController {

    @RequestMapping("/**")
    @CrossOrigin(originPatterns = "*", allowCredentials = "true")
    public String proxyRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String proxyPath = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);
        // 获取请求方法
        String method = request.getMethod();

        // 构建代理请求URL
        String backendUrl = "http://47.92.55.242:8050/" + proxyPath;

        // 根据请求方法构建不同类型的代理请求
        HttpRequest proxyRequest;
        if ("GET".equalsIgnoreCase(method)) {
            proxyRequest = HttpRequest.get(backendUrl);
            // 给proxyRequest添加请求参数
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                String paramValue = request.getParameter(paramName);
                proxyRequest.form(paramName, paramValue);
            }
        } else if ("POST".equalsIgnoreCase(method)) {
            // 检查Content-Type是否为application/json
            if ("application/json".equalsIgnoreCase(request.getContentType())) {
                // 如果是application/json，则直接读取请求体内容，并作为请求体发送给后端服务
                StringBuilder requestBody = new StringBuilder();
                try (BufferedReader reader = request.getReader()) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        requestBody.append(line);
                    }
                }
                proxyRequest = HttpRequest.post(backendUrl).body(requestBody.toString());
            } else {
                // 否则，按照表单参数的方式处理
                proxyRequest = HttpRequest.post(backendUrl);
                Enumeration<String> parameterNames = request.getParameterNames();
                while (parameterNames.hasMoreElements()) {
                    String paramName = parameterNames.nextElement();
                    String paramValue = request.getParameter(paramName);
                    proxyRequest.form(paramName, paramValue);
                }
            }
        } else {
            // 其他请求类型暂不支持
            return "Unsupported method";
        }

        // 将原始请求的头部信息添加到代理请求中
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            proxyRequest.header(headerName, headerValue);
        }

        // 执行代理请求
        HttpResponse proxyResponse = proxyRequest.execute();

        if (!proxyResponse.isOk()) {
            response.setStatus(proxyResponse.getStatus());
        }

        // 获取响应中的JSESSIONID值
        String sessionId = proxyResponse.getCookieValue("JSESSIONID");
        if (StrUtil.contains(proxyPath, "/login") && sessionId != null && StrUtil.isNotBlank(sessionId)) {
            Cookie cookie = new Cookie("JSESSIONID", sessionId);
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        log.info("Proxy request to {} with method {} and headers {}", backendUrl, method, request.getHeaderNames().toString());
        log.info("Proxy response from {} with status {} and body {}", backendUrl, proxyResponse.getStatus(), proxyResponse.body());

        // 将真正后端服务的响应返回给前端
        return proxyResponse.body();
    }
}
