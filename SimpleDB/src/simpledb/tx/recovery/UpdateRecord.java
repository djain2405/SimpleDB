package simpledb.tx.recovery;

import simpledb.server.SimpleDB;
import simpledb.buffer.*;
import simpledb.file.Block;
import simpledb.log.BasicLogRecord;

class UpdateRecord implements LogRecord {
   private int txnum, offset;
   private String val;
   private Block blk;
   private Block savedBlk;
   
   /**
    * Creates a new setstring log record.
    * @param txnum the ID of the specified transaction
    * @param blk the block containing the value
    * @param offset the offset of the value in the block
    * @param val the new value
    */
   public UpdateRecord(int txnum, Block blk, Block savedBlock) {
      this.txnum = txnum;
      this.blk = blk;
      this.savedBlk = savedBlock;
   }
   
   /**
    * Creates a log record by reading five other values from the log.
    * @param rec the basic log record
    */
   public UpdateRecord(BasicLogRecord rec) {
      txnum = rec.nextInt();
      String filename = rec.nextString();
      int blkNum = rec.nextInt();
      blk = new Block(filename, blkNum);
      String savedfilename = rec.nextString();
      int savedBlkNum = rec.nextInt();
      savedBlk = new Block(savedfilename, savedBlkNum);

   }
   
   /** 
    * Writes a setString record to the log.
    * This log record contains the SETSTRING operator,
    * followed by the transaction id, the filename, number,
    * and offset of the modified block, and the previous
    * string value at that offset.
    * @return the LSN of the last log value
    */
   public int writeToLog() {
      Object[] rec = new Object[] {UPDATE, txnum, blk.fileName(),
         blk.number(), savedBlk.fileName(), savedBlk.number()};
      return logMgr.append(rec);
   }
   
   public int op() {
      return UPDATE;
   }
   
   public int txNumber() {
      return txnum;
   }
   
   public String toString() {
      return "<UPDATE " + txnum + " " + blk.fileName() + " " + blk.number() + " " + savedBlk.number() + ">";
   }
   
   /** 
    * Replaces the specified data value with the value saved in the log record.
    * The method pins a buffer to the specified block,
    * calls setString to restore the saved value
    * (using a dummy LSN), and unpins the buffer.
    * @see simpledb.tx.recovery.LogRecord#undo(int)
    */
   public void undo(int txnum) {
	   BufferMgr buffMgr = SimpleDB.bufferMgr();
	      Buffer buff = buffMgr.pin(blk);
	      buff.restoreBlock(savedBlk.fileName(), savedBlk.number());
	      buffMgr.unpin(buff);
	   /*
      BufferMgr buffMgr = SimpleDB.bufferMgr();
      Buffer buff = buffMgr.pin(blk);
      buff.setString(offset, val, txnum, -1);
      buffMgr.unpin(buff);
   */}
}
