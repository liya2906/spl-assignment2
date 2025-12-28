package scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class TiredExecutorTest {

    // Tests for Exception Handling During Task Execution

    @Test
    void testTaskExecution_ExceptionInTask_WorkerStaysAlive() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        AtomicBoolean secondTaskExecuted = new AtomicBoolean(false);

        CountDownLatch firstTaskStarted = new CountDownLatch(1);
        CountDownLatch firstTaskFinished = new CountDownLatch(1);

        worker.start();

        // First task throws exception
        worker.newTask(() -> {
            firstTaskStarted.countDown();
            try {
                throw new RuntimeException("Task failed");
            } finally {
                firstTaskFinished.countDown();
            }
        });

        // Wait until first task actually started
        firstTaskStarted.await();

        // Wait until it finished (even with exception)
        firstTaskFinished.await();

        // Now it is safe to submit another task
        worker.newTask(() -> secondTaskExecuted.set(true));

        // Give time for second task to execute
        Thread.sleep(50);

        assertTrue(
                secondTaskExecuted.get(),
                "Worker should continue running after a task throws an exception");

        worker.shutdown();
        worker.join();
    }

    @Test
    void testTaskExecution_ExceptionInTask_BusyFlagCleared() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        worker.newTask(() -> {
            throw new RuntimeException("Task failed");
        });
        Thread.sleep(50);

        assertFalse(worker.isBusy());

        worker.shutdown();
        worker.join();
    }

    @Test
    void testTaskExecution_ExceptionInTask_TimeUsedStillIncreases() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        long initialTimeUsed = worker.getTimeUsed();
        worker.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Task failed");
        });
        Thread.sleep(100);

        assertTrue(worker.getTimeUsed() > initialTimeUsed);

        worker.shutdown();
        worker.join();
    }

    // Tests for newTask() Edge Cases

    @Test
    void testNewTask_WhileBusy_DoesNotRunInParallel() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        AtomicBoolean firstRunning = new AtomicBoolean(false);
        AtomicBoolean secondStartedWhileFirst = new AtomicBoolean(false);

        worker.start();

        worker.newTask(() -> {
            firstRunning.set(true);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            firstRunning.set(false);
        });

        Thread.sleep(20);

        worker.newTask(() -> {
            if (firstRunning.get()) {
                secondStartedWhileFirst.set(true);
            }
        });

        Thread.sleep(200);

        assertFalse(secondStartedWhileFirst.get(),
                "Second task should not start while first is running");

        worker.shutdown();
        worker.join();
    }

    @Test
    void testNewTask_ImmediatelyAfterTask_Succeeds() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        AtomicBoolean firstExecuted = new AtomicBoolean(false);
        AtomicBoolean secondExecuted = new AtomicBoolean(false);

        worker.start();
        worker.newTask(() -> firstExecuted.set(true));
        Thread.sleep(50);
        worker.newTask(() -> secondExecuted.set(true));
        Thread.sleep(50);

        assertTrue(firstExecuted.get());
        assertTrue(secondExecuted.get());

        worker.shutdown();
        worker.join();
    }

    // Tests for Boundary Conditions

    @Test
    void testTaskExecution_ZeroSleepTask() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        AtomicBoolean executed = new AtomicBoolean(false);

        worker.start();
        worker.newTask(() -> executed.set(true));
        Thread.sleep(50);

        assertTrue(executed.get());
        assertTrue(worker.getTimeUsed() >= 0);

        worker.shutdown();
        worker.join();
    }

    @Test
    void testTimeTracking_VeryShortTask() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        worker.newTask(() -> {
            int sum = 0;
            for (int i = 0; i < 100; i++) {
                sum += i;
            }
        });
        Thread.sleep(50);

        assertTrue(worker.getTimeUsed() >= 0);

        worker.shutdown();
        worker.join();
    }

    // Tests for Fatigue Factor Variations

    @Test
    void testFatigue_ZeroFatigueFactor() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 0.0);

        worker.start();
        worker.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(100);

        assertEquals(0.0, worker.getFatigue(), 0.001);

        worker.shutdown();
        worker.join();
    }

    @Test
    void testFatigue_VeryHighFatigueFactor() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 100.0);

        worker.start();
        worker.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(100);

        double expectedFatigue = 100.0 * worker.getTimeUsed();
        assertEquals(expectedFatigue, worker.getFatigue(), 0.001);

        worker.shutdown();
        worker.join();
    }

    // Tests for Shutdown Edge Cases

    @Test
    void testShutdown_DuringTaskExecution_WaitsForCompletion() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        AtomicBoolean taskCompleted = new AtomicBoolean(false);

        worker.start();
        worker.newTask(() -> {
            try {
                Thread.sleep(100);
                taskCompleted.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(20);
        worker.shutdown();
        worker.join(1000);

        assertTrue(taskCompleted.get());
        assertFalse(worker.isAlive());
    }

    // Tests for State Consistency

    @Test
    void testStateConsistency_AfterMultipleTasks() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 2.0);

        worker.start();
        for (int i = 0; i < 5; i++) {
            worker.newTask(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            Thread.sleep(30);
        }

        assertFalse(worker.isBusy());
        assertTrue(worker.getTimeUsed() > 0);
        assertTrue(worker.getTimeIdle() > 0);
        assertEquals(2.0 * worker.getTimeUsed(), worker.getFatigue(), 0.001);

        worker.shutdown();
        worker.join();
    }

    @Test
    void testCompareTo_AfterNoWork() {
        TiredThread worker1 = new TiredThread(1, 1.0);
        TiredThread worker2 = new TiredThread(2, 2.0);

        assertEquals(0, worker1.compareTo(worker2));
    }

    @Test
    void testGetWorkerId_UniqueIds() {
        TiredThread worker1 = new TiredThread(5, 1.0);
        TiredThread worker2 = new TiredThread(10, 1.0);

        assertEquals(5, worker1.getWorkerId());
        assertEquals(10, worker2.getWorkerId());
    }
}
