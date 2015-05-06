package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
	if (emptyPageList == null){
		numEmptyPages = Machine.processor().getNumPhysPages();
		emptyPageList = new PageBlock(-1, 0);
		emptyPageList.next = new PageBlock(0, numEmptyPages);
		emptyPageList.next.next = new PageBlock(numEmptyPages + 1, 0);
	}
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	console = new SynchConsole(Machine.console());
	
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
	
	// Test memory allocation
	/*
	System.out.println("Testing memory allocation");
	MemoryTest.selfTest();
	*/
	
	// Test scheduler
	/*
	System.out.println("Testing lottery scheduler");
	new LotteryScheduler().selfTest();
	*/
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();	
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }
    
    public class PageBlock{
    	public PageBlock(int position, int size){
    		this.position = position;
    		this.size = size;
    	}
    	
    	public PageBlock free(PageBlock other){
    		if (other.position < position)
    			return null;
    		PageBlock t = this;
    		while (t != null){
	    		if (other.position == t.position + t.size){
	    			numEmptyPages += other.size;
	    			t.size += other.size;
					if (t.position + t.size == t.next.position){
						t.size += t.next.size;
						t.next = t.next.next;
					}
					return t;
	    		}
	    		else if (other.position + other.size < t.next.position){
	    			numEmptyPages += other.size;
	    			other.next = t.next;
	    			t.next = other;
	    			return other;
	    		}
	    		else if (other.position + other.size == t.next.position){
	    			numEmptyPages += other.size;
	    			t.next.position -= other.size;
	    			t.next.size += other.size;
	    			return t.next;
	    		}
	    		else
	    			t = t.next;
    		}
    		return null;
    	}
    	
    	public PageBlock allocate(int size){
    		PageBlock ret;
    		if (next.size <= size){
    			numEmptyPages -= next.size;
    			ret = next;
    			next = next.next;
    			//
    			//System.out.println("Pages allocated: [" + ret.position + ", " + (ret.position + ret.size - 1) + "]");
    			return ret;
    		}
    		else{
    			numEmptyPages -= size;
    			ret = new PageBlock(next.position, size);
    			next.position += size;
    			next.size -= size;
    			//
    			//System.out.println("Pages allocated: [" + ret.position + ", " + (ret.position + ret.size - 1) + "]");
    			return ret;
    		}
    	}
    	
    	int position;
    	int size;
    	PageBlock next = null;
    }
    
    static PageBlock emptyPageList = null;
    static int numEmptyPages;

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}
