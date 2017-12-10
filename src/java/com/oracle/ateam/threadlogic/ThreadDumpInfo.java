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
/*
 * ThreadDumpInfo.java
 *
 * This file is part of TDA - Thread Dump Analysis Tool.
 *
 * TDA is free software; you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * TDA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with TDA; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * $Id: ThreadDumpInfo.java,v 1.11 2008-08-13 15:52:19 irockel Exp $
 */
package com.oracle.ateam.threadlogic;

import com.oracle.ateam.threadlogic.LockInfo.DeadLockEntry;
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup;
import com.oracle.ateam.threadlogic.categories.Category;
import com.oracle.ateam.threadlogic.parsers.AbstractDumpParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Thread Dump Information Node. It stores structural data about the thread dump
 * and provides methods for generating html information for displaying infos
 * about the thread dump.
 * 
 * @author irockel
 */
public class ThreadDumpInfo extends ThreadLogicElement {
  private int logLine;
  private int overallThreadsWaitingWithoutLocksCount;

  private String startTime;
  private String jvmVersion;
  private String jvmType;  
  private boolean parsedWithFBParser = false;  
  private String overview;
  private Analyzer dumpAnalyzer;

  private Category waitingThreads;
  private Category sleepingThreads;
  private Category lockingThreads;
  private Category monitors;
  private Category monitorsWithoutLocks;
  private Category blockingMonitors;
  private Category threads;
  private Category deadlocks;
  private HeapInfo heapInfo;
  

  protected String deadLockMsg;
  protected DeadLockEntry deadlockEntry;
  protected boolean hasDeadlock;
  protected boolean isIBMJVM = false;
  protected String mainThread = "";
  private Logfile logFile;

  protected int noOfLocks, noOfGroups, noOfThreads, noOfBlockedThreads;
  private int noOfRunningThreads;

  public static final String DEADLOCK_KEYWORD = "DEADLOCK";

  private ArrayList<ThreadInfo> threadList = new ArrayList<ThreadInfo>();
  private ArrayList<ThreadGroup> threadGrpList = new ArrayList<ThreadGroup>();

  private ArrayList<ThreadInfo> lockList = new ArrayList<ThreadInfo>();
  protected Hashtable<String, LockInfo> lockTable = new Hashtable<String, LockInfo>();
  protected Hashtable<String, ThreadInfo> threadTable = new Hashtable<String, ThreadInfo>();
  protected Hashtable<String, ThreadGroup> threadGroupTable = new Hashtable<String, ThreadGroup>();
  
  private Hashtable<String, String> threadContextDataMap = new Hashtable<String, String>();
  
  static ExecutorService fixedPoolExecutor = Executors.newFixedThreadPool(6);
  
  private static Logger theLogger = Logger.getLogger(ThreadDumpInfo.class.getSimpleName());

  public ThreadDumpInfo(String name, int lineCount) {
    super(name);
    this.logLine = lineCount;
  }

  /**
   * get the log line where to find the starting point of this thread dump in
   * the log file
   * 
   * @return starting point of thread dump in logfile, 0 if none set.
   */
  public int getLogLine() {
    return logLine;
  }

  /**
   * set the log line where to find the dump in the logfile.
   * 
   * @param logLine
   */
  public void setLogLine(int logLine) {
    this.logLine = logLine;
  }

  /**
   * get the approx. start time of the dump represented by this node.
   * 
   * @return start time as string, format may differ as it is just parsed from
   *         the log file.
   */
  public String getStartTime() {
    return startTime;
  }

  /**
   * set the start time as string, can be of any format.
   * 
   * @param startTime
   *          the start time as string.
   */
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  /**
   * get the overview information of this thread dump.
   * 
   * @return overview information.
   */
  public String getOverview() {
    if (overview == null) {
      createOverview();
    }
    return overview;
  }
  
  public String getBlockedThreadsStatus() {
      // Use the blocked thread count directly instead of going by waiting threads 
      // (as some appear as waiting but are actually blocked)
      //int threadsWaitingForLock = (getWaitingThreads() != null)? getWaitingThreads().getNodeCount(): 0;
    
      int threadsWaitingForLock = this.noOfBlockedThreads;
      
      String threadsWaitingLockColor = "";
      if (threadsWaitingForLock > 50)
        threadsWaitingLockColor = "rgb(255, 60, 60)";
      else if (threadsWaitingForLock > 20)
        threadsWaitingLockColor = "rgb(248, 116, 49)";
      
      return "<p style='background-color:" + threadsWaitingLockColor + "' >" + threadsWaitingForLock + "</p>";   
  }
  
