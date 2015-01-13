- Simple DB server needs 2 arguments now instead of 1; the first is the directory name and the second is an int value which is the i value of the GClock reference counter.

Command line argument:
e.g.
C:\Users\Username>java -cp "Pathname" simpledb.server.Startup simpleDB 5

To Run/Test task 3:(recovery management system)

- In file RecoveryMgr.java, in function setInt(), comment line 70 -  return new SetIntRecord(txnum, blk, offset, oldval).writeToLog(); and uncomment block of code from line 71 - line 79.

- In file RecoveryMgr.java, in function setString(), comment line 97 -  return new SetStringRecord(txnum, blk, offset, oldval).writeToLog(); and uncomment block of code from line 98 - line 105.

In addition to above 2 steps, to simulate the testing for task3, we have also done the following step:

- In file CommitRecord.java, comment the line 36 - return logMgr.append(rec); and uncomment the next line return 0.

Please note that this step is done so that the Commit record does not get written to the log and we can then simulate a recovery. Because in case the Commit record is written to the log file, then it is not possible to simulate a recovery.

