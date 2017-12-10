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

import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.LockInfo;
import com.oracle.ateam.threadlogic.ThreadState;
import com.oracle.ateam.threadlogic.utils.CustomLogger;
import com.oracle.ateam.threadlogic.xml.AdvisoryMapParser;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadAdvisory implements Comparable, Serializable {

  String keyword, descrp, advice, callPattern, group;
  private String[] keywordList = new String[1];
  HealthLevel health;

  public static String ADVISORY_PATH_SEPARATOR = "|";
  public static String ADVISORY_EXT_DIRECTORY = "threadlogic.advisories";

  public static String DICTIONARY_KEYS;
  public static String THREADTYPEMAPPER_KEYS;

  public static final ArrayList<String> wildcardKeywordList = new ArrayList<String>();
  
  public static final Hashtable<String, ThreadAdvisory> threadAdvisoryMap = new Hashtable<String, ThreadAdvisory>();
  public static final Hashtable<String, ThreadAdvisory> threadAdvisoryMapById = new Hashtable<String, ThreadAdvisory>();

  private static Logger theLogger = CustomLogger.getLogger(ThreadAdvisory.class.getSimpleName());
  
  static {
    DICTIONARY_KEYS = createAdvisoryMapFromExternalResources();
    String internalKeys = createAdvisoryMapFromInternalResources(ThreadLogicConstants.ADVISORY_MAP_XML);
    
    if ((DICTIONARY_KEYS.length() > 0) &&  (internalKeys.length() > 0)) {
      DICTIONARY_KEYS = DICTIONARY_KEYS + "|" + internalKeys;
    } else if (DICTIONARY_KEYS.length() == 0) {
      DICTIONARY_KEYS = internalKeys;
    }
    theLogger.fine("Complete keyword patterns: " + DICTIONARY_KEYS);
  }

  private static String createAdvisoryMapFromExternalResources() {
    
    AdvisoryMapParser advisoryMapParser = null;      
    ArrayList<ThreadAdvisory> list = null; 

    boolean empty = true;
    StringBuffer sbuf = new StringBuffer();      
    String externalAdvisoryDirectory = System.getProperty(ADVISORY_EXT_DIRECTORY, "advisories");
      File folder = new File(externalAdvisoryDirectory);
      if (folder.exists()) {              
        File[] listOfFiles = folder.listFiles();
        for(File file: listOfFiles) {
        try {
          theLogger.info("\n\nReading advisories from External resources: " + file.getAbsolutePath()+ "\n");
          BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
          advisoryMapParser = new AdvisoryMapParser(bis);
          advisoryMapParser.run();          
          bis.close();
          list = advisoryMapParser.getAdvisoryList(); 
          String keywordList = populateAdvisories(list);
          if (empty) {
            empty = false;
            sbuf.append(keywordList);          
          } else {
            sbuf.append("|" + keywordList);            
          }
        } catch(Exception ioe) {
          theLogger.warning("Problem in reading advisories from file: " + externalAdvisoryDirectory);
          ioe.printStackTrace();
        }
      }  
    }
      
    return sbuf.toString();      
  }

  private static String createAdvisoryMapFromInternalResources(String advisoryMapXml) {
    AdvisoryMapParser advisoryMapParser = null;      
    ArrayList<ThreadAdvisory> list = null; 

    boolean empty = true;
    StringBuffer sbuf = new StringBuffer();     
    
    try {
      theLogger.fine("\n\nAttempting to load Advisory Map from packaged threadlogic jar: " + advisoryMapXml + "\n");
      ClassLoader cl = ThreadLogicConstants.class.getClassLoader();

      advisoryMapParser = new AdvisoryMapParser(cl.getResourceAsStream(advisoryMapXml));
      advisoryMapParser.run();
      list = advisoryMapParser.getAdvisoryList(); 
      String keywordList = populateAdvisories(list);

      if (empty) {
        empty = false;
        sbuf.append(keywordList);          
      } else {
        sbuf.append("|" + keywordList);
      }
      
    } catch (Exception e) {
      theLogger.warning("Unable to load or parse the Advisory Map Resource:" + e.getMessage());
      e.printStackTrace();
    }
    
    return sbuf.toString();
  }

  /**
   * 
   * @param list List of ThreadAdvisories to be populated into known Advisories
   * @return String of keywords with | as separator for pattern matching
   */
  protected static String populateAdvisories(ArrayList<ThreadAdvisory> list) {
    boolean empty = true;
    StringBuffer sbuf = new StringBuffer(1000);
    
      for(ThreadAdvisory tadv: list) {
        String key = tadv.getKeyword();
        
        if (threadAdvisoryMap.containsKey(key)) {
          theLogger.warning("WARNING!! Keyword already exists:" + key + ", use different keyword or update existing Advisory");
          continue;
        }
        
        theLogger.finest("Parsed Advisory: " + tadv);
    
        threadAdvisoryMap.put(key, tadv);
        if (key.contains("*"))
          wildcardKeywordList.add(key);
        threadAdvisoryMapById.put(tadv.getPattern(), tadv);
        
        if (!empty)
          sbuf.append("|");
        
        sbuf.append("(" + key + ")");
        empty = false;
        
        int noOfKeywords = tadv.getKeywordList().length;
        if (noOfKeywords > 1) {
          for(int i = 1; i < noOfKeywords; i++) {
            key = tadv.getKeywordList()[i];
            if (threadAdvisoryMap.containsKey(key)) {
              theLogger.warning("WARNING!! Keyword already exists:" + key + " from Advisory:" + threadAdvisoryMap.get(key) + ", use different keyword or update existing Advisory");
              continue;
            }
            threadAdvisoryMap.put(key, tadv);
            if (key.contains("*"))
              wildcardKeywordList.add(key);
            sbuf.append("|(" + key + ")");
          }
        }
      }
      // Return the keyword combination for pattern matching
      return sbuf.toString();
    
  }
  
  public ThreadAdvisory() {
  }

  public ThreadAdvisory(String advisory) {
    String parts[] = advisory.split("#");

    this.keywordList[0] = this.keyword = parts[0];
    parseCompleteAdvice(parts[1]);
  }

  public ThreadAdvisory(ThreadAdvisory advisory) {
    
    this.keywordList[0] = this.keyword = advisory.keyword;
    this.callPattern = advisory.callPattern;
    this.descrp = advisory.descrp;
    this.health = advisory.health;
    this.advice = advisory.advice;    
    this.keywordList = advisory.keywordList;
  }
  
  public ThreadAdvisory(String keyword, HealthLevel health, String pattern, String descrp, String advice) {
    
    this.keywordList[0] = this.keyword = keyword;
    this.callPattern = pattern;
    this.descrp = descrp;
    this.health = health;
    this.advice = advice;
  }

  public ThreadAdvisory(String keyword, String completeAdvice) {
    this.keywordList[0] = this.keyword = keyword;
    parseCompleteAdvice(completeAdvice);
  }

  private void parseCompleteAdvice(String completeAdvice) {
    String[] tokens = completeAdvice.split("\\|");
    this.callPattern = tokens[0];
    descrp = tokens[1];
    health = HealthLevel.valueOf(tokens[2]);
    advice = tokens[3];
  }

  public String toString() {
    StringBuffer sbuf = new StringBuffer(100);
    sbuf.append("[Advice: ");
    sbuf.append(callPattern);
    sbuf.append(", Keyword: ");
    sbuf.append(keyword);
    sbuf.append(", Descrp: ");
    sbuf.append(descrp);
    sbuf.append(", Level:");
    sbuf.append(health);
    sbuf.append(", Suggestion: ");
    sbuf.append(advice);
    sbuf.append("]");
    return sbuf.toString();
  }

  public static Collection<ThreadAdvisory> getAdvisoryList() {
    ArrayList<ThreadAdvisory> list = new ArrayList<ThreadAdvisory>();
    for (ThreadAdvisory entry : threadAdvisoryMapById.values())
      list.add(entry);
    return sortByHealth(list);
  }

  public static ThreadAdvisory lookupThreadAdvisory(String key) {    
    
    ThreadAdvisory readOnlyAdvisory = threadAdvisoryMap.get(key);
    if (readOnlyAdvisory != null)
      return new ThreadAdvisory(readOnlyAdvisory);
    
    /** If a multiline wild card pattern was actually used, the m.group() might return something vastly different from the pattern keyword:
     * For example:WsCalloutRuntimeStep.*StageMetadataImpl
     * would match against:
    
     * WsCalloutRuntimeStep$WsCalloutDispatcher.dispatch(WsCalloutRuntimeStep.java:1391)
     * at stages.transform.runtime.WsCalloutRuntimeStep.processMessage(WsCalloutRuntimeStep.java:236)
	   * at com.bea.wli.sb.stages.StageMetadataImpl$WrapperRuntimeStep.processMessage(StageMetadataImpl
     
     * In those cases, re-run the pattern so we can truly identify which one really matches..
     * use the wildcard key list instead of going against the full dictionary key set.
     */
    
    for(String wildcardKey: wildcardKeywordList) {      
      
      Pattern p = Pattern.compile(wildcardKey, Pattern.DOTALL);
      Matcher m = p.matcher(key);
      if (m.find()) {
        // Found match of the wild card key....
        readOnlyAdvisory = threadAdvisoryMap.get(wildcardKey);
        return new ThreadAdvisory(readOnlyAdvisory); 
      }
    }
    return null;
  }

  public static ThreadAdvisory lookupThreadAdvisoryByName(String name) {
    ThreadAdvisory readOnlyAdvisory = threadAdvisoryMapById.get(name);
    if (readOnlyAdvisory != null)
      return new ThreadAdvisory(readOnlyAdvisory);
    
    return null;
  }

  public static ThreadAdvisory getHotPatternAdvisory() {
    return threadAdvisoryMap.get(ThreadLogicConstants.HOT_CALL_PATTERN);
  }

  public static ThreadAdvisory getDeadlockAdvisory() {
    return threadAdvisoryMap.get(ThreadLogicConstants.DEADLOCK_PATTERN);
  }

  public String getAdvice() {
    return advice;
  }

  public void setAdvice(String advice) {
    this.advice = advice;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
    this.keywordList[0] = keyword;
  }

  public String getPattern() {
    return callPattern;
  }

  public void setPattern(String callPattern) {
    this.callPattern = callPattern;
  }

  public HealthLevel getHealth() {
    return health;
  }

  public void setHealth(HealthLevel state) {
    this.health = state;
  }

  public String getDescrp() {
    return descrp;
  }

  public void setDescrp(String descrp) {
    this.descrp = descrp;
  }

  public String getOverview() {
    StringBuffer statData = new StringBuffer("<tr bgcolor=\"#cccccc\"><td><font face=System size=-1"
        + ">Thread Advisory Name</td><td width=\"400\"><b><font face=System>");
    statData.append(this.getPattern());

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System  size=-1"
        + ">Health Level </td><td width=\"400\"><b><font face=System size>");
    
    String color = this.health.getBackgroundRGBCode();
    statData.append("<p style=\"background-color:" + color + ";\">" + this.health + "</p>");


    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System  size=-1"
        + ">Keyword</td><td width=\"400\"><b><font face=System size>");
    for (int i=0; i < keywordList.length; i++) {
      String key = keywordList[i];      
      if (i != 0) {
        statData.append(", ");
      }
      statData.append(key);
    }

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System  size=-1"
        + ">Description</td><td width=\"400\"><b><font face=System size>");
    statData.append(this.descrp);

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System  size=-1"
        + ">Advice </td><td width=\"400\"><b><font face=System>");
    statData.append(this.advice);
    statData.append("</b></td></tr>\n\n");

    return statData.toString();
  }

  public int compareTo(Object o) {
    ThreadAdvisory tadv = (ThreadAdvisory) o;
    return this.health.ordinal() - tadv.health.ordinal();
  }

  public boolean equals(Object o) {
    if (!(o instanceof ThreadAdvisory))
      return false;

    ThreadAdvisory tadv = (ThreadAdvisory) o;
    if (this.callPattern.equals(tadv.callPattern))
      return true;
    
    return this.keyword.equals(tadv.keyword);
  }

  public int hashcode() {
    return (this.callPattern.hashCode() & this.keyword.hashCode());
  }

  public static void runThreadAdvisory(ThreadInfo threadInfo) {

    String threadName = threadInfo.getName();
    String threadStack = threadInfo.getContent();
    ThreadState state = threadInfo.getState();
    
    if (threadInfo.isBlockedForLock() & (threadInfo.getBlockedForLock() != null)) {      
      threadInfo.setHealth(HealthLevel.WATCH);
    }

    boolean isPollerThread = false;
    // Check if the thread is a Poller thread like AQ Adapter or IWay SAP Poller
    for (String pollerPattern : ThreadLogicConstants.POLLERS) {
      if (threadStack.contains(pollerPattern)) {
        isPollerThread = true;
        break;
      }
    }
    
    Pattern vmPattern = Pattern.compile(ThreadAdvisory.DICTIONARY_KEYS, Pattern.DOTALL);
    Matcher m = vmPattern.matcher(threadStack);

    ArrayList<ThreadAdvisory> advisoryList = new ArrayList<ThreadAdvisory>();

    // Poller threads might get marked as STUCK, ignore those thread labels...
    if (threadName.contains(ThreadLogicConstants.STUCK_PATTERN) && !isPollerThread) {
      ThreadAdvisory advisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.STUCK_PATTERN);
      advisoryList.add(advisory);
      threadInfo.setHealth(HealthLevel.FATAL);
    }

    while (m.find()) {
      String keyword = m.group();
      keyword = keyword.replaceAll("/", ".");
      keyword = keyword.replaceAll("\\$", ".");
      keyword = keyword.replaceAll("_", ".");
      
      
      if ( (keyword.contains(ThreadLogicConstants.REENTRANTLOCK_PATTERN)
            || keyword.contains(ThreadLogicConstants.SEMAPHORE_PATTERN))
              && (state == ThreadState.PARKING)) {
        threadInfo.setState(ThreadState.BLOCKED); 
        if (threadInfo.getHealth().ordinal() < HealthLevel.WATCH.ordinal())
          threadInfo.setHealth(HealthLevel.WATCH);
      } else if (keyword.equals(ThreadLogicConstants.STUCK_PATTERN) && isPollerThread) {
        continue;
      }
      
      ThreadAdvisory advisory = ThreadAdvisory.lookupThreadAdvisory(keyword);
      if (advisory == null) {
        theLogger.warning("Unable to find matching advisory with keyword:" + keyword);
      }
      
      if (advisory != null && !advisoryList.contains(advisory))
        advisoryList.add(advisory);
    }

    // Check if the thread is holding a lock and there are multiple other
    // threads waiting for same lock
    // But the owner thread is itself waiting on an event...
    // The event might never occur and all other threads will be blocked
    // forever...

    for (LockInfo lock : threadInfo.getOwnedLocks()) {
      // If the thread is busy in some application logic -- has decent stack
      // depth
      // Is waiting for an event or in Timed wait
      // and there are others blocked for lock owned by this thread
      // Tag the thread at WARNING level

      if ((lock.getBlockers().size() > 1) && ((state == ThreadState.WAITING) || (state == ThreadState.TIMED_WAIT))) {
        advisoryList.add(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WAITING_WHILE_BLOCKING));
      }
    }

    // Check if some advisories are not adverse based on thread types
    resetAdvisoriesBasedOnThread(threadInfo, advisoryList);   
    

    // If the thread is in WAIT or BLOCKED state but the call came in as a
    // servlet, then tag that at WARNING level...

    if ((threadStack.contains(ThreadLogicConstants.SERVLET_PATTERN1) || threadStack.contains(ThreadLogicConstants.SERVLET_PATTERN2))
        && (state.equals(ThreadState.BLOCKED) || state.equals(ThreadState.WAITING) || state
            .equals(ThreadState.TIMED_WAIT))) {

      advisoryList.add(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WAITING_INSIDE_WEBLAYER));
    } 
    
    // If the thread is in BLOCKED state but the call came in as a
    // EJB, then tag that as EJB Blocked...

    if ((threadStack.contains(ThreadLogicConstants.EJB_PATTERN))
        && state.equals(ThreadState.BLOCKED)) {

      advisoryList.add(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.EJB_BLOCKED));
    }
    
    // Run SOA Advisory
    runSOAAdvisory(advisoryList,threadInfo); 	
    
    if (threadName.contains("weblogic.cluster.MessageReceiver")
        && !threadStack.contains("FragmentSocketWrapper.receive")) {
      advisoryList.add(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLS_CLUSTER_MESSAGERECEIVER_RUNNING));
    }
    
    if (threadName.contains(ThreadLogicConstants.FINALIZER_THREAD) 
            && !threadInfo.isBlockedForLock() 
            && (threadInfo.getBlockedForLock() == null) ) {          
          
      // Remove the Advisory for the Finalizer thread as its not really blocked...
      threadInfo.setHealth(HealthLevel.NORMAL);
      advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.FINALIZER_THREAD_BLOCKED));
    }

    threadInfo.addAdvisories(advisoryList);
    
    if (threadInfo.getAdvisories().isEmpty()
                  && (threadInfo.getHealth().ordinal() < HealthLevel.WATCH.ordinal()) ) {
      int stackDepth = threadStack.split("\n").length;
      if (stackDepth >= ThreadLogicConstants.ACTIVETHREAD_STACKDEPTH)       
        threadInfo.setHealth(HealthLevel.UNKNOWN);
    }
  }

  // Ignore the thread label/top few lines that might vary between threads...
  // Get the rest of the threads and check for repeat occurence of the same
  // stack pattern
  public static String getThreaddumpSubset(ThreadInfo thread, int minLenToConsider, int offset, int maxDepth) {
    StringBuffer sbuf = new StringBuffer(1000);

    String[] threads = thread.getContent().split("(\n)|(\r\n)");

    ArrayList<String> stackLines = new ArrayList<String>();

    for (String entry : threads) {

      // Skip entries... that dont start with Quotes as thread name are
      // surrounded by quotes or should be of length > 50
      if ((entry.length() > 50) || !entry.startsWith("\""))
        stackLines.add(entry);
    }

    // If the nested call stack is not really that deep, ignore it
    // Most often these might be routine idle/housekeeping threads.
    if (stackLines.size() < minLenToConsider)
      return null;

    // Get the lines from the starting offset
    // If the stack is not deep enough for maxLength, just save whatever is left
    for (int i = offset; (i < stackLines.size() && ((i - offset) < maxDepth)); i++) {
      sbuf.append(stackLines.get(i) + "\n");
    }
    String subset = sbuf.toString();
    
    // Empty up the lock info as these can be unique and result in cache misses
    subset = subset.replaceAll("<.*>", "");
    
    return subset;

  }

  //Derek Kam : For SOA Related Advisory
  public static void runSOAAdvisory(ArrayList<ThreadAdvisory> advisoryList,ThreadInfo threadInfo){
	    // If the thread is in WAIT or BLOCKED state but the call contain SOA BPEL Engine EJB Bean in as a
	    // then tag that at WARNING level...
	    String threadName = threadInfo.getName();
	    String threadStack = threadInfo.getContent();
	    ThreadState state = threadInfo.getState();
	    
	    if ((threadStack.contains(ThreadLogicConstants.SOA_ENGINE_PATTERN1) || threadStack.contains(ThreadLogicConstants.SOA_ENGINE_PATTERN2))
	            && (state.equals(ThreadState.BLOCKED) || state.equals(ThreadState.WAITING) || state
	                .equals(ThreadState.TIMED_WAIT))) {

	        advisoryList.add(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.SOA_ENGINE_BLOCKED));
	    }
	    if ((threadName.contains(ThreadLogicConstants.STUCK_PATTERN) && threadStack.contains(ThreadLogicConstants.SOA_HTTPCLIENT_PATTERN))) {

	        advisoryList.add(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.SOA_HTTPCLIENT_STUCKED));
	    }
	    if ((threadName.contains(ThreadLogicConstants.STUCK_PATTERN) && threadStack.contains(ThreadLogicConstants.SOA_DMS_COLLECTOR_PATTERN))) {

	        advisoryList.add(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.SOA_DMS_COLLECTOR_STUCKED));
	    }	
	    if ((threadName.contains(ThreadLogicConstants.STUCK_PATTERN) && threadStack.contains(ThreadLogicConstants.SOA_BPELXPATHFUNCTIONRESOLVER_PATTERN))) {

	        advisoryList.add(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.SOA_BPELXPATHFUNCTIONRESOLVER_STUCKED));
	    }	
	    if ((threadName.contains(ThreadLogicConstants.STUCK_PATTERN) && threadStack.contains(ThreadLogicConstants.CLUSTER_DEPLOYMENT_PATTERN))) {

	        advisoryList.add(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.CLUSTER_DEPLOYMENT_STUCKED));
	    }
	  
  }

  public static void runLockInfoAdvisory(LockInfo lockInfo) {

    ThreadAdvisory unownedLockContentionAdvisory = ThreadAdvisory
        .lookupThreadAdvisory(ThreadLogicConstants.CONTENTION_FOR_UNOWNED_LOCK);
    ThreadAdvisory blockedThreadAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.BLOCKED_THREADS);

    ThreadAdvisory blockOrUnContentedThreadAdvisory = blockedThreadAdvisory;

    if (lockInfo.getBlockers().size() >= ThreadLogicConstants.BLOCKED_THREADS_THRESHOLD) {

      ThreadInfo lockOwner = lockInfo.getLockOwner();

      // If the owner thread is a WLS Muxer thread, then ignore the blocked lock
      // advisory even if there are multiple blockers for the lock...
      if ((lockOwner != null) && lockOwner.getName().contains("Muxer"))
        return;

      // If the owner thread is null and the blockers are waiting for the lock
      // on the weblogic/timers/internal/TimerThread, then ignore it as a
      // bottleneck
      // There can be multiple threads waiting for notification from
      // TimerThread, so this is normal
      if ((lockOwner == null) && lockInfo.getLockId().contains("TimerThread"))
        return;

      lockInfo.addAdvisory(blockOrUnContentedThreadAdvisory);

      // If there is no owner for the lock, switch to the Contention for Unowned
      // Lock Advisory
      if (lockOwner == null)
        blockOrUnContentedThreadAdvisory = unownedLockContentionAdvisory;

      lockInfo.addAdvisory(blockOrUnContentedThreadAdvisory);
      
      // Check if the Lock is for the Weblogic JMS Queue
      // Add it as an advisory if so
      ThreadAdvisory jmsQueueBottleneckAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLS_JMS_QUEUE_BOTTLENECK);
      if (!lockInfo.getLockId().contains("QueueImpl"))
        jmsQueueBottleneckAdvisory = null;
      
      for (ThreadInfo ti : lockInfo.getBlockers()) {
        ti.addAdvisory(blockOrUnContentedThreadAdvisory);
        if (ti.getHealth().ordinal() < HealthLevel.WARNING.ordinal())
          ti.setHealth(HealthLevel.WARNING);
        
        if (jmsQueueBottleneckAdvisory != null)
          ti.addAdvisory(jmsQueueBottleneckAdvisory);        
      }

      if (lockInfo.getLockOwner() != null)
        lockInfo.getLockOwner().addAdvisory(blockedThreadAdvisory);
    }
  }

  public static void resetAdvisoriesBasedOnThread(ThreadInfo threadInfo, ArrayList<ThreadAdvisory> advisoryList) {

    String threadNameLower = threadInfo.getName().toLowerCase();
    boolean isAtWatchLevel = (threadInfo.getHealth() == HealthLevel.WATCH);

    if (threadNameLower.contains("muxer")) {      
      WLSMuxerThreadGroup.resetAdvisoriesBasedOnThread(threadInfo, advisoryList);
    } else if (threadInfo.getContent().contains("oracle.tip.adapter")) {
      SOAThreadGroup.resetAdvisoriesBasedOnThread(threadInfo, advisoryList);
    }

    if (isAtWatchLevel && threadNameLower.contains("ldap") || threadNameLower.contains("aq adapter")) {

      boolean watchLevelOnlyDueToSocketRead = true;
      for (ThreadAdvisory advisory : advisoryList) {
        if ((advisory.getHealth() == HealthLevel.WATCH)
            && !(advisory.getKeyword().equals(ThreadLogicConstants.SOCKET_READ)
                || advisory.getKeyword().equals(ThreadLogicConstants.DB_STMT_EXECUTE) || advisory.getKeyword().equals(
                ThreadLogicConstants.DB_PSTMT_EXECUTE))) {
          watchLevelOnlyDueToSocketRead = false;
          break;
        }
      }

      // Reset the Health level to NORMAL only for LDAP/Adapter threads
      // if the watch level is purely due to socket reads
      // Or DB Read for Adapters
      if (watchLevelOnlyDueToSocketRead) {
        threadInfo.setHealth(HealthLevel.NORMAL);
      }
    }

  }

  
  public static ArrayList<ThreadAdvisory> sortByHealth(ThreadAdvisory[] arr) {

    ArrayList<ThreadAdvisory> list = new ArrayList<ThreadAdvisory>();
    for (ThreadAdvisory o : arr) {
      list.add(o);
    }

    return sortByHealth(list);
  }

  public static ArrayList<ThreadAdvisory> sortByHealth(ArrayList<ThreadAdvisory> list) {

    // Check if there is nothing to sort...
    if ((list == null) || (list.size() <= 1))
      return list;

    // Sort using the underlying Health level
    Collections.sort(list);

    // Reverse for descending order of severity
    Collections.reverse(list);
    return list;
  }

  /**
   * @return the keywordList
   */
  public String[] getKeywordList() {
    return keywordList;
  }

  /**
   * @param keywordList the keywordList to set
   */
  public void setKeywordList(String[] keywordList) {
    this.keywordList = keywordList;
  }

}