  public static String getThreadDumpsOverview(ArrayList<ThreadDumpInfo> tdumpsList) {
    StringBuffer statData = new StringBuffer("<font face=System><br/><table border=1 cellpadding=0  >");   
    statData.append("<tr bgcolor=\"#bbbbbb\" style='text-align:center'>")
            .append("<td width=80 style='width:50pt'>Dump No</td>")
            .append("<td width=200 style='width:150pt'>Timestamp</td>")
            .append("<td width=80 style='width:60pt'>Thread Count</td>")
            .append("<td width=80 style='width:80pt'>Health</td>")
            .append("<td width=80 style='width:80pt'>Threads Blocked</td>")
            .append("<td width=80 style='width:80pt'>Threads Running</td>")
            .append("<td width=600 style='width:500pt'>Critical Advisories</td>")
            .append("</tr>");      

    String oddRow = "\"#eeeeee\"";
    String evenRow = "\"#dddddd\"";
    boolean rowIsOdd = true;
    
    for(ThreadDumpInfo td: tdumpsList) {
      String color = td.getHealth().getBackgroundRGBCode();
      String healthEntry = "<p style='background-color:" + color + "' >" + td.getHealth() + "</p>";

      int i = 0;
      ArrayList<ThreadAdvisory> critAdvisories = td.getCritAdvisories();
      StringBuffer critAdvisoryBuf = new StringBuffer("<p>");
      for(ThreadAdvisory critAdvisory: critAdvisories) {    
        if ( i++ > 0)
          critAdvisoryBuf.append(", ");
        
        critAdvisoryBuf.append(critAdvisory.getPattern());
      }
      critAdvisoryBuf.append("</p>");
      
      String startTime = td.getStartTime();
      if (startTime == null)
        startTime = "";      
      
      statData.append("<tr style='text-align:center' bgcolor=")
              .append((rowIsOdd?oddRow:evenRow))
              .append("><td width=80 style='width:50pt'>")
              .append(td.getName().replace("Dump No. ", ""))
              .append("</td><td width=200 style='width:150pt'>")
              .append(startTime)
              .append("</td><td width=80 style='width:60pt'>")
              .append(td.getThreads().getNodeCount())
              .append("</td><td width=80 style='width:80pt'>")
              .append(healthEntry)
              .append("</td><td width=60 style='width:60pt'>")
              .append(td.getBlockedThreadsStatus())
              .append("</td><td width=60 style='width:60pt'>")
              .append(td.getNoOfRunningThreads())
              .append("</td><td width=600 style='width:500pt;text-align:left'>")                    
              .append(critAdvisoryBuf)
              .append("</td></tr>");
      
      rowIsOdd = !rowIsOdd;       
    }
    statData.append("</table><br/>");
    
    return statData.toString();
  }

  
  public boolean isIBMJVM() {
    return this.isIBMJVM;
  }
  
  public void setIsIBMJVM() {
    this.isIBMJVM = true;
  }

