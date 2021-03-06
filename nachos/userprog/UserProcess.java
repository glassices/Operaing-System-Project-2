package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.UserKernel.PageBlock;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
		pid = numOfProcess++;
		numOfRunningProcess++;
        fd[0] =  UserKernel.console.openForReading();
        fd[1] =  UserKernel.console.openForWriting();
    	allocatedPages = new ArrayList<PageBlock>();
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	thread = new UThread(this);
	thread.setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
     
    /**
     * changed by Xiang Sitao
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	if (vaddr < 0 || vaddr >= numPages * Processor.pageSize)
	    return 0;
	int amount = Math.min(length, memory.length-vaddr);
	int beginVPN = vaddr / Processor.pageSize;
	int beginOffset = vaddr % Processor.pageSize;
	int endVPN = (vaddr + amount) / Processor.pageSize;
	int endOffset = (vaddr + amount) % Processor.pageSize;
	if (beginVPN == endVPN) {
		System.arraycopy(memory, pageTable[beginVPN].ppn * Processor.pageSize + beginOffset, data, offset, endOffset - beginOffset);
		return amount;
	}
	System.arraycopy(memory, pageTable[beginVPN].ppn * Processor.pageSize + beginOffset, data, offset, Processor.pageSize - beginOffset);
	offset += Processor.pageSize - beginOffset;
	for (int i = beginVPN + 1; i < endVPN; i++){
		System.arraycopy(memory, pageTable[i].ppn * Processor.pageSize, data, offset, Processor.pageSize);
		offset += Processor.pageSize;
	}
	if (endOffset != 0){
		System.arraycopy(memory, pageTable[endVPN].ppn * Processor.pageSize, data, offset, endOffset);
	}

	return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
     
    /**
     * changed by Xiang Sitao
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	if (vaddr < 0 || vaddr >= numPages * Processor.pageSize)
	    return 0;
	int amount = Math.min(length, memory.length-vaddr);
	int beginVPN = vaddr / Processor.pageSize;
	int beginOffset = vaddr % Processor.pageSize;
	int endVPN = (vaddr + amount) / Processor.pageSize;
	int endOffset = (vaddr + amount) % Processor.pageSize;
	if (beginVPN == endVPN) {
		System.arraycopy(data, offset, memory, pageTable[beginVPN].ppn * Processor.pageSize + beginOffset, endOffset - beginOffset);
		return amount;
	}
	System.arraycopy(data, offset, memory, pageTable[beginVPN].ppn * Processor.pageSize + beginOffset, Processor.pageSize - beginOffset);
	offset += Processor.pageSize - beginOffset;
	for (int i = beginVPN + 1; i < endVPN; i++){
		System.arraycopy(data, offset, memory, pageTable[i].ppn * Processor.pageSize, Processor.pageSize);
		offset += Processor.pageSize;
	}
	if (endOffset != 0){
		System.arraycopy(data, offset, memory, pageTable[endVPN].ppn * Processor.pageSize, endOffset);
	}

	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;
	
	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }
    
    /**
     * add by Xiang Sitao
     */
    public boolean allocateMemory(){
    	boolean s = Machine.interrupt().disable();
    	if (numPages > UserKernel.numEmptyPages){
        	Machine.interrupt().restore(s);
    		return false;
    	}
    	int t = numPages;
    	while (t != 0){
    		PageBlock p = UserKernel.emptyPageList.allocate(t);
    		t -= p.size;
    		allocatedPages.add(p);
    	}
    	Machine.interrupt().restore(s);
    	pageTable = new TranslationEntry[numPages];
    	int k = 0;
    	for (int i = 0; i < allocatedPages.size(); i++)
    		for (int j = 0; j < allocatedPages.get(i).size; j++){
    			pageTable[k] = new TranslationEntry(k, allocatedPages.get(i).position + j, true, false, false, false);
    			k++;
    		}
    	return true;
    }
    
    public void freeMemory(){
    	PageBlock p = UserKernel.emptyPageList;
    	boolean s = Machine.interrupt().disable();
    	for (int i = 0; i < allocatedPages.size(); i++)
    		p = p.free(allocatedPages.get(i));
    	Machine.interrupt().restore(s);
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}
	
	if (!allocateMemory())
		return false;

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;

		section.loadPage(i, pageTable[vpn].ppn);
		pageTable[vpn].readOnly = section.isReadOnly();
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	freeMemory();
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }

	/**
	 *  the code Dieqiao Feng Added
	 * */


	private int handleExec(int fileAddress, int argc, int argvAddress){
		String filename = readVirtualMemoryString(fileAddress, 256);
		String[] args = new String[argc];
		for (int i = 0; i < argc; i++){
			byte[] address = new byte[4];
			readVirtualMemory(argvAddress+i*4, address);
			args[i] = readVirtualMemoryString(Lib.bytesToInt(address,0), 256);
		}
		UserProcess process = UserProcess.newUserProcess();
		process.parentProcess = this;
		childProcess.add(process);
		if (!process.execute(filename, args))
			return -1;
		return process.pid;
    }

	private int handleJoin(int pid, int statusAddress){
		UserProcess process = null;
		for (int i = 0; i < childProcess.size(); i++){
			if (pid == childProcess.get(i).pid){
				process = childProcess.get(i);
				break;
			}
		}
		if (process == null || process.thread == null)
			return -1;
		process.thread.join();
		byte[] byteArray = new byte[4];
		Lib.bytesFromInt(byteArray,0,process.status);
		int tt = writeVirtualMemory(statusAddress, byteArray);
		if (process.normalExit && tt == 4)
			return 1;
		return 0;
	}

	private int handleExit(int status){
		coff.close();
		for (int i = 0; i < 16; i++)
			if (fd[i] != null){
				fd[i].close();
				fd[i] = null;
			}
		this.status = status;
		normalExit = true;
		for (int i = 0; i < childProcess.size(); i++)
			childProcess.get(i).parentProcess = null;
		unloadSections();
		if (numOfRunningProcess == 1) Machine.halt();
		UThread.finish();
		numOfRunningProcess--;
		return 0;
	}

	/**
	 *  the code Luo Heng Add
	 */
	private int handleRead(int fileDescriptor, int buffer, int count){
        if (fileDescriptor<0 || fileDescriptor > MaxNumberOfFilesCanBeOpen){
            //System.out.println("aaaa");
            return -1;
        }
        if (fd[fileDescriptor] == null ){
           //System.out.println("bbbb");
            return -1;
        }
        if (count<0) {
           //System.out.println("cccc");
            return -1;
        }
        OpenFile theFile= fd[fileDescriptor];
        byte[] buf = new byte[count + 1];
        int offset = 0;
        int length = count;
        int returnValue =  theFile.read(buf, offset, length);
        writeVirtualMemory(buffer, buf);
        return returnValue;


    }
    private int handleWrite(int fileDescriptor, int buffer, int count){
        //System.out.print(fileDescriptor);
        //System.out.println("aaaa");
        if (fileDescriptor<0 || fileDescriptor > MaxNumberOfFilesCanBeOpen){
            return -1;
        }
        if (fd[fileDescriptor] == null ){
            return -1;
        }
        if (count<0) {
            return -1;
        }
        OpenFile theFile= fd[fileDescriptor];
        int pos=theFile.tell();
        byte[] buf= new byte[count + 1];	
        int offset = 0;
        int length = count;
        readVirtualMemory(buffer, buf);
        int returnValue =  theFile.write(buf, offset, length);
        //System.out.println("aaaa");

        return returnValue;

    }
    private int handleOpen(int name){
        int len= maxLengthGiven;
        String nameInFS = readVirtualMemoryString(name, len);
        if (nameInFS == null) {
            return -1;
        }
        //System.out.println(nameInFS);
        System.out.print(nameInFS);
        System.out.println(" open");
        Integer v00 = fileStatus.get(nameInFS);
        Boolean v01 = fileUnlinkStatus.get(nameInFS);
        //System.out.println(v00);
        //System.out.println(v01);
        //System.out.println("counter and flag");
        //System.out.println();
        if (v01==null){
            fileUnlinkStatus.put(nameInFS,false);
        }else if(v01){
            return -1;
        }

        if (v00 != null){
            fileStatus.put(nameInFS,v00+1);
        }else {
            fileStatus.put(nameInFS,1);
            fileUnlinkStatus.put(nameInFS,false);
        }
        OpenFile theFile= UserKernel.fileSystem.open(nameInFS, false);
        int fileDescriptor=-1;
        int flag =0;
        for (int i=2 ;i<MaxNumberOfFilesCanBeOpen;i++){
            if (fd[i] == null) {
                fd[i] = theFile;
                fileDescriptor = i;
                flag = 1;
                break;
            }
        }
        if (flag == 0) {
            return -1;
        }
        return fileDescriptor;
    }
    private int handleCreate(int name){
        int len= maxLengthGiven;
        String nameInFS = readVirtualMemoryString(name, len);

        if (nameInFS == null) {
            return -1;
        }
        System.out.print(nameInFS);
        System.out.println(" created");
        Integer v00 = fileStatus.get(nameInFS);
        Boolean v01 = fileUnlinkStatus.get(nameInFS);
        //System.out.println(v00);
        //System.out.println(v01);
        //System.out.println("counter and flag");
        //System.out.println();
        if (v01==null){
            fileUnlinkStatus.put(nameInFS,false);
        }else if(v01){
            return -1;
        }

        if (v00 != null){
            fileStatus.put(nameInFS,v00+1);
        }else {
            fileStatus.put(nameInFS,1);
        }
        OpenFile theFile= UserKernel.fileSystem.open(nameInFS, true);
        int fileDescriptor=-1;
        int flag =0;
        for (int i=2 ;i<MaxNumberOfFilesCanBeOpen;i++){
            if (fd[i] == null) {
                fd[i] = theFile;
                fileDescriptor = i;
                flag = 1;
                break;
            }
        }
        if (flag == 0) {
            return -1;
        }
        return fileDescriptor;
    }
    private int handleClose(int fileDescriptor){
        if (fileDescriptor<0 || fileDescriptor > MaxNumberOfFilesCanBeOpen){
            return -1;
        }
        if (fd[fileDescriptor] == null) {
            return -1;
        }
        OpenFile theFile=fd[fileDescriptor];
        System.out.print(theFile.getName());
        System.out.println(" closed");

        Integer v00 = fileStatus.get(theFile.getName());
        Boolean v01 = fileUnlinkStatus.get(theFile.getName());
        //System.out.println(v00);
        //System.out.println(v01);
        //System.out.println("counter and flag");
        //System.out.println();
        if (v00 != 1){
            fileStatus.put(theFile.getName(),v00-1);
        }else {
            if (!v01) {
                fileStatus.put(theFile.getName(), v00 - 1);
                //System.out.println(fileStatus.get(theFile.getName()));
            }else{
                //System.out.println("get it");
                String v02= theFile.getName();
                theFile.close();
                handleDelete(v02);
                fd[fileDescriptor]=null;
                return 0;
            }

        }
        theFile.close();
        fd[fileDescriptor]=null;
        return 0;
    }
    private int handleDelete(String name){
        System.out.print(name);
        System.out.println(" deleted");
        UserKernel.fileSystem.remove(name);
        return 0;
    }
    private int handleUnlink(int name){

        int len= maxLengthGiven;
        int rvalue=0;
        String nameInFS = readVirtualMemoryString(name, len);
        Integer v00 = fileStatus.get(nameInFS);
        Boolean v01 = fileUnlinkStatus.get(nameInFS);
        System.out.print(nameInFS);
        System.out.println(" wanted to be deleted");
        //System.out.println(v00);
        //System.out.println(v01);
        //System.out.println("counter and flag");
        //System.out.println();
        if (v00 == 0) {
            rvalue = handleDelete(nameInFS);
            fileUnlinkStatus.put(nameInFS,false);
        }else {
            fileUnlinkStatus.put(nameInFS,true);
        }
        return rvalue;
    }

    /**
     * Until here
     */
    private static final int syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        //System.out.print(syscall);
        //System.out.println("syscall");
        switch (syscall) {
	        case syscallHalt:
	            return handleHalt();
            case syscallExit:
                return  handleExit(a0);
            case syscallExec:
                return  handleExec(a0, a1, a2);
            case syscallJoin:
                return  handleJoin(a0, a1);
            case syscallOpen:
                return  handleOpen(a0);
            case syscallCreate:
                return  handleCreate(a0);
            case syscallRead:
                return  handleRead(a0, a1, a2);
            case syscallWrite:
                return  handleWrite(a0, a1, a2);
            case syscallClose:
                return  handleClose(a0);
            case syscallUnlink:
                return  handleUnlink(a0);

	        default:
	            Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	            Lib.assertNotReached("Unknown system call!");
	    }
	    return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

	/**
	 * Added by Dieqiao Feng
	 */

    public UserProcess parentProcess = null;
    private int pid;
    public static int numOfProcess = 0;
    public static int numOfRunningProcess = 0;
    public LinkedList<UserProcess> childProcess = new LinkedList();
    public boolean normalExit = false;
    public int status = 0;
    public UThread thread = null;
    
    /**
     * add by Luo Heng
     */
    protected int MaxNumberOfFilesCanBeOpen=16;
    protected OpenFile[] fd = new OpenFile[MaxNumberOfFilesCanBeOpen];

    protected int maxLengthGiven = 256;
    public static Hashtable<String,Integer>fileStatus=new Hashtable<String,Integer>(); //taa
    public static Hashtable<String,Boolean>fileUnlinkStatus=new Hashtable<String,Boolean>(); //taa

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    
    
    /**
     * add by Xiang Sitao
     */
    protected ArrayList<PageBlock> allocatedPages = null;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
}
