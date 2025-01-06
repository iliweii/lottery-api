package top.lucki.lottery.system.service;


import org.springframework.stereotype.Service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class LotteryService {

    private final AtomicBoolean isCountdownActive = new AtomicBoolean(false);

    public boolean isCountdownActive() {
        return isCountdownActive.get();
    }

    public void startCountdown() {
        isCountdownActive.set(true);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                isCountdownActive.set(false);
            }
        }, 5000);
    }
}