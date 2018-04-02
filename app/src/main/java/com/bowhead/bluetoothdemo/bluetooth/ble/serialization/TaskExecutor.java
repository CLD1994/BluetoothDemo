package com.bowhead.bluetoothdemo.bluetooth.ble.serialization;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by cld.
 */
public class TaskExecutor {

    private final BlockingQueue<Task> mBlockingQueue = new LinkedBlockingQueue<>();

    private Thread mThread;

    public TaskExecutor() {
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Task task = mBlockingQueue.take();
                        QueueSemaphore semaphore = new QueueSemaphore();
                        task.run(semaphore);
                        semaphore.awaitRelease();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });

        mThread.start();
    }

    public void close() {
        mThread.interrupt();
    }

    public void cleanTask(){
        mBlockingQueue.clear();
    }

    public boolean addTask(Task task) {
        return mBlockingQueue.add(task);
    }

    public boolean removeTask(Task task){
        return mBlockingQueue.remove(task);
    }
}
