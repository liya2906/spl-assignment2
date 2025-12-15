package scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TiredThread extends Thread implements Comparable<TiredThread> {

    private static final Runnable POISON_PILL = () -> {}; // Special task to signal shutdown

    private final int id; // Worker index assigned by the executor
    private final double fatigueFactor; // Multiplier for fatigue calculation

    private final AtomicBoolean alive = new AtomicBoolean(true); // Indicates if the worker should keep running

    // Single-slot handoff queue; executor will put tasks here
    private final BlockingQueue<Runnable> handoff = new ArrayBlockingQueue<>(1);

    private final AtomicBoolean busy = new AtomicBoolean(false); // Indicates if the worker is currently executing a task

    private final AtomicLong timeUsed = new AtomicLong(0); // Total time spent executing tasks
    private final AtomicLong timeIdle = new AtomicLong(0); // Total time spent idle
    private final AtomicLong idleStartTime = new AtomicLong(0); // Timestamp when the worker became idle

    public TiredThread(int id, double fatigueFactor) {
        this.id = id;
        this.fatigueFactor = fatigueFactor;
        this.idleStartTime.set(System.nanoTime());
        setName(String.format("FF=%.2f", fatigueFactor));
    }

    public int getWorkerId() {
        return id;
    }

    public double getFatigue() {
        return fatigueFactor * timeUsed.get();
    }

    public boolean isBusy() {
        return busy.get();
    }

    public long getTimeUsed() {
        return timeUsed.get();
    }

    public long getTimeIdle() {
        return timeIdle.get();
    }

    /**
     * Assign a task to this worker.
     * This method is non-blocking: if the worker is not ready to accept a task,
     * it throws IllegalStateException.
     */
    public void newTask(Runnable task) {
        if ( !alive.get() || !handoff.offer(task) )
            throw new IllegalStateException("worker is unavailable");
    }

    /**
     * Request this worker to stop after finishing current task.
     * Inserts a poison pill so the worker wakes up and exits.
     */
    public void shutdown() {
        alive.set(false);
        handoff.offer(POISON_PILL);
    }

    @Override
    public void run() {
        try {
            this.idleStartTime.set(System.nanoTime());
            while (true) {

                // calculating Idle time (because take() blocks the thread until there is a task)
                Runnable task = handoff.take();
                long endTime = System.nanoTime();
                this.timeIdle.addAndGet( endTime - this.idleStartTime.get());

                // first condition - checks if shutdown was made while waiting at take()
                if (!alive.get() || task == POISON_PILL)
                    break;

                // counting used time
                busy.set(true);
                long startTime = System.nanoTime();

                // with try{},finally{} we are making sure that even if run() throws exception,
                // the TiredThread fields will remain valid
                try {
                    task.run();
                }
                finally {
                    busy.set(false);
                    endTime =System.nanoTime();
                    this.timeUsed.addAndGet(endTime - startTime);
                    this.idleStartTime.set(endTime);
                }
            }
        }
        // Interrupted while blocked on take(), stop waiting and exit
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
            return;
        }
    }

    @Override
    public int compareTo(TiredThread o) {
        return Double.compare(this.getFatigue(), o.getFatigue());
    }
}