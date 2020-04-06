import sun.misc.Unsafe;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * @author zhangdididi
 */
public class Aqs {
    
    /**
     * 记录当前加锁的次数
     */
    private volatile int state = 0;
    /**
     * 当前持有锁的线程
     */
    private Thread lockHolder;
    /**
     * 线程安全的队列---基于CAS算法
     */
    private static ConcurrentLinkedQueue<Thread> waiters = new ConcurrentLinkedQueue<Thread>();

    public int getState() {
        return state;
    }

    public void setLockHolder(Thread lockHolder) {
        this.lockHolder = lockHolder;
    }

    /**
     * 尝试获取锁
     */
    public boolean aquire() {
        Thread current = Thread.currentThread();
        //当前加锁的状态
        int c = getState();
        if (c == 0) {
            //当前同步器还没有被持有
            boolean canUse = waiters.isEmpty() || current == waiters.peek();
            if (canUse && compareAndSetState(0, 1)) {
                //修改成功的线程，设置为持有者
                setLockHolder(current);
                return true;
            }
        }
        return false;
    }

    /**
     * 释放锁
     */
    public void lock() {
        //加锁成功
        if (aquire()) {
            return;
        }
        //没有加锁成功

        //cas比较与交换（原子算法）
        Thread current = Thread.currentThread();
        waiters.add(current);
        for (;;){
            //确保没有获取到锁的线程不再执行下面代码/占有CPU

            if (aquire()) {
                waiters.poll();//唤醒队头线程的话，就需要从队列中移除该线程
                return;
            }

            LockSupport.park();//阻塞--释放CPU使用权  -->RSS
        }
    }

    /**
     * 释放锁
     */
    public void unloclk() {
        if (lockHolder != Thread.currentThread()) {
            //抛出异常
            throw new RuntimeException("lockholder is not current thread");
        }
        int state = getState();
        if (compareAndSetState(state, 0)) {
            setLockHolder(null);
            Thread first = waiters.peek();
            if (first != null) {
                //就需要唤醒队首线程
                LockSupport.unpark(first);
            }
        }
    }



    public final boolean compareAndSetState(int expect, int updata) {
        return UNSAFE.compareAndSwapInt(this, STATEOFFSET, expect, updata);
    }

    /**
     *  通过反射获取一个Unsafe实例对象
     */
    private static final Unsafe UNSAFE = UnsafeIntance.reflectGetUnsafe();

    /**
     * 偏移量
     */
    private static final long STATEOFFSET;

    static {
        try {
            STATEOFFSET = UNSAFE.objectFieldOffset(Aqs.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error();
        }
    }
}
