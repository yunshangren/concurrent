import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class Restaurant {
    static ExecutorService cookers =  Executors.newFixedThreadPool(2);
    static ThreadPoolExecutor waiters = (ThreadPoolExecutor)Executors.newFixedThreadPool(3);
    // 负责产生顾客的线程
    static ExecutorService customerIN = Executors.newSingleThreadExecutor();
    static ExecutorService customerOut = Executors.newFixedThreadPool(5);

    static ReentrantLock roomLock = new ReentrantLock();
    Condition noCus = roomLock.newCondition();
    Condition fullCus = roomLock.newCondition();
    static final int capacity = 20;
    static final Object busyLock = new Object();
    // 订单编号
    static AtomicInteger order = new AtomicInteger(1);
    // 订单队列
    static Deque<String> tasks = new ArrayDeque<>();

    // 顾客编号
    static AtomicInteger id = new AtomicInteger(1);
    // 顾客集合
    private static List<Customer> customers = new ArrayList<>(20);

    public static void main(String[] args) throws InterruptedException {
        Restaurant restaurant = new Restaurant();
        customers = Collections.synchronizedList(customers);

        customerIN.execute(() -> {
            while(true){
                try {
                    Thread.sleep(120);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                restaurant.enter(new Customer(id.getAndIncrement()));
            }
        });
        Thread.sleep(500);
        while (true){
            customerOut.execute(() -> {
                    Customer customer = null;
                    if(customers.size() > 0) {
                        customer = customers.get(new Random().nextInt(customers.size()));

                    }
                    try {
                        Thread.sleep(120);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    restaurant.out(customer);

            });
        }



        // 顾客在集合里面是无序的，不能通过customers.get()的方式获取
        /*Thread.sleep(100);
        restaurant.out(customers.get(19));
        Thread.sleep(100);
        restaurant.out(customers.get(15));
        Thread.sleep(100);
        restaurant.out(customers.get(9));*/


    }
    // 获取店内顾客人数
    public static int getCapacity(){
        return customers.size();
    }

    public void enter(Customer customer){
        roomLock.lock();
        try{
            if(getCapacity() == capacity){
                log.debug("The restaurant is full");
                // 成功进入id才+1
                id.getAndDecrement();
                try {
                    fullCus.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                log.debug("{} enters",customer.toString());
                customers.add(customer);
                noCus.signalAll();
            }
        }finally {
            roomLock.unlock();
        }

    }
    public void out(Customer customer){
       roomLock.lock();
       try{
           if(customer == null){
               log.debug("The restaurant is empty");
               noCus.await();
           }else{
               synchronized (busyLock){
                   while(waiters.getActiveCount() == waiters.getCorePoolSize()){
                       log.debug("all waiters are busy now");
                       try {
                           busyLock.wait();
                       } catch (InterruptedException e) {
                           e.printStackTrace();
                       }
                   }
               }
               log.debug("{} paying",customer.toString());
               Thread.sleep(1000);
               log.debug("{} outs",customer.toString());
               customers.remove(customer);
               fullCus.signal();
           }
       } catch (InterruptedException e) {
           e.printStackTrace();
       } finally {
           roomLock.unlock();
       }



    }
}
class Customer{
    private String name;
    private int id;

    public Customer(int id) {
        this.name = "C";
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return name + "" + id;
    }
}
