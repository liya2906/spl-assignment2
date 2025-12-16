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
        // TODO: submit tasks one by one and wait until all finish
    }

    public void shutdown() throws InterruptedException {
        // TODO
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        return null;
    }
}
