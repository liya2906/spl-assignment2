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
            // Start the worker thread; it enters run() and blocks on handoff.take(),
            // until the first task is assigned
            workers[i].start();
            idleMinHeap.add(workers[i]);
        }

    }



    public void submit(Runnable task) {

        if (task == null) {
            throw new IllegalArgumentException("task cannot be null");
        }

        while (true){ //TODO: understand if we need to do this while in order to move to a different thread if the current thread got interrupted,
            //TODO: or that if the current thread got interrupted it means that all of the threads got interrupted and the while is useless


            TiredThread curr=null;

            try {
                curr = idleMinHeap.take();
            }
            // Interrupted while blocked on take(), stop waiting and exit
            catch(InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("submit interrupted", e);
            }

            TiredThread copy = curr;
            inFlight.addAndGet(1);
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

            try {
                curr.newTask(wrappedTask);
            }
            // if the worker was unavailable; return it to the pool and retry
            catch (IllegalStateException e) {

                if (curr != null && curr.isAlive())
                    idleMinHeap.add(curr);

                if (inFlight.decrementAndGet() == 0){
                    synchronized (this) {
                        this.notifyAll();
                    }
                }

                throw e;
            }

            return; // SUCCESS - exit submit

        }
    }


    public void submitAll(Iterable<Runnable> tasks) {
        //  submit tasks one by one and wait until all finish
         if (tasks== null)
            throw new IllegalArgumentException("tasks cannot be null");

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
                    return; // TODO: check if we need to throw an exception
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {

        // wait until all in-flight tasks are completed
        synchronized (this){
            while (inFlight.get() > 0){
                try {
                    this.wait();
                }
                // current thread was interrupted while waiting for shutdown conditions
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }

        for (int i = 0 ; i<workers.length; i++){
            workers[i].shutdown();
        }
        // waiting until the thread are shutdown for real
        for (int i = 0 ; i<workers.length; i++){
            workers[i].join();
        }
        idleMinHeap.clear();
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

