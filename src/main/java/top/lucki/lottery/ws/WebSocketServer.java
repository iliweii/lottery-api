package top.lucki.lottery.ws;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class WebSocketServer extends TextWebSocketHandler implements WebSocketService {

    // 保存所有连接的会话
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);  // 添加新的连接
        log.info("新的连接建立，当前连接数：{}", sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);  // 关闭连接后移除会话
        log.info("连接关闭，当前连接数：{}", sessions.size());
    }

    // 从后端主动发送消息到所有客户端
    @Async  // 保持异步
    @Override
    public void sendMessage(WsMessage message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(JSONUtil.toJsonStr(message)));
                    log.info("成功发送消息: {}", message);
                } catch (IOException e) {
                    log.error("消息发送失败", e);  // 捕获异常并记录，但不影响其他推送任务
                }
            }
        }
    }
}
