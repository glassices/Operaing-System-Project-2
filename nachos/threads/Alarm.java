//u`ofe`{i`nlhofgdhbi`nsthvtmhg`o
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;
import java.util.Comparator;
import java.util.PriorityQueue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
    	Machine.timer().setInterruptHandler(new Runnable() {
    		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	WaitingType w = waitingQueue.peek();

    	while (w != null && Machine.timer().getTime() >= w.wakeupTime) {
    		Semaphore sem = w.sem;
    			
    		lock.acquire();
    		waitingQueue.poll();
    		w = waitingQueue.peek();
    		lock.release();
    			
    		sem.V();
    	}
    	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
    	long wakeTime = Machine.timer().getTime() + x;
    	WaitingType w = new WaitingType(wakeTime , new Semaphore(0));
    	
    	lock.acquire();
    	waitingQueue.add(w);
    	lock.release();
    	
    	w.sem.P();
    }
    
    public class WaitingType {
    	long wakeupTime;
    	Semaphore sem;
    	
    	WaitingType(long wakeupTime , Semaphore sem) {
    		this.wakeupTime = wakeupTime;
    		this.sem = sem;
    	}
    }
    
    private Lock lock = new Lock();
    
    private PriorityQueue<WaitingType> waitingQueue =
    		new PriorityQueue<WaitingType>(1 , new Comparator() {
    			@Override
    			public int compare(Object o1 , Object o2) {
    				WaitingType w1 = (WaitingType)o1;
    				WaitingType w2 = (WaitingType)o2;
    				if (w1.wakeupTime < w2.wakeupTime) 
    					return -1;
    				else if (w1.wakeupTime > w2.wakeupTime)
    					return 1;
    				else 
    					return 0;
    			}
    		});
}