  /**
   * creates the overview information for this thread dump.
   */
  private void createOverview() {
    StringBuffer statData = new StringBuffer("<font face=System "
        + "><table border=0><tr bgcolor=\"#dddddd\" ><td><font face=System "
        + ">Thread Dump Name</td><td></td><td width=\"150\"><b><font face=System>");
    statData.append(this.getName());    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#ffffff\"><td></td></tr>");    
    
    statData.append("<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Overall Thread Count</td><td></td><td colspan=3><b><font face=System>");
    statData.append(getThreads() == null ? 0 : getThreads().getNodeCount());
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System"
        + ">Main Thread</td><td></td><td colspan=3><b><font face=System>");
    statData.append(this.mainThread);
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System"
        + ">Timestamp</td><td></td><td colspan=3><b><font face=System>");
    statData.append( (this.startTime == null)? "Not Available":startTime);
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">JVM Vendor</td><td></td><td colspan=3><b><font face=System>");
    statData.append(this.getJvmType());
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">JVM Version</td><td></td><td colspan=3><b><font face=System>");    
    statData.append((this.jvmVersion == null)? "Not Available":jvmVersion);
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System"
        + ">Overall Monitor Count</td><td></td><td colspan=3><b><font face=System>");
    statData.append(getMonitors() == null ? 0 : getMonitors().getNodeCount());
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Number of threads waiting for a monitor</td><td></td><td><b><font face=System>");
    statData.append(getBlockedThreadsStatus());
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Number of threads locking a monitor</td><td></td><td><b><font face=System size>");
    statData.append(getLockingThreads() == null ? 0 : getLockingThreads().getNodeCount());
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Number of threads sleeping on a monitor</td><td></td><td><b><font face=System>");
    statData.append(getSleepingThreads() == null ? 0 : getSleepingThreads().getNodeCount());
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Number of threads running</td><td></td><td><b><font face=System>");
    statData.append(getNoOfRunningThreads());
    /*
     * statData.append(
     * "</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System " +
     * ">Number of deadlocks</td><td></td><td><b><font face=System>");
     * statData.append(getDeadlocks() == null? 0 :
     * getDeadlocks().getNodeCount());
     */
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Found Deadlock</td><td></td><td><b><font face=System>");
    statData.append(this.hasDeadlock);

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Number of Monitors without locking threads</td><td></td><td><b><font face=System>");
    statData.append(getMonitorsWithoutLocks() == null ? 0 : getMonitorsWithoutLocks().getNodeCount());
    statData.append("</b></td></tr>");
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Thread Dump Health </td><td></td><td><b><font face=System size>");
    
    String color = this.health.getBackgroundRGBCode();
    statData.append("<p style=\"background-color:" + color + ";\">" + this.health + "</p>");

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Critical Advisories </td><td></td><td width=600 style='width:500pt;text-align:left'><b><font face=System size>");
    
    ArrayList<ThreadAdvisory> critAdvisories = this.getCritAdvisories();
    int i = 0;
    for(ThreadAdvisory critAdvisory: critAdvisories) {    
      if ( i++ > 0)
        statData.append(", ");
      statData.append(critAdvisory.getPattern());
    }
    
    if (this.isParsedWithFBParser()) {    
      statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Was parsed via non VM specific Parser </td><td></td><td><b><font face=System+1>");    
      statData.append("<p><font style=color:Red><b> YES </b></font><p><br>");
    }   
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#ffffff\"><td></td></tr></table>");

    statData.append("<font face=System><table border=0>");
    
    if (this.isParsedWithFBParser()) {
      
      statData.append("<tr bgcolor=\"#cccccc\" ><td colspan=2><font face=System"
          + "><p><font style=color:Red><b>WARNING!!!</b></font><p><br>");
      
      statData.append("<font style=color:Red>Partial thread dumps or dumps generated via WLST or WLS Admin Console dumps won't help in indicating <br>");
      statData.append("complete information about Thread IDs or locking information between threads (except for JRockit). ThreadLogic won't be <br>");
      statData.append("able to analyze or report existence of Deadlocks or other Blocked conditions, Bottlenecks due to missing data on Locks or Threads.<br>");
      statData.append("<br>Also options such as WLST or Console <u>might not be successful if server is in hung or unresponsive</u> condition<br><br>");
      statData.append("<b>General Recommendation:</b><br> Use System options (ex: kill -3 or jrcmd or jstack) to generate thread dumps with complete monitor, lock<br>");
      statData.append("and thread id information and use complete thread dumps whenever possible for full detailed analysis!!");
      statData.append("</font><br></p></td></tr>");
      statData.append("<tr bgcolor=\"#ffffff\"><td></td></tr>");
    }    

    
    if (getHeapInfo() != null) {
      statData.append(getHeapInfo());
    }
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#ffffff\"><td></td></tr></table>");

    statData.append("<font face=System><table border=0>");   
          
    statData.append("<tr bgcolor=\"#cccccc\" ><td colspan=2><font face=System")
            .append("><p>Please click on <a href=\"threadgroups://\"><b>Thread Groups Summary</b></a>")
            .append(" for overall thread dump analysis and expand the node to see more detailed analysis<br>")
            .append("for each thread group category and individual threads.</font><p><br></td></tr></table>");      

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#ffffff\"><td></td></tr></table>");

    statData.append("<font face=System><table border=0>"); 
    // add hints concerning possible hot spots found in this thread dump.
    statData.append(getDumpAnalyzer().analyzeDump());

    setOverview(statData.toString());

  }

  /**
   * generate a monitor info node from the given information.
   * 
   * @param locks
   *          how many locks are on this monitor?
   * @param waits
   *          how many threads are waiting for this monitor?
   * @param sleeps
   *          how many threads have a lock on this monitor and are sleeping?
   * @return a info node for the monitor.
   */
  public static String getMonitorInfo(int locks, int waits, int sleeps) {
    StringBuffer statData = new StringBuffer(
        "<table border=0 bgcolor=\"#dddddd\"><tr><td><font face=System"
            + ">Threads locking monitor</td><td><b><font face=System>");
    statData.append(locks);
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td>");
    statData.append("<font face=System>Threads sleeping on monitor</td><td><b><font face=System>");
    statData.append(sleeps);
    statData.append("</b></td></tr>\n\n<tr><td>");
    statData.append("<font face=System>Threads waiting to lock monitor</td><td><b><font face=System>");
    statData.append(waits);
    statData.append("</b></td></tr>\n\n");
    if (locks == 0) {
      statData.append("<tr bgcolor=\"#ffffff\"<td></td></tr>");
      // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5086475
      statData
          .append("<tr bgcolor=\"#cccccc\"><td><font face=System> "
              + "<p>This monitor doesn't have a thread locking it. This means one of the following is true:</p>"
              + "<ul><li>a VM Thread is holding it."
              + "<li>This lock is a <tt>java.util.concurrent</tt> lock and the thread holding it is not reported in the stack trace"
              + "because the JVM option -XX:+PrintConcurrentLocks is not present."
              + "<li>This lock is a custom java.util.concurrent lock either not based off of"
              + " <tt>AbstractOwnableSynchronizer</tt> or not setting the exclusive owner when a lock is granted.</ul>");
      statData.append("If you see many monitors having no locking thread (and the latter two conditions above do "
          + "not apply), this usually means the garbage collector is running.<br>");
      statData
          .append("In this case you should consider analyzing the Garbage Collector output. If the dump has many monitors with no locking thread<br>");
      statData
          .append("a click on the <a href=\"dump://\">dump node</a> will give you additional information.<br></td></tr>");
    }
    if (areALotOfWaiting(waits)) {
      statData.append("<tr bgcolor=\"#ffffff\"<td></td></tr>");
      statData.append("<tr bgcolor=\"#cccccc\"><td><font face=System "
          + "<p>A lot of threads are waiting for this monitor to become available again.</p><br>");
      statData
          .append("This might indicate a congestion. You also should analyze other locks blocked by threads waiting<br>");
      statData.append("for this monitor as there might be much more threads waiting for it.<br></td></tr>");
    }
    statData.append("</table>");

    return (statData.toString());
  }

  /**
   * checks if a lot of threads are waiting
   * 
   * @param waits
   *          the wait to check
   * @return true if a lot of threads are waiting.
   */
  public static boolean areALotOfWaiting(int waits) {
    return (waits > 5);
  }

  /**
   * set the overview information of this thread dump.
   * 
   * @param overview
   *          the infos to be displayed (in html)
   */
  public void setOverview(String overview) {
    this.overview = overview;
  }

  public Category getWaitingThreads() {
    return waitingThreads;
  }

  public void setWaitingThreads(Category waitingThreads) {
    this.waitingThreads = waitingThreads;
  }

  public Category getSleepingThreads() {
    return sleepingThreads;
  }

  public void setSleepingThreads(Category sleepingThreads) {
    this.sleepingThreads = sleepingThreads;
  }

  public Category getLockingThreads() {
    return lockingThreads;
  }

  public void setLockingThreads(Category lockingThreads) {
    this.lockingThreads = lockingThreads;
  }

  public Category getMonitors() {
    return monitors;
  }

  public void setMonitors(Category monitors) {
    this.monitors = monitors;

  }

  public Category getBlockingMonitors() {
    return blockingMonitors;
  }

  public void setBlockingMonitors(Category blockingMonitors) {
    this.blockingMonitors = blockingMonitors;
  }

  public Category getMonitorsWithoutLocks() {
    return monitorsWithoutLocks;
  }

  public void setMonitorsWithoutLocks(Category monitorsWithoutLocks) {
    this.monitorsWithoutLocks = monitorsWithoutLocks;
  }

  public Category getThreads() {
    return threads;
  }

  public void setThreads(Category threads) {
    this.threads = threads;
    int noOfThreads = threads.getNodeCount();
    int index = 0;
    
    while (index < noOfThreads) {
      ThreadInfo ti = (ThreadInfo) threads.getNodeAt(index++).getUserObject();
      ti.setParentThreadDump(this);
      ti.setIsIBMJVM(this.isIBMJVM);
      this.threadList.add(ti);
      
      String nid = ti.getNid();      
      String contextData = this.getThreadContextData(nid);
      if (contextData != null)
        ti.setCtxData(contextData);
      
      if (ti.isMainThread()) {
        String[] stackLines = ti.getContent().split("(\n)|(\r\n)");        
        for(String classEntry: stackLines) {
          
          int mainIndex = classEntry.indexOf(".main(");
          if (mainIndex > 0) {
            this.mainThread = classEntry.substring(0, mainIndex).replaceAll("/", ".").trim();
            int len = this.mainThread.length();
            mainIndex = this.mainThread.indexOf(" ");
            if (mainIndex > 0) {
              this.mainThread = this.mainThread.substring(mainIndex + 1);
            }
            break;
          }
        }
      }
        

      // ThreadInfo.getName() returns everything including state/nid/tid..., so
      // use the filteredName that does not have the rest of the labels...      
      this.threadTable.put(ti.getNameId(), ti);
    }
  }

  public ThreadInfo getThread(String threadName) {
    // FIXME - SABHA
    // Remove the [ACTIVE], [STANDBY], [STUCK] labels as they interfere with diff/merge reporting
    String filteredThreadName = threadName.replaceAll("\\[.*\\] ", "").replaceAll("\" .*$", "\"").trim();    
    return this.threadTable.get(filteredThreadName);
  }
  
  public ThreadInfo getThreadByName(String threadName) {
    String filteredSearchName = threadName.replaceAll("\\[.*\\] ", "").replaceAll("\" .*$", "\"").trim();
    Collection<ThreadInfo> threads = this.threadTable.values();
    for(ThreadInfo ti: threads) {
      String filteredName = ti.getFilteredName();      
      if (filteredName.contains(filteredSearchName))
        return ti;
    }
    return null;
  }
  
  public ThreadInfo getThreadById(String id) {
    if (id == null)
      return null;
    
    Collection<ThreadInfo> threads = this.threadTable.values();
    for(ThreadInfo ti: threads) {
      String tid = ti.getId();      
      if (id.contains(id))
        return ti;
    }
    return null;
  }

  public Hashtable<String, ThreadInfo> getThreadMap() {
    return this.threadTable;
  }

  public void parseLocks(AbstractDumpParser parser) {

    for (ThreadInfo ti : threadList) {
      parser.createLockInfo(ti);

      String lockId = "";
      for (LockInfo lock : ti.getOwnedLocks()) {
        this.addLock(lock);
        lockId = lock.getLockId();
        if (this.lockTable.get(lockId) == null)
          lockTable.put(lock.getLockId(), lock);
      }

      LockInfo lock = ti.getBlockedForLock();
      if (lock != null) {
        this.addLock(lock);
        lockId = lock.getLockId();
        if (this.lockTable.get(lockId) == null)
          lockTable.put(lock.getLockId(), lock);
      }
    }

  }

  // Special method for IBM Thread dumps
  // The lock & threads are separate and have to be linked together
  // The lock has partial thread info while the threads dont know abt locks...
  public void linkThreadsWithLocks(Hashtable<String, LockInfo> lockTable) {

    this.lockTable = lockTable;

    for (LockInfo lock : lockTable.values()) {
      String lockId = lock.getLockId();
      this.addLock(lock);

      // Find out saved blocked threads
      // but these are not complete as they dont have stack trace/state... etc,
      // so get the names and then replace them with the full threads
      ArrayList<ThreadInfo> actualBlockerList = new ArrayList<ThreadInfo>();
      for (ThreadInfo blockedTi : lock.getBlockers()) {
        String threadNameId = blockedTi.getNameId();
        ThreadInfo actualThread = searchThreadFromTable(threadNameId);

        // should not happen
        if (actualThread == null)
          continue;

        actualBlockerList.add(actualThread);
      }

      lock.setBlockers(actualBlockerList);

      // Cannot set the thread's blockedForLock in the earlier loop as there
      // will be concurrent modification on the same lockinfo's blockers list...
      for (int i = 0; i < actualBlockerList.size(); i++) {
        ThreadInfo blockedThread = actualBlockerList.get(i);
        blockedThread.setBlockedForLock(lock);
      }

      ThreadInfo lockOwner = lock.getLockOwner();
      if (lockOwner == null)
        continue;

      ThreadInfo actualThreadOwner = searchThreadFromTable(lockOwner.getNameId());
      lock.setLockOwner(actualThreadOwner);
      actualThreadOwner.addOwnedLocks(lock);
    }
  }
  
  public ThreadInfo searchThreadFromTable(String nameId) {
    
    ThreadInfo ti = null;
    ti = threadTable.get(nameId);
    if (ti != null)
      return ti;
    
    // Its possible the thread didnt have ID information (as in IBM Lock section only carries the thread name, no tid)
    // 2LKMONINUSE sys_mon_t:0x000000011C47FAF8 infl_mon_t: 0x000000011C47FB38:
    // 3LKMONOBJECT java/net/URLClassLoader@0x0700000000F829F8/0x0700000000F82A10: Flat locked by "ReplicatedCache|SERVICE_STOPPED" (0x000000017E3A7700), entry count 2    
    // so the given nameId might be really only just the name
    Iterator<String> keys = threadTable.keySet().iterator();
    while(keys.hasNext()) {
      String key = keys.next();
      if (key.contains(nameId))
        return threadTable.get(key);
    }
    return ti;
  }

  public LockInfo findLock(String lockId) {
    return lockTable.get(lockId);
  }

  public void addLock(LockInfo lock) {
    // Check if Lock is already registered...
    if (lockTable.contains(lock.getLockId()))
      return;

    lockTable.put(lock.getLockId(), lock);

    ThreadInfo lockOwner = lock.getLockOwner();
    theLogger.finest("********************Lock Registered..." +
     lock.getLockId() + ", hashCode: " + lock.hashCode()
     + ", owner: " + ((lockOwner == null)? "null":lockOwner.getNameId()) +
     ", blocked:" + lock.getBlockers().size());
  }

  public ThreadInfo getLockOwner(String lock) {
    theLogger.finest("Lock searched for: " + lock);
    theLogger.finest("Lock Table size: " + lockTable.size());
    Enumeration<String> keys = lockTable.keys();
    while(keys.hasMoreElements()) {
      String key = keys.nextElement();
      theLogger.finest("Lock : " + key + ", lock:" + lockTable.get(key));
    }
    theLogger.finest("Lock: " + lockTable.get(lock));
    return this.lockTable.get(lock).getLockOwner();
  }

  public boolean detectDeadlock() {
    if (this.hasDeadlock && deadlockEntry != null)
      return true;

    LockInfo[] lockArr = lockTable.values().toArray(new LockInfo[] {});
    for (LockInfo lock : lockArr) {
      lock.setParentThreadDump(this);
      ThreadAdvisory.runLockInfoAdvisory(lock);
    }

    deadlockEntry = LockInfo.detectDeadlock(lockArr);
    if (deadlockEntry != null) {
      ThreadAdvisory deadlockAdvisory = ThreadAdvisory.getDeadlockAdvisory();

      this.hasDeadlock = true;
      this.deadLockMsg = deadlockEntry.getDeadlockMsg();
      this.addAdvisory(deadlockAdvisory);
    }

    return this.hasDeadlock;
  }

  public Category getDeadlocks() {
    return deadlocks;
  }

  public void setDeadlocks(Category deadlocks) {
    this.deadlocks = deadlocks;
  }

  private Analyzer getDumpAnalyzer() {
    if (dumpAnalyzer == null) {
      setDumpAnalyzer(new Analyzer(this));
    }
    return dumpAnalyzer;
  }

  private void setDumpAnalyzer(Analyzer dumpAnalyzer) {
    this.dumpAnalyzer = dumpAnalyzer;
  }

  public int getOverallThreadsWaitingWithoutLocksCount() {
    return overallThreadsWaitingWithoutLocksCount;
  }

  public void setOverallThreadsWaitingWithoutLocksCount(int overallThreadsWaitingWithoutLocksCount) {
    this.overallThreadsWaitingWithoutLocksCount = overallThreadsWaitingWithoutLocksCount;
  }

  /**
   * add given category to the custom category.
   * 
   * @param cat
   */
  public void addToCustomCategories(Category cat) {

  }

  /**
   * get the set heap info
   * 
   * @return the set heap info object (only available if the thread dump is from
   *         Sun JDK 1.6 so far.
   */
  public HeapInfo getHeapInfo() {
    return (heapInfo);
  }

  /**
   * set the heap information for this thread dump.
   * 
   * @param value
   *          the heap information as string.
   */
  public void setHeapInfo(HeapInfo value) {
    heapInfo = value;
  }

  /**
   * string representation of this node, is used to displayed the node info in
   * the tree.
   * 
   * @return the thread dump information (one line).
   */
  public String toString() {
    StringBuffer postFix = new StringBuffer();
    if (logLine > 0) {
      postFix.append(" at line " + getLogLine());
    }
    if ((startTime != null) && (startTime != null)) {
      postFix.append(" around " + startTime);
    }
    return (getName() + postFix);
  }

  public void runThreadsAdvisory() {    
    
    // Speed up individual thread analysis using concurrent executors...
    int totalThreads = this.threadTable.values().size();
    final AtomicInteger processedThreads = new AtomicInteger(0);
    for (final ThreadInfo ti : this.threadTable.values()) {
      fixedPoolExecutor.execute( 
              new Runnable() { 
                public void run() { ti.runAdvisory(); processedThreads.incrementAndGet(); }
              } );
    }
    
    do {
      try {
      Thread.sleep(100);
      } catch(InterruptedException ie) {}
    } while (totalThreads > processedThreads.get());
    
    this.threadList = sortByHealth(this.threadList);    
  }  
  
  public synchronized void runAdvisory() {

    detectDeadlock();

    for (ThreadGroup group : this.threadGroupTable.values()) {
      // group.runAdvisory();
      for (ThreadAdvisory advisory : group.getCritAdvisories()) {        
          this.addAdvisory(advisory);
      }
    }

    for (ThreadInfo ti : this.threadTable.values()) {
      switch (ti.getState()) {
      case BLOCKED:
        ++noOfBlockedThreads;
        break;
      case RUNNING:
        if (!ti.getTGroup().contains("JVM"))
          setNoOfRunningThreads(getNoOfRunningThreads() + 1);
        break;
      }
    }

    this.noOfGroups = this.threadGroupTable.size();
    this.noOfThreads = this.threadTable.size();
    for (LockInfo lock : getLockTable().values()) {
      if (lock.getBlockers().size() > 0)
        ++noOfLocks;
    }

    this.health = HealthLevel.NORMAL;
    this.advisories = ThreadAdvisory.sortByHealth(this.advisories);
    if (this.advisories.size() > 0 && this.advisories.get(0).getHealth().ordinal() > this.health.ordinal()) {
      this.health = this.advisories.get(0).getHealth();
    }

    this.threadList = ThreadInfo.sortByHealth(this.threadList);
    this.threadTable.clear();
    for (ThreadInfo ti : threadList) {
      theLogger.finest("Saving inside ThreadMap: threadName: " + ti.getName() + ", NameId is: " + ti.getNameId());
      this.threadTable.put(ti.getNameId(), ti);
    }
    theLogger.finest("ThreadDump[" + this.name + "] blocked:" +
     noOfBlockedThreads + ", running:" + noOfRunningThreads + ", noOfLocks:" +
     noOfLocks);
  }

  /**
   * creates the overview information for this thread group.
   */
  public String getTGSummaryOverview() {

    this.runAdvisory();

    StringBuffer statData = new StringBuffer("<font face=System "
        + "><table border=0><tr bgcolor=\"#dddddd\" ><td><font face=System "
        + ">Thread Dump Name</td><td width=\"150\"><b><font face=System>");
    statData.append(this.getName());
    statData.append("<tr bgcolor=\"#ffffff\"><td></td></tr>");
        
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Thread Dump Main Thread </td><td><b><font face=System size>");
    statData.append(this.mainThread);

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Thread Dump Health </td><td><b><font face=System size>");
    
    String color = this.health.getBackgroundRGBCode();
    statData.append("<p style=\"background-color:" + color + ";\">" + this.health + "</p>");

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Deadlock Found </td><td><b><font face=System size>");
    statData.append(this.hasDeadlock);

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Thread Groups </td><td><b><font face=System size>");
    statData.append(this.threadGroupTable.size());

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Total Number of threads </td><td><b><font face=System size>");
    statData.append(this.threadTable.size());

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Number of Locks (with threads blocked)</td><td><b><font face=System size>");
    statData.append(this.noOfLocks);

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Number of blocked threads</td><td><b><font face=System size>");
    statData.append(this.noOfBlockedThreads);

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Number of running threads</td><td><b><font face=System>");
    statData.append(this.getNoOfRunningThreads());
    
    if (this.isParsedWithFBParser()) {    
      statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Was generated via WLST</td><td></td><td><b><font face=System+1>");    
      statData.append("<p><font style=color:Red><b> YES </b></font><p><br>");
    }   
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#ffffff\"><td></td></tr></table>");

    statData.append("<font face=System><table border=0>");
    
    if (this.isParsedWithFBParser()) {
      
      statData.append("<tr bgcolor=\"#cccccc\" ><td colspan=2><font face=System"
          + "><p><font style=color:Red><b>WARNING!!!</b></font><p><br>");
      
      statData.append("<font style=color:Red>WLST or WLS Admin Console generated thread dumps wont indicate Thread IDs or locking information between threads <br>");
      statData.append("(except for JRockit). ThreadLogic won't be able to analyze or report existence of deadlocks or other blocked conditions, <br>");
      statData.append("bottlenecks due to missing lock data. Also WLST might not be successful if server is in hung situation<br><br>");
      statData.append("Strongly Recommendation: Use other system options (kill -3 or jrcmd or jstack) to generate thread dumps for real monitor/lock information and detailed analysis!!");
      statData.append("</font><br></p></td></tr>");
      statData.append("<tr bgcolor=\"#ffffff\"><td></td></tr>");
    }
    
    if (this.hasDeadlock) {
      statData.append("<tr bgcolor=\"#cccccc\" ><td colspan=2><font face=System"
          + "><p><font style=color:Red><b>Deadlock Found !!!</b></font><p><br>");
      statData.append(LockInfo.printDeadlockChain(this.deadlockEntry.getDeadlockChain()) + "<br>");
      statData
          .append("<font style=color:Red>Deadlocked threads cannot proceed without killing the threads or restarting the JVM<p><br>");
      statData
          .append("Analyze the reasons for deadlock - it could be caused by wrong order of obtaining locks or unnecessary synchronization<br>");
      statData
          .append("Reduce contentions by changing code to avoid synchronized blocks, or change invocation path, or increase resources ");
      statData.append("or caching as well as modifying the order of locking.</font><br></td></tr>");
      statData.append("<tr bgcolor=\"#ffffff\"><td></td></tr>");
    }

    int percentageRunning = (int) (getNoOfRunningThreads() * 100.0 / this.threadTable.size());
    if (percentageRunning != 0) {

      statData.append("<tr bgcolor=\"#cccccc\" ><td colspan=2><font face=System" + "><p>" + percentageRunning
          + "% of threads are running Healthy (not waiting or blocked).</p>");
      statData.append("</td></tr>");
      statData.append("<tr bgcolor=\"#ffffff\"><td></td></tr>");
    }

    int percentageBlocked = (int) (noOfBlockedThreads * 100.0 / this.threadTable.size());
    if (percentageBlocked != 0) {

      statData.append("<tr bgcolor=\"#cccccc\" ><td colspan=2><font face=System" + "><p>" + percentageBlocked
          + "% of threads are Blocked.</p>");
      if (percentageBlocked > 30) {
        statData
            .append("<font style=color:Red> This would indicate heavily synchronized code and contention among threads for single or multiple locks<br>");
        statData
            .append("Would be good to identify  and reduce contentions by changing code to avoid synchronized blocks, or change invocation path, or increase resources or caching</font><br></td></tr>");
      }
      statData.append("</td></tr>");
      statData.append("<tr bgcolor=\"#ffffff\"><td></td></tr>");
    }

    ArrayList<ThreadAdvisory> critList = getCritAdvisories();
    if (critList.size() > 0) {
      statData.append("<tr bgcolor=\"#cccccc\" ><td colspan=2><font face=System>"
          + "<b>Critical Advisories (WATCH, WARNING or FATAL levels) Found</b></td></tr>");

      for (ThreadAdvisory advisory : critList) {
        statData.append("\n\n<tr bgcolor=\"#ffffff\"><td></td></tr>");
        statData.append(advisory.getOverview());
      }
    }

    statData.append("<tr bgcolor=\"#ffffff\"><td></td></tr>");
    statData.append("</table>");

    return statData.toString();
  }

  public void setThreadGroups(Collection<ThreadGroup> threadGroups) {
    for (ThreadGroup tg : threadGroups) {
      threadGroupTable.put(tg.getThreadGroupName(), tg);
      this.threadGrpList.add(tg);
    }

    sortByHealth(this.threadGrpList);
  }

  public Hashtable<String, LockInfo> getLockTable() {
    return lockTable;
  }

  public void setLockTable(Hashtable<String, LockInfo> lockTable) {
    this.lockTable = lockTable;
  }

  public Hashtable<String, ThreadGroup> getThreadGroupTable() {
    return threadGroupTable;
  }

  public boolean hasDeadlock() {
    return hasDeadlock;
  }

  public Collection<ThreadInfo> getDeadlockedThreads() {
    if (this.hasDeadlock)
      return this.deadlockEntry.getDeadlockChain();
    return new ArrayList<ThreadInfo>() {
    };
  }

  public String getDeadlockedInfo() {
    if (!this.hasDeadlock)
      return "";

    Collection<ThreadInfo> deadLockedThreads = this.getDeadlockedThreads();
    StringBuffer sbuf = new StringBuffer("<table width=1200><tr bgcolor=\"#cccccc\" >");
    sbuf.append("<td colspan=3><font face=System><p><font style=color:Red>");
    sbuf.append("<b>Deadlock Found !!!</b></font><p><br>");
    
    sbuf.append(LockInfo.printDeadlockChain(deadLockedThreads));
    sbuf.append("<br></td>\n\n<tr bgcolor=\"#ffffff\"><td></td></tr></table>");

    sbuf.append("<table><tr><td>Associated Java Thread Stacks<hr><br></td></tr>");
    for(ThreadInfo ti: deadLockedThreads) {
      sbuf.append("<tr><td> <b>");
      sbuf.append(ti.getFilteredName());      
      sbuf.append(":</b><br>");
      sbuf.append(ti.getContent());
      sbuf.append("<br></td></tr>");
    }
    sbuf.append("</table>");    
    return sbuf.toString();
  }

  /**
   * @return the jvmVersion
   */
  public String getJvmVersion() {
    return jvmVersion;
  }

  /**
   * @param jvmVersion the jvmVersion to set
   */
  public void setJvmVersion(String jvmVersion) {
    this.jvmVersion = jvmVersion;
  }

  /**
   * @return the jvmType
   */
  public String getJvmType() {
    return jvmType;
  }

  /**
   * @param jvmType the jvmType to set
   */
  public void setJvmType(String jvmType) {
    this.jvmType = jvmType;
  }

  /**
   * @return the parsedWithVendorAgnosticParser
   */
  public boolean isParsedWithFBParser() {
    return parsedWithFBParser;
  }

  /**
   * @param parsedWithVendorAgnosticParser the parsedWithVendorAgnosticParser to set
   */
  public void setParsedWithFBParser(boolean parsedWithFBParser) {
    this.parsedWithFBParser = parsedWithFBParser;
  }
  
  /**
   * @return the logFile
   */
  public Logfile getLogFile() {
    return logFile;
  }

  /**
   * @param logFile the logFile to set
   */
  public void setLogFile(Logfile logFile) {
    this.logFile = logFile;
  }

  /**
   * @return the noOfRunningThreads
   */
  public int getNoOfRunningThreads() {
    return noOfRunningThreads;
  }

  /**
   * @param noOfRunningThreads the noOfRunningThreads to set
   */
  public void setNoOfRunningThreads(int noOfRunningThreads) {
    this.noOfRunningThreads = noOfRunningThreads;
  }
  
  public static void shutdownExecutor() {
    if (fixedPoolExecutor != null)
      fixedPoolExecutor.shutdown();
  }
  
  public void addThreadContextData(String id, String contextData) {
    if (id != null)
      this.threadContextDataMap.put(id, contextData);
  }
  
  
  public String getThreadContextData(String id) {
    if (id == null)
      return null;
    
    return this.threadContextDataMap.get(id);
  }
}
