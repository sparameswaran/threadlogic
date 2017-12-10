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
package com.oracle.ateam.threadlogic.advisories;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;


import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.ThreadLogicElement;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.ThreadState;
import com.oracle.ateam.threadlogic.utils.CustomLogger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ThreadGroup extends ThreadLogicElement {

  private static Logger theLogger = CustomLogger.getLogger(ThreadGroup.class.getSimpleName());

  public class HotCallPattern implements Serializable {
    String threadPattern;
    ArrayList<ThreadInfo> threads;

    public String geThreadPattern() {
      return threadPattern;
    }

    public ArrayList<ThreadInfo> geThreads() {
      return threads;
    }

  }

  protected String overview;
  protected int blockedThreads, runningThreads, bpelInvokeThreads, b2bExecutorThreads, bpelEngineThreads,
      soaJMSConsumerThreads;
  protected String threadGroupName;

  protected boolean isJVMThreadGroup, isWLSThreadGroup;
  private boolean advisoryRun = false;

  protected ArrayList<ThreadInfo> threads = new ArrayList<ThreadInfo>();  
  protected ArrayList<ThreadInfo> waitingThreads = new ArrayList<ThreadInfo>();
  protected ArrayList<ThreadInfo> sleepingThreads = new ArrayList<ThreadInfo>();
  protected ArrayList<ThreadInfo> lockingThreads = new ArrayList<ThreadInfo>();
  protected ArrayList<ThreadAdvisory> exclusionList = new ArrayList<ThreadAdvisory>();
  
  protected ArrayList<HotCallPattern> hotPatternList = new ArrayList<HotCallPattern>();

  public String getId() {
    return this.threadGroupName;
  }

  public ThreadGroup(String grpName) {
    super(grpName);
    this.threadGroupName = grpName;    
    init();
  }
  
  protected void init() {    
  }
  
  public int getGroupSize() {
    return this.threads.size();
  }
  
  public ArrayList<ThreadAdvisory> getExclusionList() {
    return this.exclusionList;
  }
  
  public void setExclusionList(ArrayList<ThreadAdvisory> list) {
    this.exclusionList = list;
  }

  public void addToExclusionList(ThreadAdvisory tadv) {
    if (!this.exclusionList.contains(tadv))
      exclusionList.add(tadv);
  }
  
  public void addToExclusionList(ArrayList<ThreadAdvisory> list) {
    for (ThreadAdvisory tadv: list) {
      if (!this.exclusionList.contains(tadv))
        exclusionList.add(tadv);
    }
  }
  
  public boolean wasAdvisoryRun() {
    return advisoryRun;
  }

  public void setAdvisoryRun() {
    this.advisoryRun = true;
  }

  public void runAdvisory() {
    
    if (wasAdvisoryRun()) {
      return;
    }
    
    boolean tooManyThreads = this.threads.size() > ThreadLogicConstants.TOO_MANY_THREADS_LIMIT;
    ThreadAdvisory tooManyThreadsAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.TOO_MANY_THREADS);
    
    if (tooManyThreads) {      
      this.addAdvisory(tooManyThreadsAdvisory);
      this.setHealth(tooManyThreadsAdvisory.getHealth());
    }
    
    Hashtable<String, HotCallPattern> threadStackCache = new Hashtable<String, HotCallPattern>();
    String threadGroupNameLower = this.threadGroupName.toLowerCase();

    int size = this.getGroupSize();
    Hashtable<ThreadAdvisory, AtomicInteger> watchAdvisoryHitList = new Hashtable<ThreadAdvisory, AtomicInteger>();
    
    
    for (ThreadInfo tholder : this.threads) {

      //if (tooManyThreads)
      //  tholder.addAdvisory(tooManyThreadsAdvisory);
      
      // Downgrade the health level of advisories that are specified in the Exclusion list
      if (exclusionList != null && exclusionList.size() > 0) {
        tholder.recalibrateHealthForExcludedAdvisories(HealthLevel.NORMAL, exclusionList);
      }

      if (tholder.getState() == ThreadState.BLOCKED) {
        ++blockedThreads;
      } else if (tholder.getState() == ThreadState.RUNNING) {
        ++runningThreads;
      }
      
      // Ignore the thread label/top few lines that might vary between
      // threads...
      // Get the rest of the threads and check for repeat occurence of the same
      // stack pattern

      // If the thread call stack is not deep enough, the subset of thread dump
      // will be returned as null form getThreaddumpSubset() call
      // In case those cases, ignore checking for repeat pattern as the calls
      // most oftne would be routine/idle/housekeeping, no application related.

      // Get a snapshot of the thread stack that has atleast
      // MIN_THREADSTACK_LEN_TO_CONSIDER stack depth
      // Start from offset of THREAD_STACK_OFFSET and grab the next
      // MAX_THREADSTACK_LEN_SUBSET lines
      String threadDumpSubset = ThreadAdvisory.getThreaddumpSubset(tholder,
          ThreadLogicConstants.MIN_THREADSTACK_LEN_TO_CONSIDER, ThreadLogicConstants.THREAD_STACK_OFFSET,
          ThreadLogicConstants.MAX_THREADSTACK_LEN_SUBSET);
      
      if (threadDumpSubset != null) {

        HotCallPattern hotCallPattern = null;
        if (threadStackCache.containsKey(threadDumpSubset)) {
          hotCallPattern = threadStackCache.get(threadDumpSubset);
          hotCallPattern.threads.add(tholder);
        } else {
          hotCallPattern = new HotCallPattern();
          hotCallPattern.threads = new ArrayList<ThreadInfo>();
          hotCallPattern.threads.add(tholder);
          hotCallPattern.threadPattern = threadDumpSubset;
          threadStackCache.put(threadDumpSubset, hotCallPattern);
        }
      }

      HealthLevel tiHealth = tholder.getHealth();
      for (ThreadAdvisory savedAdvisory : tholder.getAdvisories()) {

        HealthLevel state = savedAdvisory.getHealth();

        // Add the advisory if its above or equal to WARNING
        if (state.ordinal() >= HealthLevel.WARNING.ordinal() && !savedAdvisory.getPattern().contains("Sleep")) {
          this.addAdvisory(savedAdvisory);
        } else if (state == HealthLevel.WATCH) {
          // If the advisory is at WATCH level, add to the hit list
          // If large # of threads display same advisory, set to WARNING at the Thread Group level.
          AtomicInteger counter = watchAdvisoryHitList.get(savedAdvisory);
          if (counter == null) {
            counter = new AtomicInteger(0);
            watchAdvisoryHitList.put(savedAdvisory, counter);
          }
          counter.incrementAndGet();          
        }
      }

      // If the thread's health is at a higher level, set the thread group
      // health to that higher level
      // Not directly going with the Advisory default health level as we
      // downgrade level for muxer/adapters...
      // So compare against the thread's health instead of advsiory.
      if (tiHealth.ordinal() > this.health.ordinal()) {
        this.health = tiHealth;
      }
    }

    // If large # of threads display same advisory, set to WARNING at the Thread Group level.
    for(ThreadAdvisory watchLevelAdvisory: watchAdvisoryHitList.keySet()) {
      if (watchAdvisoryHitList.get(watchLevelAdvisory).get() > ThreadLogicConstants.HOT_CALL_MIN_OCCURENCE)
        watchLevelAdvisory.setHealth(HealthLevel.WARNING);
        this.addAdvisory(watchLevelAdvisory);      
    }
    
    theLogger.finest("*************** No of Hot Patterns:" +
     threadStackCache.keySet().size());
    for (String key : threadStackCache.keySet()) {
      HotCallPattern hotCallPattern = threadStackCache.get(key);
      int noOfHits = hotCallPattern.threads.size();

      theLogger.finest("*************** Hot Patterns - hit:" + noOfHits + ", pattern: " + hotCallPattern.geThreadPattern());
      
      // If similar pattern is seen frequently but is not associated with Muxer
      // or JVM GC threads, then flag that as worth WATCHing
      if (!threadGroupNameLower.contains("muxer") && !threadGroupNameLower.contains("jvm")
              && !threadGroupNameLower.contains("oracle ons")
              && (noOfHits >= ThreadLogicConstants.HOT_CALL_MIN_OCCURENCE)) {

        ThreadAdvisory hotThreadsAdvisory = ThreadAdvisory.getHotPatternAdvisory();

        // Dont add this to the general advisories as this gets treated like the
        // standard advisory ThreadLogicConstants.HOT_CALL_PATTERN
        // Difficult to display the thread ids...
        this.addAdvisory(hotThreadsAdvisory);        

        this.hotPatternList.add(hotCallPattern);
        theLogger.finest(this.threadGroupName + ": Added a Hot call Pattern:" +
          hotCallPattern.threadPattern);
      }
    }

    threadStackCache.clear();

    // Ignore blocked threads if its with the Muxer as its normal for 1 thread
    // to hold lock while others are blocked.
    if (blockedThreads > 5 && !threadGroupNameLower.contains("muxer")) {
      this.addAdvisory(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.BLOCKED_THREADS));
    }

    this.threads = ThreadInfo.sortByHealth(this.threads);
    this.advisories = ThreadAdvisory.sortByHealth(this.advisories);
    this.setAdvisoryRun();
  }  
  
  /**
   * get the overview information of this thread groups.
   * 
   * @return overview information.
   */
  public String getOverview() {
    if (overview == null) {
      createOverview();
    }
    return overview;
  }

  /**
   * get the overview information of this thread groups.
   * 
   * @return overview information.
   */
  public void setOverview(String overview) {
    this.overview = overview;
  }

  /**
   * creates the overview information for this thread group.
   */
  protected void createOverview() {
    setOverview( getBaseOverview() + getEndOfBaseOverview() + getCritOverview());
  }
  
  protected String getBaseOverview() {
    StringBuffer statData = new StringBuffer("<font face=System "
        + "><table border=0><tr bgcolor=\"#dddddd\" ><td><font face=System "
        + ">Thread Group Name</td><td><b><font face=System>");
    statData.append(this.getThreadGroupName());

    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Thread Group Health </td><td><b><font face=System size>");
    
    String color = this.health.getBackgroundRGBCode();
    statData.append("<p style=\"background-color:" + color + ";\">" + this.health + "</p>");

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Total Number of threads </td><td><b><font face=System size>");
    statData.append(this.threads.size());

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Number of threads blocked for locks</td><td><b><font face=System size>");
    statData.append(getNoOfBlockedThreads());

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Number of busy (not waiting or blocked) threads </td><td><b><font face=System>");
    statData.append(getNoOfRunningThreads());

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Number of Hot Patterns Found </td><td><b><font face=System>");
    statData.append(this.hotPatternList.size());

    statData.append("</b></td></tr>\n\n");    
    return statData.toString();
  }
  
  protected String getEndOfBaseOverview() {
    return "</table>";
  }
  
  protected String getCritOverview() {
    
    StringBuffer statData = new StringBuffer("<font face=System "
        + "><table border=0>");
    
    statData.append("<tr bgcolor=\"#ffffff\"><td></td></tr>");

    int percentageRunning = (int) (getNoOfRunningThreads() * 100.0 / this.threads.size());
    if (percentageRunning != 0) {

      statData.append("<tr bgcolor=\"#cccccc\" ><td colspan=2><font face=System" + "><p>" + percentageRunning
          + "% of threads are running Healthy (not waiting or blocked).</p>");
      statData.append("</td></tr>");
      statData.append("<tr bgcolor=\"#ffffff\"><td></td></tr>");
    }

    int percentageBlocked = (int) (getNoOfBlockedThreads() * 100.0 / this.threads.size());
    if (!this.getThreadGroupName().contains("Muxer") && percentageBlocked != 0) {

      statData.append("<tr bgcolor=\"#cccccc\" ><td colspan=2><font face=System" + "><p>" + percentageBlocked
          + "% of threads are blocked.</p><br>");
      if (percentageBlocked > 30) {
        statData
            .append("<font style=color:Red> This would indicate heavily synchronized code and contention among threads for single or multiple locks.<br>");
        statData
            .append("Would be good to identify  and reduce contentions by changing code to avoid synchronized blocks, or change invocation path, or");
        statData
            .append(" change constraints to increase resource availability or via caching as applicable</font><br></td></tr>");
      }
      statData.append("</td></tr>");
      statData.append("<tr bgcolor=\"#ffffff\"><td></td></tr>");
    }

    if (getHotCallPatternPercentage() > 30) {
      statData
          .append("<tr bgcolor=\"#cccccc\" ><td colspan=2><font face=System"
              + "><p>"
              + getHotCallPatternPercentage()
              + "% of Threads in this thread group exhibit a pattern of executing same code paths tagged as Hot Patterns.</p><br>");
      statData
          .append("<font style=color:Red>This implies multiple threads are executing the same code path and can be affected by locks,<br>");
      statData.append(" synchronization, resource constraints or limits as part of the same code path execution.</font><br></td></tr>");

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

  public int getHotCallPatternPercentage() {
    double hotCalls = 0;

    for (HotCallPattern hotpattern : this.getHotPatterns()) {
      hotCalls += hotpattern.threads.size();
    }

    return (int) ((hotCalls * 100.0) / this.threads.size());
  }

  public String getThreadGroupName() {
    return threadGroupName;
  }

  public void setThreadGroupName(String threadGroupName) {
    this.threadGroupName = threadGroupName;
  }

  public ArrayList<ThreadInfo> getThreads() {
    return threads;
  }

  public void setThreads(ArrayList<ThreadInfo> threads) {
    this.threads = threads;
  }

  public void addThread(ThreadInfo thread) {
    this.threads.add(thread);
  }

  public void addThreads(ThreadGroup thGroup) {
    if (!thGroup.threadGroupName.equals(this.threadGroupName))
      return;

    this.threads.addAll(thGroup.threads);
  }

  public void addThreads(ThreadInfo[] threads) {
    for (ThreadInfo tholder : threads)
      this.threads.add(tholder);
  }

  public void addThreads(ArrayList<ThreadInfo> threads) {
    this.threads.addAll(threads);
  }

  



  public void removeThread(ThreadInfo thread) {
    this.threads.remove(thread);
  }

  public int getNoOfBlockedThreads() {
    return blockedThreads;
  }

  public void setNoOfBlockedThreads(int blocked) {
    this.blockedThreads = blocked;
  }

  public int getNoOfRunningThreads() {
    return runningThreads;
  }

  public void setNoOfRunningThreads(int running) {
    this.runningThreads = running;
  }

  public ArrayList<HotCallPattern> getHotPatterns() {
    return hotPatternList;
  }

  public ArrayList<ThreadAdvisory> getAdvisories() {
    return ThreadAdvisory.sortByHealth(advisories);
  }

}
