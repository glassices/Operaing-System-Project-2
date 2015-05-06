package nachos.userprog;

import nachos.userprog.UserKernel.PageBlock;

public class MemoryTest extends UserProcess {
	public static void selfTest(){
		MemoryTest memoryTest1 = new MemoryTest(5);
		memoryTest1.allocateMemory();
		MemoryTest memoryTest2 = new MemoryTest(5);
		memoryTest2.allocateMemory();
		MemoryTest memoryTest3 = new MemoryTest(5);
		memoryTest3.allocateMemory();
		MemoryTest memoryTest4 = new MemoryTest(5);
		memoryTest4.allocateMemory();
		MemoryTest memoryTest5 = new MemoryTest(5);
		memoryTest5.allocateMemory();
		MemoryTest memoryTest6 = new MemoryTest(5);
		memoryTest6.allocateMemory();
		MemoryTest memoryTest7 = new MemoryTest(5);
		memoryTest7.allocateMemory();
		MemoryTest memoryTest8 = new MemoryTest(5);
		memoryTest8.allocateMemory();
		MemoryTest memoryTest9 = new MemoryTest(5);
		memoryTest9.allocateMemory();
		MemoryTest memoryTest10 = new MemoryTest(5);
		memoryTest10.allocateMemory();
		MemoryTest memoryTest11 = new MemoryTest(5);
		memoryTest11.allocateMemory();
		memoryTest2.freeMemory();
		memoryTest3.freeMemory();
		memoryTest5.freeMemory();
		memoryTest7.freeMemory();
		memoryTest6.freeMemory();
		memoryTest10.freeMemory();
		memoryTest9.freeMemory();
		MemoryTest memoryTest12 = new MemoryTest(10);
		memoryTest12.allocateMemory();
		MemoryTest memoryTest13 = new MemoryTest(30);
		memoryTest13.allocateMemory();
		memoryTest1.freeMemory();
		memoryTest4.freeMemory();
		memoryTest8.freeMemory();
		memoryTest11.freeMemory();
		MemoryTest memoryTest14 = new MemoryTest(20);
		memoryTest14.allocateMemory();
		memoryTest12.freeMemory();
		memoryTest13.freeMemory();
		memoryTest14.freeMemory();
	}
	
	public MemoryTest(int numPages){
		this.numPages = numPages;
	}
	
	public boolean allocateMemory(){
		boolean ret = super.allocateMemory();
		System.out.println(toString() + ": " + numPages + " pages allocated");
		for (int i = 0; i < allocatedPages.size(); i++)
			System.out.println("  [" + allocatedPages.get(i).position + ", " + (allocatedPages.get(i).position + allocatedPages.get(i).size - 1) + "]");
		System.out.println(UserKernel.numEmptyPages + " unallocated pages remaining:");
		PageBlock p = UserKernel.emptyPageList.next;
		while (p.next != null){
			System.out.println("  [" + p.position + ", " + (p.position + p.size - 1) + "]");
			p = p.next;
		}
		return ret;
	}
	
	public void freeMemory(){
		super.freeMemory();
		System.out.println(toString() + ": " + numPages + " pages freed");
		for (int i = 0; i < allocatedPages.size(); i++)
			System.out.println("  [" + allocatedPages.get(i).position + ", " + (allocatedPages.get(i).position + allocatedPages.get(i).size - 1) + "]");
		System.out.println(UserKernel.numEmptyPages + " unallocated pages remaining:");
		PageBlock p = UserKernel.emptyPageList.next;
		while (p.next != null){
			System.out.println("  [" + p.position + ", " + (p.position + p.size - 1) + "]");
			p = p.next;
		}
	}
}
