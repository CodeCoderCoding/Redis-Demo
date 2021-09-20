import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author supremepole
 */
public class RedissonLockDemo {
    static int fixNum=5;
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch=new CountDownLatch(fixNum);
        //默认连接地址为127.0.0.1:6379
        RedissonClient redisson= Redisson.create();
        //线上生产环境要使用sentinel或cluster方案
        ExecutorService exec= Executors.newFixedThreadPool(fixNum);
        for(int i=0;i<fixNum;i++){
            exec.submit(new TestLock("client-"+i,redisson,latch));
        }
        exec.shutdown();
        latch.await();
    }
    static class TestLock implements Runnable{
        private String name;
        RedissonClient redisson;
        private CountDownLatch latch;

        public TestLock(String name, RedissonClient redisson, CountDownLatch latch){
            this.name=name;
            this.redisson=redisson;
            this.latch=latch;
        }

        @Override
        public void run() {
            //定义锁
            RLock lock=redisson.getLock("TestLock");
            //Redisson的分布式可重入锁RLock
            try{
                if(lock.tryLock(300,30, TimeUnit.MILLISECONDS)){
                    try{
                        Thread.sleep(2*100);
                        latch.countDown();
                    }finally{
                        lock.unlock();
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
