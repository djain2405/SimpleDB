package simpledb.buffer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import simpledb.file.Block;
import simpledb.file.FileMgr;

/**
 * Manages the pinning and unpinning of buffers to blocks.
 * @author Edward Sciore
 *
 */
class BasicBufferMgr {
   private Buffer[] bufferpool;
   private ConcurrentHashMap<Block, Buffer> bufferPoolMap;
   private int numAvailable;
   private int clockhand;
   private int gClockRefCounter;
   
   /**
    * Creates a buffer manager having the specified number 
    * of buffer slots.
    * This constructor depends on both the {@link FileMgr} and
    * {@link simpledb.log.LogMgr LogMgr} objects 
    * that it gets from the class
    * {@link simpledb.server.SimpleDB}.
    * Those objects are created during system initialization.
    * Thus this constructor cannot be called until 
    * {@link simpledb.server.SimpleDB#initFileAndLogMgr(String)} or
    * is called first.
    * @param numbuffs the number of buffer slots to allocate
    */
   BasicBufferMgr(int numbuffs, int gClockRefCounter) {
	   this.gClockRefCounter = gClockRefCounter;
      bufferpool = new Buffer[numbuffs];
	  bufferPoolMap = new ConcurrentHashMap<Block, Buffer>();
      numAvailable = numbuffs;
      for (int i=0; i<numbuffs; i++)
         bufferpool[i] = new Buffer();
      clockhand = 0;
   }
   
   /**
    * Flushes the dirty buffers modified by the specified transaction.
    * @param txnum the transaction's id number
    */
   synchronized void flushAll(int txnum) {
      for (Buffer buff : bufferpool)
         if (buff.isModifiedBy(txnum))
         buff.flush();
   }
   
   /**
    * Pins a buffer to the specified block. 
    * If there is already a buffer assigned to that block
    * then that buffer is used;  
    * otherwise, an unpinned buffer from the pool is chosen.
    * Returns a null value if there are no available buffers.
    * @param blk a reference to a disk block
    * @return the pinned buffer
    */
   synchronized Buffer pin(Block blk) {
      Buffer buff = findExistingBuffer(blk);
      if (buff == null) {
         buff = chooseUnpinnedBuffer();
         if (buff == null)
            return null;
         
         // Delete the <Block, Buffer> from Map if Buffer was previously allocated
         for(Map.Entry<Block, Buffer> entry : bufferPoolMap.entrySet()) {
        	 if(entry.getValue() == buff)
        		 bufferPoolMap.remove(entry.getKey());
         }
         
         buff.assignToBlock(blk);
         bufferPoolMap.put(blk, buff);
      }

      if (!buff.isPinned())
         numAvailable--;
      
      buff.pin();
      
   	  int noFreeBuf = 0;
   	  for (int i = 0; i < bufferpool.length; i++) {
   		  Buffer b = bufferpool[i];
   		  
			if(b.isPinned())
				
				System.out.println("B "+(i+1));
			else
			{
				System.out.println("-");
				noFreeBuf++;
			}
			
			System.out.println("PIN COUNT "+b.getPins());
			
			if(b.isPinned())
				System.out.println("REF COUNT -");
			else
				System.out.println("REF COUNT "+b.getReferenceCounter());
		}
   	System.out.println("---------------------------------------------------------------------------------------------------");
   	  System.out.println("AVAILABLE BUFFER COUNT : "+noFreeBuf);
   	  System.out.println("---------------------------------------------------------------------------------------------------");
      
      return buff;
   }
   
   /**
    * Allocates a new block in the specified file, and
    * pins a buffer to it. 
    * Returns null (without allocating the block) if 
    * there are no available buffers.
    * @param filename the name of the file
    * @param fmtr a pageformatter object, used to format the new block
    * @return the pinned buffer
    */
   synchronized Buffer pinNew(String filename, PageFormatter fmtr) {
      Buffer buff = chooseUnpinnedBuffer();
      if (buff == null)
         return null;
      buff.assignToNew(filename, fmtr);
/*      Block b = buff.block();
        if(b!=null && !allocatedBuffers.containsKey(b))
        allocatedBuffers.put(buff.block(), buff);
*/      numAvailable--;
      buff.pin();
      return buff;
   }
   
   /**
    * Unpins the specified buffer.
    * @param buff the buffer to be unpinned
    */
   synchronized void unpin(Buffer buff) {
      buff.unpin();
      if (!buff.isPinned()) {
    	  numAvailable++;
    	  buff.setReferenceCounter(gClockRefCounter);
      }   
      int noFreeBuf = 0;
	  for (int i = 0; i < bufferpool.length; i++) {
		  Buffer b = bufferpool[i];
		  

			if(b.isPinned())
				
				System.out.println("B "+(i+1));
			else
			{
				System.out.println("-");
				noFreeBuf++;
			}
			
			System.out.println("PIN COUNT "+b.getPins());
			
			if(b.isPinned())
				System.out.println("REF COUNT -");
			else
				System.out.println("REF COUNT "+b.getReferenceCounter());
	}
	   	System.out.println("---------------------------------------------------------------------------------------------------");
	   	  System.out.println("AVAILABLE BUFFER COUNT : "+noFreeBuf);
	   	  System.out.println("---------------------------------------------------------------------------------------------------");
   }
   
   /**
    * Returns the number of available (i.e. unpinned) buffers.
    * @return the number of available buffers
    */
   int available() {
      return numAvailable;
   }
   
   private Buffer findExistingBuffer(Block blk) {
	  if(bufferPoolMap.containsKey(blk))
		  return bufferPoolMap.get(blk);
      return null;
   }
   
   private Buffer chooseUnpinnedBuffer() {
	  /* for (Buffer buff : bufferpool)
       if (!buff.isPinned())
       return buff;
    return null;*/
	   System.out.println();
	   System.out.println("GClock policy used!");
	   int maxRotations = gClockRefCounter + 1;
	   while(maxRotations>0) {
		   //System.out.println("......................................"+bufferpool[clockhand].getReferenceCounter());
		   if(!bufferpool[clockhand].isPinned()) {
			   if(bufferpool[clockhand].getReferenceCounter() == 0)
			   {
					try {
						System.out.println("BLOCK REPLACED : "+(clockhand+1)+". File : "+bufferpool[clockhand].block().fileName());
					} catch (Exception e) {
					}
				   return bufferpool[clockhand];
			   }
			   else {
				   bufferpool[clockhand].decrementReferenceCounter();
				   clockhand = (clockhand+1) % bufferpool.length;
			   }	   
		   } else {
			   clockhand = (clockhand+1) % bufferpool.length;
			   maxRotations--;
		   }
	   }
	   return null;
    }
}
