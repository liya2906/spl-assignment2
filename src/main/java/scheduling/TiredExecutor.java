package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {

        if (numThreads<=0)
            throw new IllegalArgumentException("cannot initialize TiredExecutor - num of threads <=0 ");

        workers=new TiredThread[numThreads];
        for (int i=0 ; i<numThreads; i++){
            workers[i]= new TiredThread( i , 0.5 + Math.random());
            idleMinHeap.add(workers[i]);
            // Start the worker thread; it enters run() and blocks on handoff.take(),
            // until the first task is assigned
            workers[i].start();
        }

    }


    //TODO: CHECK THE INFLIGHT UPDATE!!!!!
    public void submit(Runnable task) {

        while (true){

            TiredThread curr=null;

            try {

                curr = idleMinHeap.take(); // first possible exception

                TiredThread copy = curr;
                inFlight.addAndGet(1); //CHECK THE INFLIGHT UPDATE!!!!!
                // wrap the task so we can maintain the inFlight and idleMinHeap fields
                Runnable wrappedTask = () -> {
                    try {
                        task.run();
                    } finally {
                        // task finished, worker became idle again
                        idleMinHeap.add(copy);

                        // notify all threads that are waiting if all tasks completed
                        if (inFlight.decrementAndGet() == 0) {
                            synchronized (this) {
                                this.notifyAll();
                            }
                        }
                    }
                };

                curr.newTask(wrappedTask);// second possible exception
                return; // SUCCESS - exit submit
            }

            // Interrupted while blocked on take(), stop waiting and exit
            catch(InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // if the worker was unavailable; return it to the pool and retry
            catch (IllegalStateException e) {
                inFlight.decrementAndGet();
                if (curr != null && curr.isAlive())
                    idleMinHeap.add(curr);
            }
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        //  submit tasks one by one and wait until all finish
        for (Runnable task : tasks) {
            submit(task);
        }
        synchronized (this) {
            while (inFlight.get() > 0) {
                try{
                    // waiting until inFlight= 0 (notified by submit)
                    this.wait();
                }
                catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        for (int i = 0 ; i<workers.length; i++){
            workers[i].shutdown();
        }
        // waiting until the thread are shutdown for real
        for (int i = 0 ; i<workers.length; i++){
            workers[i].join();
        }
        idleMinHeap.clear();
        inFlight.set(0);
    }

    public synchronized String getWorkerReport() {
        //  return readable statistics for each worker
            StringBuilder sb = new StringBuilder();

            sb.append("=============== WORKER REPORT ===============\n");

            for (TiredThread w : workers) {
                sb.append("Worker #").append(w.getWorkerId())
                        .append(" | Busy: ").append(w.isBusy())
                        .append(" | Fatigue: ").append(w.getFatigue())
                        .append(" | Work Time: ").append(w.getTimeUsed() / 1_000_000.0).append(" ms")
                        .append(" | Idle Time: ").append(w.getTimeIdle() / 1_000_000.0).append(" ms")
                        .append("\n");
            }

            sb.append("============================================\n");
            return sb.toString();
        }
    }

