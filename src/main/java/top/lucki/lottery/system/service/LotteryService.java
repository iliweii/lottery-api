package top.lucki.lottery.system.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.lucki.lottery.system.entity.Process;
import top.lucki.lottery.ws.WebSocketServer;
import top.lucki.lottery.ws.WsMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class LotteryService {

    private final WebSocketServer webSocketServer;

    private final AtomicBoolean isCountdownActive = new AtomicBoolean(false);

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);  // 创建线程池

    private long countdownStartTime = 0; // 用来记录倒计时开始的时间

    public boolean isCountdownActive() {
        return isCountdownActive.get();
    }

    public void startCountdown(Process currentLottery) {
        if (isCountdownActive.compareAndSet(false, true)) {  // 确保只在倒计时未开始时启动
            final int countdownTime = 5;  // 倒计时时间（秒）
            int[] remainingTime = {countdownTime};  // 使用数组包装为可变类型，方便内部修改
            countdownStartTime = System.currentTimeMillis();  // 记录倒计时开始的时间

            // 检查线程池是否已经关闭，如果已关闭，则重新创建一个新的线程池
            if (scheduler.isShutdown() || scheduler.isTerminated()) {
                scheduler = Executors.newScheduledThreadPool(1);  // 创建新的线程池
            }

            // 每秒执行一次任务，直到倒计时结束
            scheduler.scheduleAtFixedRate(() -> {
                long elapsedTime = (System.currentTimeMillis() - countdownStartTime) / 1000;  // 计算已经过去的秒数

                try {
                    // 判断是否超时
                    if (elapsedTime > 6) { // 如果倒计时已经超过6秒，则认为超时
                        isCountdownActive.set(false);
                        scheduler.shutdown();  // 关闭调度器
                        // 发送超时消息
                        webSocketServer.sendMessage(new WsMessage("倒计时超时", "倒计时已超过6秒"));
                        return;
                    }

                    if (remainingTime[0] > 0) {
                        // 发送倒计时信息
                        WsMessage wsMessage = new WsMessage("倒计时", String.valueOf(remainingTime[0]));
                        wsMessage.addExtParam("countdown", remainingTime[0]);
                        wsMessage.addExtParam("result", currentLottery);
                        webSocketServer.sendMessage(wsMessage);
                        remainingTime[0]--;
                    } else {
                        // 倒计时结束
                        isCountdownActive.set(false);
                        scheduler.shutdown();  // 关闭调度器

                        webSocketServer.sendMessage(new WsMessage("倒计时结束", "倒计时结束"));
                    }
                } catch (Exception e) {
                    // 捕获异常并重置倒计时状态
                    isCountdownActive.set(false);
                    scheduler.shutdown();  // 关闭调度器
                    webSocketServer.sendMessage(new WsMessage("倒计时异常", "倒计时过程中出现错误"));
                    e.printStackTrace();
                }

            }, 0, 1, TimeUnit.SECONDS);  // 初始延迟 0 秒，每秒执行一次

            // 发送初始的倒计时信息
            webSocketServer.sendMessage(new WsMessage("倒计时开始", String.valueOf(countdownTime)));
        }
    }
}
