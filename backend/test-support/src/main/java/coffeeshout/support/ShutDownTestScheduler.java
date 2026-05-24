package coffeeshout.support;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class ShutDownTestScheduler extends ThreadPoolTaskScheduler {

    public ShutDownTestScheduler() {
        this.setPoolSize(2);
        this.setThreadNamePrefix("shut-down-test-scheduler-");
        this.setDaemon(false);
        this.setWaitForTasksToCompleteOnShutdown(false);
        this.setAwaitTerminationSeconds(10);
        this.setPhase(Integer.MAX_VALUE - 1);
        this.initialize();
    }
}
