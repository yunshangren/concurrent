import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class Test01 {
    static ExecutorService cookerPool = Executors.newFixedThreadPool(2, new ThreadFactory() {
        AtomicInteger i = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
           return new Thread(r,"cooker-" + i.getAndIncrement());
        }
    });
    static ExecutorService waiterPool = Executors.newFixedThreadPool(5, new ThreadFactory() {
        AtomicInteger i = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r,"waiter-" + i.getAndIncrement());
        }
    });
    static ReentrantLock lock = new ReentrantLock();
    static Condition noTask = lock.newCondition();
    static Condition fullTask = lock.newCondition();
    // 订单编号
    static AtomicInteger order = new AtomicInteger(1);
    static final int capacity = 20;
    // 任务队列
    static Deque<Task> tasks = new ArrayDeque<>();

    public static void main(String[] args) {
        while (true){
            waiterPool.execute(() -> {
                order();
            });
            cookerPool.execute(() -> {
                cook();
            });
        }

    }

    public static void order(){
        lock.lock();
        try{
            while (tasks.size() == capacity){
                log.debug("tasks full");
                try {
                    fullTask.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Task task = new Task(order.getAndIncrement());
            log.debug("task {} commit",task.getId());
            tasks.addLast(task);
            noTask.signalAll();
        }finally {
            lock.unlock();
        }
    }

    public static void cook(){
        lock.lock();
        try{
            while (tasks.size() == 0){
                log.debug("no task");
                noTask.await();
            }
            Thread.sleep(1000);
            log.debug("task {} done",tasks.removeFirst().getId());
            fullTask.signalAll();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}
class Task{
    private int id;

    public Task(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
