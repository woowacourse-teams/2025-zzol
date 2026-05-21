package coffeeshout.minigame.domain;

public class MiniGameTask implements Runnable {

    private final Runnable task;

    public MiniGameTask(Runnable task) {
        this.task = task;
    }

    @Override
    public void run() {
        task.run();
    }
}
