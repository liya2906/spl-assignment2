package scheduling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorTests {

    @Test
    @Timeout(5)
    void testSubmitAllExecutesAllTasks() {
        TiredExecutor executor = new TiredExecutor(4);
        AtomicInteger counter = new AtomicInteger(0);

        Runnable task = counter::incrementAndGet;

        executor.submitAll(java.util.List.of(task, task, task, task, task));
        assertEquals(5, counter.get());
    }

    @Test
    @Timeout(5)
    void testExecutorConcurrency() {
        TiredExecutor executor = new TiredExecutor(8);
        AtomicInteger counter = new AtomicInteger(0);

        int n = 100;
        java.util.List<Runnable> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            tasks.add(counter::incrementAndGet);
        }

        executor.submitAll(tasks);
        assertEquals(n, counter.get());
    }

    @Test
    @Timeout(5)
    void testShutdownWaitsForTasks() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(2);
        AtomicInteger counter = new AtomicInteger(0);

        Runnable slow = () -> {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            counter.incrementAndGet();
        };

        executor.submitAll(java.util.List.of(slow, slow, slow));
        executor.shutdown();

        assertEquals(3, counter.get());
    }
}
