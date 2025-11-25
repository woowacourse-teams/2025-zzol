package coffeeshout.global.config;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

// 프로그램이 종료되면 진행중인 태스크도 종료되는 스케줄러
public class ShutDownTestScheduler extends ThreadPoolTaskScheduler {

    public ShutDownTestScheduler() {
        this.setPoolSize(2);
        this.setThreadNamePrefix("shut-down-test-scheduler-");
        this.setDaemon(false);
        this.setWaitForTasksToCompleteOnShutdown(false);
        this.setAwaitTerminationSeconds(10);
        this.initialize();
    }
}
