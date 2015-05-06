//u`ofe`{i`nlhofgdhbi`nsthvtmhg`o
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	lock.acquire();
    	
    	while (activeSpeakers > 0) {
    		waitingSpeakers++;
    		speaker.sleep();
    		waitingSpeakers--;
    	}
    	
    	activeSpeakers++;
    	
    	speakingWord = word;
    	
    	if (activeListeners > 0)
    		retCond.wake();
    	else {
    		if (waitingListeners > 0)
    			listener.wake();
    		
    		retCond.sleep();
    		
    		activeSpeakers--;
    		activeListeners--;
    		
    		if (waitingSpeakers > 0)
    			speaker.wake();
    	}
    	
    	lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
    public int listen() {
    	lock.acquire();
    	
    	while (activeListeners > 0) {
    		waitingListeners++;
    		listener.sleep();
    		waitingListeners--;
    	}
    	
    	activeListeners++;
    	
    	if (activeSpeakers > 0)
    		retCond.wake();
    	else {
    		if (waitingSpeakers > 0)
    			speaker.wake();
    		
    		retCond.sleep();
    		
    		activeSpeakers--;
    		activeListeners--;
    		
    		if (waitingListeners > 0)
    			listener.wake();
    	}
    	
    	int res = speakingWord;
    	
    	lock.release();
    	
    	return res;
    }
    
    private Lock lock = new Lock();
    private Condition listener = new Condition(lock);
    private Condition speaker = new Condition(lock);
    private Condition retCond = new Condition(lock);
    private int waitingListeners = 0 , waitingSpeakers = 0;
    private int activeListeners = 0 , activeSpeakers = 0;
    private Integer speakingWord = null; 
}
