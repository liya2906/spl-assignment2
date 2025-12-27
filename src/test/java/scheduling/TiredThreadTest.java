package scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class TiredThreadTest {

    // Tests for Construction & Initialization

    @Test
    void testConstructor_CreatesWorkerWithCorrectId() {
        TiredThread worker = new TiredThread(5, 1.0);

        assertEquals(5, worker.getWorkerId());
    }

    @Test
    void testConstructor_InitialStateNotBusy() {
        TiredThread worker = new TiredThread(1, 1.0);

        assertFalse(worker.isBusy());
    }

    @Test
    void testConstructor_InitialTimeUsedIsZero() {
        TiredThread worker = new TiredThread(1, 1.0);

        assertEquals(0, worker.getTimeUsed());
    }

    @Test
    void testConstructor_InitialTimeIdleIsNonNegative() {
        TiredThread worker = new TiredThread(1, 1.0);

        assertTrue(worker.getTimeIdle() >= 0);
    }

    @Test
    void testConstructor_InitialFatigueIsZero() {
        TiredThread worker = new TiredThread(1, 2.5);

        assertEquals(0.0, worker.getFatigue(), 0.001);
    }

    @Test
    void testConstructor_ThreadNameContainsFatigueFactor() {
        TiredThread worker = new TiredThread(1, 2.5);

        assertTrue(worker.getName().contains("2.50"));
    }

    @Test
    void testConstructor_DifferentFatigueFactors() {
        TiredThread worker1 = new TiredThread(1, 1.0);
        TiredThread worker2 = new TiredThread(2, 3.5);

        assertTrue(worker1.getName().contains("1.00"));
        assertTrue(worker2.getName().contains("3.50"));
    }

    // Tests for Task Execution Behavior

    @Test
    void testTaskExecution_RunnableIsExecuted() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        AtomicBoolean executed = new AtomicBoolean(false);

        worker.start();
        worker.newTask(() -> executed.set(true));
        Thread.sleep(50);
        worker.shutdown();
        worker.join();

        assertTrue(executed.get());
    }

    @Test
    void testTaskExecution_IsBusyDuringExecution() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        AtomicBoolean wasBusy = new AtomicBoolean(false);

        worker.start();
        worker.newTask(() -> {
            wasBusy.set(worker.isBusy());
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(50);
        worker.shutdown();
        worker.join();

        assertTrue(wasBusy.get());
    }

    @Test
    void testTaskExecution_NotBusyAfterCompletion() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        worker.newTask(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(100);

        assertFalse(worker.isBusy());

        worker.shutdown();
        worker.join();
    }

    @Test
    void testTaskExecution_TimeUsedIncreasesAfterTask() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        worker.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(100);

        assertTrue(worker.getTimeUsed() > 0);

        worker.shutdown();
        worker.join();
    }


    @Test
    void testTaskExecution_MultipleTasksExecutedSequentially() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        AtomicBoolean task1Executed = new AtomicBoolean(false);
        AtomicBoolean task2Executed = new AtomicBoolean(false);

        worker.start();
        worker.newTask(() -> task1Executed.set(true));
        Thread.sleep(50);
        worker.newTask(() -> task2Executed.set(true));
        Thread.sleep(50);
        worker.shutdown();
        worker.join();

        assertTrue(task1Executed.get());
        assertTrue(task2Executed.get());
    }

    // Tests for newTask() Validation

    @Test
    void testNewTask_NullTask_ThrowsException() {
        TiredThread worker = new TiredThread(1, 1.0);

        assertThrows(IllegalArgumentException.class, () -> {
            worker.newTask(null);
        });
    }

    @Test
    void testNewTask_WorkerAlreadyHasTask_ThrowsException() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        worker.newTask(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertThrows(IllegalStateException.class, () -> {
            worker.newTask(() -> {
            });
        });

        worker.shutdown();
        worker.join();
    }

    @Test
    void testNewTask_AfterShutdown_ThrowsException() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        worker.shutdown();
        worker.join();

        assertThrows(IllegalStateException.class, () -> {
            worker.newTask(() -> {
            });
        });
    }

    // Tests for shutdown() Behavior

    @Test
    void testShutdown_StopsWorkerThread() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        worker.shutdown();
        worker.join(1000);

        assertFalse(worker.isAlive());
    }

    @Test
    void testShutdown_IsIdempotent() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        worker.shutdown();
        worker.shutdown();
        worker.join(1000);

        assertFalse(worker.isAlive());
    }

    @Test
    void testShutdown_WakesBlockedThread() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        Thread.sleep(50);
        worker.shutdown();
        worker.join(1000);

        assertFalse(worker.isAlive());
    }

    @Test
    void testShutdown_WorkerExitsCleanly() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        worker.newTask(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(50);
        worker.shutdown();
        worker.join(1000);

        assertFalse(worker.isAlive());
    }

    @Test
    void testShutdown_BeforeStart_DoesNotCrash() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.shutdown();
        worker.start();
        worker.join(1000);

        assertFalse(worker.isAlive());
    }

    // Tests for Time Tracking

    @Test
    void testTimeTracking_TimeUsedIncreasesAfterTask() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        long initialTimeUsed = worker.getTimeUsed();
        worker.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(100);

        assertTrue(worker.getTimeUsed() > initialTimeUsed);

        worker.shutdown();
        worker.join();
    }

    @Test
    void testTimeTracking_FatigueCalculation() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 2.0);

        worker.start();
        worker.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(100);

        double expectedFatigue = 2.0 * worker.getTimeUsed();
        assertEquals(expectedFatigue, worker.getFatigue(), 0.001);

        worker.shutdown();
        worker.join();
    }

    @Test
    void testTimeTracking_FatigueWithDifferentFactors() throws InterruptedException {
        TiredThread worker1 = new TiredThread(1, 1.0);
        TiredThread worker2 = new TiredThread(2, 3.0);

        worker1.start();
        worker2.start();

        worker1.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        worker2.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(100);

        assertTrue(worker2.getFatigue() > worker1.getFatigue());

        worker1.shutdown();
        worker2.shutdown();
        worker1.join();
        worker2.join();
    }

    // Tests for compareTo()

    @Test
    void testCompareTo_HigherFatigueIsGreater() throws InterruptedException {
        TiredThread worker1 = new TiredThread(1, 1.0);
        TiredThread worker2 = new TiredThread(2, 2.0);

        worker1.start();
        worker2.start();

        worker1.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        worker2.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(100);

        assertTrue(worker2.compareTo(worker1) > 0);

        worker1.shutdown();
        worker2.shutdown();
        worker1.join();
        worker2.join();
    }

    @Test
    void testCompareTo_EqualFatigueIsEqual() {
        TiredThread worker1 = new TiredThread(1, 1.0);
        TiredThread worker2 = new TiredThread(2, 1.0);

        assertEquals(0, worker1.compareTo(worker2));
    }

    @Test
    void testCompareTo_LowerFatigueIsLess() throws InterruptedException {
        TiredThread worker1 = new TiredThread(1, 1.0);
        TiredThread worker2 = new TiredThread(2, 2.0);

        worker1.start();
        worker2.start();

        worker1.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        worker2.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(100);

        assertTrue(worker1.compareTo(worker2) < 0);

        worker1.shutdown();
        worker2.shutdown();
        worker1.join();
        worker2.join();
    }

    @Test
    void testCompareTo_SortingByFatigue() throws InterruptedException {
        TiredThread worker1 = new TiredThread(1, 3.0);
        TiredThread worker2 = new TiredThread(2, 1.0);
        TiredThread worker3 = new TiredThread(3, 2.0);

        worker1.start();
        worker2.start();
        worker3.start();

        worker1.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        worker2.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        worker3.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(100);

        java.util.List<TiredThread> workers = java.util.Arrays.asList(worker1, worker2, worker3);
        java.util.Collections.sort(workers);

        assertEquals(worker2, workers.get(0));
        assertEquals(worker3, workers.get(1));
        assertEquals(worker1, workers.get(2));

        worker1.shutdown();
        worker2.shutdown();
        worker3.shutdown();
        worker1.join();
        worker2.join();
        worker3.join();
    }

    // Tests for Concurrency Safety

    @Test
    void testConcurrency_NoOverlappingExecution() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        AtomicBoolean task1Running = new AtomicBoolean(false);
        AtomicBoolean task2Running = new AtomicBoolean(false);
        AtomicBoolean overlap = new AtomicBoolean(false);

        worker.start();
        worker.newTask(() -> {
            task1Running.set(true);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            task1Running.set(false);
        });
        Thread.sleep(20);
        worker.newTask(() -> {
            task2Running.set(true);
            if (task1Running.get()) {
                overlap.set(true);
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            task2Running.set(false);
        });
        Thread.sleep(150);

        assertFalse(overlap.get());

        worker.shutdown();
        worker.join();
    }

    @Test
    void testConcurrency_BusyFlagBehavesCorrectly() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        assertFalse(worker.isBusy());

        worker.newTask(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(20);
        assertTrue(worker.isBusy());

        Thread.sleep(60);
        assertFalse(worker.isBusy());

        worker.shutdown();
        worker.join();
    }

    // Tests for Defensive Behavior

    @Test
    void testDefensiveBehavior_TaskThrowsException_WorkerContinues() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        AtomicBoolean task2Executed = new AtomicBoolean(false);

        worker.start();
        worker.newTask(() -> {
            throw new RuntimeException("Task failed");
        });
        Thread.sleep(50);
        worker.newTask(() -> task2Executed.set(true));
        Thread.sleep(50);

        assertTrue(task2Executed.get());
        assertFalse(worker.isBusy());

        worker.shutdown();
        worker.join();
    }

    @Test
    void testDefensiveBehavior_TimeTrackingAfterException() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);

        worker.start();
        worker.newTask(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Task failed");
        });
        Thread.sleep(100);

        assertTrue(worker.getTimeUsed() > 0);
        assertFalse(worker.isBusy());

        worker.shutdown();
        worker.join();
    }

}
