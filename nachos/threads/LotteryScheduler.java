package nachos.threads;

import java.util.ArrayList;
import java.util.Random;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends Scheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    public void selfTest(){
    	KThread dummy[] = new KThread[12];
    	for (int i = 0; i < 12; i++)
    		dummy[i] = new KThread();
    	setPriority(dummy[0], 1);
    	for (int i = 1; i < 8; i++)
    		setPriority(dummy[i], i);
    	for (int i = 8; i < 12; i++)
    		setPriority(dummy[i], 1);
    	for (int i = 0; i < 12; i++)
    		dummy[i].setName("dummy thread " + i);
    	LotteryQueue queue0 = new LotteryQueue(true);
    	LotteryQueue queue1 = new LotteryQueue(true);
    	LotteryQueue queue2 = new LotteryQueue(true);
    	queue2.acquire(dummy[9]);
    	queue2.waitForAccess(dummy[10]);
    	queue2.waitForAccess(dummy[11]);
    	queue1.acquire(dummy[7]);
    	queue1.waitForAccess(dummy[8]);
    	queue1.waitForAccess(dummy[9]);
    	queue0.acquire(dummy[0]);
    	for (int i = 1; i < 8; i++)
    		queue0.waitForAccess(dummy[i]);
    	queue0.selfTest(10000);
    	setPriority(dummy[11], 5);
    	queue0.selfTest(10000);
    	setPriority(dummy[11], 3);
    	queue0.selfTest(10000);
    	queue0.nextThread();
    	queue0.selfTest(10000);
    	queue0.nextThread();
    	queue0.selfTest(10000);
    	queue0.nextThread();
    	queue0.selfTest(10000);
    }
    
    protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
		    thread.schedulingState = new ThreadState(thread);
		return (ThreadState) thread.schedulingState;
    }
    
    public int getPriority(KThread thread) {
    	return getThreadState(thread).priority;
    }
    
    public int getEffectivePriority(KThread thread) {
    	return getThreadState(thread).tickets;
    }
    
    public void setPriority(KThread thread, int priority) {
    	getThreadState(thread).setPriority(priority);
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new LotteryQueue(transferPriority);
    }
    
    protected class BinaryIndexedTree{
    	public BinaryIndexedTree(){
    		array = new ArrayList<ThreadState>();
    		array.add(null);
    		sum = new ArrayList<Integer>();
    		sum.add(0);
    		totalTickets = 0;
    		rand = new Random();
    	}
    	
    	public int prefixSum(int index){
    		int ret = 0;
    		while (index > 0){
    			ret += sum.get(index);
    			index &= index - 1;
    		}
    		return ret;
    	}
    	
    	public void modify(int index, int x){
    		while (index < sum.size()){
    			sum.set(index, sum.get(index) + x);
    			index += index & -index;
    		}
    		totalTickets += x;
    	}
    	
    	public void add(ThreadState state){
    		//
    		//System.out.print(totalTickets + " " + state.tickets);
    		int t = array.size();
    		array.add(state);
    		state.treeIndex = t;
    		totalTickets += state.tickets;
    		//
    		//System.out.println(" " + totalTickets);
    		sum.add(totalTickets - prefixSum(t & (t - 1)));
    		//
    		//System.out.println("Thread " + state.thread.toString() + " added to queue " + this.toString());
    	}
    	
    	public ThreadState remove(int index){
    		ThreadState ret = array.get(index);
    		//
    		//System.out.print(totalTickets + " " + array.get(index).tickets);
    		modify(index, array.get(array.size() - 1).tickets - array.get(index).tickets);
    		array.set(index, array.get(array.size() - 1));
    		array.get(index).treeIndex = index;
    		totalTickets -= array.get(array.size() - 1).tickets;
    		array.remove(array.size() - 1);
    		sum.remove(sum.size() - 1);
    		//
    		//System.out.println(" " + totalTickets);
    		//
    		//System.out.println("Thread " + ret.thread.toString() + " removed from queue " + this.toString());
    		return ret;
    	}
    	
    	public int binarySearch(int key){
    		int zl = 0;
    		int zr = sum.size() - 1;
    		while (zr - zl > 1){
    			int mid = (zl + zr) / 2;
    			if (prefixSum(mid) > key)
    				zr = mid;
    			else
    				zl = mid;
    		}
    		return zr;
    	}
    	
    	public ThreadState draw(){
    		if (array.size() == 1)
    			return null;
    		else
    			return remove(binarySearch(rand.nextInt(totalTickets)));
    	}
    	
    	public int peek(){
    		if (array.size() == 1)
    			return -1;
    		else
    			return binarySearch(rand.nextInt(totalTickets));
    	}
    	
    	ArrayList<ThreadState> array = null;
    	ArrayList<Integer> sum = null;
    	int totalTickets;
    	Random rand = null;
    }
    
    protected class ThreadState{
    	public ThreadState(KThread thread){
    		this.thread = thread;
    		priority = priorityDefault;
    		tickets = priority;
    	}
    	
    	public void gainTickets(int tickets){
    		//
    		//System.out.println(thread.toString() + ": " + tickets + " tickets gained " + this.tickets + " -> " + (this.tickets + tickets));
    		this.tickets += tickets;
    		if (waitQueue != null){
    			waitQueue.tree.modify(treeIndex, tickets);
    			if (waitQueue.transferPriority)
    				if (waitQueue.threadHoldingResource != null)
    					waitQueue.threadHoldingResource.gainTickets(tickets);
    		}
    	}
    	
    	public void setPriority(int priority){
    		if ((priority > priorityMaximum) || (priority < priorityMinimum) || (priority == this.priority))
    			return;
    		//
    		//System.out.println(thread.toString() + ": priority changed " + this.priority + " -> " + priority);
    		gainTickets(priority - this.priority);
    		this.priority = priority;
    	}
    	
    	public boolean increasePriority(){
    		if (priority == priorityMaximum)
    			return false;
    		setPriority(priority + 1);
    		return true;
    	}
    	
    	public boolean decreasePriority(){
    		if (priority == priorityMinimum)
    			return false;
    		setPriority(priority - 1);
    		return true;
    	}
    	
    	LotteryQueue waitQueue = null;
    	KThread thread = null;
    	int priority;
    	int tickets;
    	int treeIndex;
    }
    
    protected class LotteryQueue extends ThreadQueue{
    	public LotteryQueue(boolean transferPriority){
    		this.transferPriority = transferPriority;
    		tree = new BinaryIndexedTree();
    	}
    	
        public void waitForAccess(KThread thread){
        	ThreadState state = getThreadState(thread);
        	tree.add(state);
        	state.waitQueue = this;
        	if (transferPriority)
        		if (threadHoldingResource != null){
        			threadHoldingResource.gainTickets(state.tickets);
        		}
        }
        
        public KThread nextThread(){
        	if (transferPriority)
        		if (threadHoldingResource != null){
        			threadHoldingResource.gainTickets(-tree.totalTickets);
        			threadHoldingResource = null;
        		}
        	ThreadState state = tree.draw();
        	if (state == null)
        		return null;
        	else{
        		state.waitQueue = null;
        		return state.thread;
        	}
        }

        public void acquire(KThread thread){
        	threadHoldingResource = getThreadState(thread);
        	if (transferPriority)
        		threadHoldingResource.gainTickets(tree.totalTickets);
        }
        
        public void print(){
        }
        
        public void selfTest(int times){
        	int n = tree.array.size();
        	if (n == 1)
        		return;
        	int stat[] = new int[n];
        	for (int i = 1; i < n; i++)
        		stat[i] = 0;
        	for (int i = 0; i < times; i++)
        		stat[tree.peek()]++;
        	System.out.println("Peeked " + times + " times");
        	System.out.println("Total tickets: " + tree.totalTickets);
        	for (int i = 1; i < n; i++)
        		System.out.println(tree.array.get(i).thread.toString() + ": have " + tree.array.get(i).tickets + " tickets, peeked " + stat[i] + " times, " + (times * 1.0 * tree.array.get(i).tickets / tree.totalTickets) + " times expected");
        }
        
    	BinaryIndexedTree tree = null;
    	ThreadState threadHoldingResource = null;
    	boolean transferPriority;
    }

    public static final int priorityDefault = 1;
    public static final int priorityMinimum = 1;
    public static final int priorityMaximum = 7;
}
