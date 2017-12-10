/**
 * Copyright (c) 2012 egross, sabha.
 * 
 * ThreadLogic - parses thread dumps and provides analysis/guidance
 * It is based on the popular TDA tool.  Thank you!
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.oracle.ateam.threadlogic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

import com.oracle.ateam.threadlogic.advisories.ThreadLogicConstants;
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.utils.CustomLogger;
import java.util.logging.Logger;

public class LockInfo implements Serializable {

  public static class DeadLockEntry implements Serializable {
    String deadlockMsg;
    String completeDeadlockStack;
    Collection<ThreadInfo> deadlockChain;
    
    private static Logger theLogger = CustomLogger.getLogger(LockInfo.class.getSimpleName());

    public DeadLockEntry(String deadlockMsg, Stack<ThreadInfo> deadlockChain) {
      this.deadlockMsg = deadlockMsg;
      this.deadlockChain = sanitiseChain(deadlockChain);

      ThreadAdvisory deadLockAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.DEADLOCK_PATTERN);
      Iterator<ThreadInfo> iter = this.deadlockChain.iterator();
      while (iter.hasNext()) {
        ThreadInfo th = iter.next();
        th.setHealth(HealthLevel.FATAL);
        ;
        th.getBlockedForLock().addAdvisory(deadLockAdvisory);
        th.addAdvisory(deadLockAdvisory);
      }

      theLogger.fine("Final Deadlock Chain Stack contains:\n");
      StringBuffer sbuf = new StringBuffer("Deadlock Chain:\n--------------------------\n");
      sbuf.append(deadlockMsg + "\n");
      int size = deadlockChain.size();
      for (iter = deadlockChain.iterator(); iter.hasNext();) {
        sbuf.append("\n\n\t" + iter.next().getContent());
      }
      sbuf.append("\n--------------------------\n");
      this.completeDeadlockStack = sbuf.toString();
    }

    private Collection<ThreadInfo> sanitiseChain(Collection<ThreadInfo> deadlockChain) {

      // Its possible for the chain to be deadlocked but the individual threads
      // might not be part of circular chain
      // Start with the end of the chain and navigate backward to see if they
      // all are mutually linked

      int stackDepth = deadlockChain.size();

      Collection<ThreadInfo> blockedChain = new LinkedList<ThreadInfo>();
      ArrayList<ThreadInfo> linkedThreads = new ArrayList<ThreadInfo>(deadlockChain);

      // Add the very last thread to chain first...
      ThreadInfo lastSavedThread = linkedThreads.get(stackDepth - 1);
      blockedChain.add(lastSavedThread);

      for (int i = 0; i < stackDepth - 1; i++) {
        ThreadInfo blockedThread = linkedThreads.get(i);
        LockInfo blockedForLock = blockedThread.getBlockedForLock();

        // If the last thread was owning the locks which is required by the
        // current thread
        // then they are linked...
        if (lastSavedThread.getOwnedLocks().contains(blockedForLock)) {
          blockedChain.add(blockedThread);
          lastSavedThread = blockedThread;
          break;
        }

        // Else check if the current thread owns lock required by the previously
        // saved thread
        LockInfo prevThreadBlockingLock = lastSavedThread.getBlockedForLock();
        if (blockedThread.getOwnedLocks().contains(prevThreadBlockingLock)) {
          blockedChain.add(blockedThread);
          lastSavedThread = blockedThread;
          break;
        }
      }
      return blockedChain;
    }

    public String getCompleteDeadlockStack() {
      return completeDeadlockStack;
    }

    public String getDeadlockMsg() {
      return deadlockMsg;
    }

    public void setDeadlockMsg(String deadlockMsg) {
      this.deadlockMsg = deadlockMsg;
    }

    public Collection<ThreadInfo> getDeadlockChain() {
      return deadlockChain;
    }
  }

  protected String id;
  protected ThreadInfo lockOwner;
  protected ThreadDumpInfo tdi;
  protected ArrayList<ThreadInfo> blockers = new ArrayList<ThreadInfo>();
  protected ArrayList<ThreadAdvisory> advisories = new ArrayList<ThreadAdvisory>();

  public LockInfo(String lockId) {
    this.id = lockId;
  }

  public LockInfo(String lockId, ThreadInfo lockOwner) {
    this.id = lockId;
    this.lockOwner = lockOwner;
    // if (this.lockOwner != null)
    // this.tDump = lockOwner.getTDump();
  }

  public ArrayList<ThreadInfo> getBlockers() {
    return blockers;
  }

  public void setBlockers(ArrayList<ThreadInfo> blockers) {
    this.blockers = blockers;
  }

  public void addBlocker(ThreadInfo blocker) {
    if (!this.blockers.contains(blocker))
      this.blockers.add(blocker);
  }

  public void removeBlocker(ThreadInfo blocker) {
    this.blockers.remove(blocker);
  }

  public String getLockId() {
    return id;
  }

  public void setLockId(String lockId) {
    this.id = lockId;
  }

  public ThreadInfo getLockOwner() {
    return lockOwner;
  }

  public void setLockOwner(ThreadInfo lockOwner) {
    this.lockOwner = lockOwner;
  }

  public static DeadLockEntry detectDeadlock(ArrayList<LockInfo> locks) {
    return detectDeadlock(locks.toArray(new LockInfo[] {}));
  }

  public static DeadLockEntry detectDeadlock(LockInfo[] locks) {

    // This stack will be used to track which threads have been visited
    // previously while navigating blocked lock chains...
    Stack<ThreadInfo> threadChainStack = new Stack<ThreadInfo>();

    for (LockInfo lock : locks) {

      // Clear the stack and start with the new lock in the list for a possible
      // deadlock...
      threadChainStack.clear();

      // Check if current owner is also blocked for a different lock
      // Then navigate to other locks to see if thats held by someone waiting
      // for the first lock...
      String currentLockId = lock.getLockId();
      ThreadInfo currentOwnerThread = lock.getLockOwner();

      ThreadAdvisory.runLockInfoAdvisory(lock);

      if (currentOwnerThread == null) {
        // theLogger.fine("Contention for an unlockedlock:" + currentLockId
        // +
        // " Possible due to high concurrency..\nSo skipping that from Deadlock check");
        continue;
      }

      ArrayList<ThreadInfo> blockedThreads = lock.getBlockers();
      ArrayList<LockInfo> allOwnedLocks = currentOwnerThread.getOwnedLocks();
      LockInfo blockedForLock = currentOwnerThread.getBlockedForLock();

      // Current owner not blocked for anything, so safe....
      // Nor is anyone currently waiting to lock
      // Ignore for case where the owner is also blocked for the same lock - must be transient - 
      // for a case where the lock its holding is the same lock its blocking too
      // Saw a rare case of the owner of the lock blocking for the same lock it owns
      // Possible the thread dump was taken at the exact moment as the thread tried to reobtain its lock
      /*
       * "RMICallHandler-2283" prio=1 tid=0x00002aab08ca8c70 nid=0x1f90 runnable [0x0000000046c5d000..0x0000000046c60c10]
       * at oracle.xml.parser.v2.XMLNode.xdkInit(XMLNode.java:3511)
       * - waiting to lock <0x00002acb22f504b0> (a oracle.j2ee.ws.saaj.soap.SOAPPartImpl$SOAPPartDocument)
       * at oracle.xml.parser.v2.XMLNode.<init>(XMLNode.java:469)
       * ...........
       * at oracle.j2ee.ws.saaj.soap.SOAPPartImpl.getEnvelope(SOAPPartImpl.java:77)
       * - locked <0x00002acb22f504b0> (a oracle.j2ee.ws.saaj.soap.SOAPPartImpl$SOAPPartDocument)
       * at oracle.j2ee.ws.saaj.soap.MessageImpl.getSOAPBody(MessageImpl.java:989)
       * 
       */      
      // Ignore this and move to next lock
      if ((blockedForLock == null) || (blockedThreads.size() == 0) || lock.equals(blockedForLock)) {
        continue;
      }

      threadChainStack.push(currentOwnerThread);
      String navigationTrail = "Checking for deadlock with Thread[" + currentOwnerThread.getName()
          + "], blocked for Lock [" + blockedForLock.getLockId() + "] while holding Lock [" + currentLockId + "] ";
      // theLogger.fine(navigationTrail);

      // Check to see if the waiter on the lock can actual hold the lock
      // Deadlock will occur when waiter on this lock blocks the owner of the
      // lock from grabbing a resource that the waiter has already locked
      // In that case, canThreadWaitForLock() will return false as it will mean
      // a deadlock if the waiter does grab a lock...
      if (isCyclicDependency(blockedForLock, threadChainStack)) {
        String deadlockChain = printDeadlockChain(threadChainStack);

        ThreadAdvisory advisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.DEADLOCK_PATTERN);
        lock.addAdvisory(advisory);

        return new DeadLockEntry(deadlockChain, threadChainStack);
      }

    }
    return null;
  }

  public static String printDeadlockChain(Collection<ThreadInfo> threadChainStack) {

    StringBuffer sbuf = new StringBuffer(1000);
    int size = threadChainStack.size();
    for (ThreadInfo th : threadChainStack) {
      LockInfo targetLock = th.getBlockedForLock();
      ThreadInfo targetLockOwner = targetLock.getLockOwner();

      sbuf.append("   Thread: ");
      sbuf.append(th.getFilteredName());
      sbuf.append(" is waiting to lock monitor ");
      sbuf.append(targetLock.getLockId());
      sbuf.append(",<br>&nbsp;&nbsp; which is held by Thread: ");
      sbuf.append(targetLockOwner.getFilteredName());
      sbuf.append("<br><br>");
    }
    return sbuf.toString();
  }

  /*
   * public static String getDeadlockChainAsHtml(Stack<ThreadInfo>
   * threadChainStack) {
   * 
   * StringBuffer lockRowsBuf = new StringBuffer(1000); int size =
   * threadChainStack.size();
   * 
   * boolean asAnchor = false; boolean hasStateHealth = false;
   * lockRowsBuf.append(genRow("Lock" + COLUMN_SEPARATOR +
   * "Thread blocked for Lock" + COLUMN_SEPARATOR + "Thread holding the Lock",
   * asAnchor, hasStateHealth)) ;
   * 
   * for (int i = 0; i < size; i++) {
   * 
   * ThreadHolder th = threadChainStack.get(i); LockInfo targetLock =
   * th.getBlockedForLock(); ThreadHolder targetLockOwner =
   * targetLock.getLockOwner();
   * 
   * StringBuffer sbuf = new StringBuffer();
   * 
   * asAnchor = false; hasStateHealth = false;
   * 
   * sbuf.append(targetLock.genAnchorRef()); sbuf.append(COLUMN_SEPARATOR);
   * sbuf.append(th.genAnchorRef()); sbuf.append(COLUMN_SEPARATOR);
   * sbuf.append(targetLockOwner.genAnchorRef());
   * 
   * lockRowsBuf.append(genRow(sbuf.toString(), asAnchor, hasStateHealth));
   * lockRowsBuf.append(genRow("" + COLUMN_SEPARATOR + th.getThreadStack() +
   * COLUMN_SEPARATOR + targetLockOwner.getThreadStack(), asAnchor,
   * hasStateHealth)); } return lockRowsBuf.toString(); }
   */

  public static boolean isCyclicDependency(LockInfo blockedForLock, Stack<ThreadInfo> threadChainStack) {

    // The targetLock owner cannot be blocked for the same lock,
    // so we can ignore the check if lockOwner is same as the thread contending
    // for lock...

    // Lets look if the owner of the current desired lock is blocking for any
    // locks owned by the original thread wanting the desired lock
    // We can check the stack to see if there is a matching entry...
    ThreadInfo ownerOfBlockingLock = blockedForLock.lockOwner;

    if ((ownerOfBlockingLock == null) || (ownerOfBlockingLock.getBlockedForLock() == null)
        || (ownerOfBlockingLock.getOwnedLocks().size() == 0) ) {
      // This thread has no locks or its not blocked for any locks
      return false;
    }

    
    // Saw a rare case of the owner of the lock blocking for the same lock it owns
    // Possible the thread dump was taken at the exact moment as the thread tried to reobtain its lock
    /*
     * "RMICallHandler-2283" prio=1 tid=0x00002aab08ca8c70 nid=0x1f90 runnable [0x0000000046c5d000..0x0000000046c60c10]
     * at oracle.xml.parser.v2.XMLNode.xdkInit(XMLNode.java:3511)
     * - waiting to lock <0x00002acb22f504b0> (a oracle.j2ee.ws.saaj.soap.SOAPPartImpl$SOAPPartDocument)
     * at oracle.xml.parser.v2.XMLNode.<init>(XMLNode.java:469)
     * ...........
     * at oracle.j2ee.ws.saaj.soap.SOAPPartImpl.getEnvelope(SOAPPartImpl.java:77)
     * - locked <0x00002acb22f504b0> (a oracle.j2ee.ws.saaj.soap.SOAPPartImpl$SOAPPartDocument)
     * at oracle.j2ee.ws.saaj.soap.MessageImpl.getSOAPBody(MessageImpl.java:989)
     * 
     */

    if (blockedForLock.getBlockers().contains(ownerOfBlockingLock)) {
      return false;
    }

    if (threadChainStack.contains(ownerOfBlockingLock))
      return true;

    // Save this thread in the stack for chaining of dependency....
    threadChainStack.push(ownerOfBlockingLock);

    // Now lets recursively see who is holding lock for the ownerOfBlockingLock
    return isCyclicDependency(ownerOfBlockingLock.getBlockedForLock(), threadChainStack);
  }

  public void addAdvisory(ThreadAdvisory advisory) {
    if ((advisories != null) && !advisories.contains(advisory))
      this.advisories.add(advisory);
  }

  public void addAdvisories(ArrayList<ThreadAdvisory> advisories) {
    if (advisories != null) {
      for (ThreadAdvisory advisory : advisories) {
        if (!this.advisories.contains(advisory))
          this.advisories.addAll(advisories);
      }
    }
  }

  public void setParentThreadDump(ThreadDumpInfo tdi) {
    this.tdi = tdi;
  }

  public ThreadDumpInfo getParentThreadDump() {
    return this.tdi;
  }

}
