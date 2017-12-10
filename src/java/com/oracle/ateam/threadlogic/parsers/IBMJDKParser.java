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
package com.oracle.ateam.threadlogic.parsers;

import com.oracle.ateam.threadlogic.LockInfo;
import com.oracle.ateam.threadlogic.ThreadLogic;
import com.oracle.ateam.threadlogic.ThreadDumpInfo;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.categories.Category;
import com.oracle.ateam.threadlogic.categories.TableCategory;
import com.oracle.ateam.threadlogic.categories.TreeCategory;
import com.oracle.ateam.threadlogic.monitors.IBMMonitorMap;
import com.oracle.ateam.threadlogic.monitors.MonitorMap;
import com.oracle.ateam.threadlogic.parsers.AbstractDumpParser.LineChecker;
import com.oracle.ateam.threadlogic.utils.CustomLogger;
import com.oracle.ateam.threadlogic.utils.DateMatcher;
import com.oracle.ateam.threadlogic.utils.IconFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import java.util.Hashtable;
import java.util.logging.Logger;

public class IBMJDKParser extends AbstractDumpParser {

  private String monObject = "3LKMONOBJECT";
  private String monPattern = "3LKMONOBJECT\\s*([^:.]*): (owner|Flat locked by|<unowned>) (\\\".*[^(]) \\(.*";
  private String waitNotify = "3LKWAITNOTIFY";
  private String waiter = "3LKWAITER";  
  private String parsedStartTime;

  private static Logger theLogger = CustomLogger.getLogger(IBMJDKParser.class.getSimpleName());
  
  public IBMJDKParser(LineNumberReader bis, Map threadStore, int lineCounter, boolean withCurrentTimeStamp,
      int startCounter, DateMatcher dm) {
    super(bis, dm);
    this.threadStore = threadStore;
    this.withCurrentTimeStamp = withCurrentTimeStamp;
    this.lineCounter = lineCounter;
    this.counter = startCounter;
    this.lineChecker = new LineChecker();
    this.lineChecker.setFullDumpPattern("(.*0SECTION.*)");
    this.lineChecker.setAtPattern("4XESTACKTRACE\\s{6}(.*at.*)");
    this.lineChecker.setStackStartPattern("3XMTHREADINFO\\s{6}(.*\".*)");
    this.lineChecker.setThreadStatePattern("(.*java.lang.Thread.State.*)");
    this.lineChecker.setLockedOwnablePattern("(.*Locked ownable synchronizers:.*)");
    this.lineChecker.setWaitingOnPattern("3LKWAITNOTIFY\\s*(.*\").*");
    this.lineChecker.setParkingToWaitPattern("(.*- parking to wait.*)");
    this.lineChecker.setWaitingToPattern("3LKWAITER\\s*(.*\").*");
    this.lineChecker.setLockedPattern("3LKMONOBJECT");
    this.lineChecker.setEndOfDumpPattern(".*(VM Periodic Task Thread|Suspend Checker Thread|<EndOfDump>).*");
    this.setJvmVendor(JVM_VENDOR_LIST[IBM_VM]);
    
    resetDmPattern(bis, dm);
    parseJvmVersion(bis);
  }

  protected void initVars() {
    LOCKED = "3LKMONOBJECT\\s*.*: owner ";
    BLOCKED_FOR_LOCK = "3LKWAITNOTIFY\\s*(.*\")";
    GENERAL_WAITING = "3LKWAITER\\s*(.*\")";
  }

  /**
   * @param bis the LineNumberReader
   * @param dm Date Matcher pattern
   */  
  protected void resetDmPattern(LineNumberReader bis, DateMatcher dm) {
    int count = 0;
    boolean foundDate = false;
    Pattern ibmDatePattern = Pattern.compile("1TIDATETIME\\s*Date:\\s*\\d\\d\\d\\d.\\d\\d.\\d\\d\\sat\\s\\d\\d:\\d\\d:\\d\\d");
    dm.setDefaultPattern(ibmDatePattern);
    
    try {
      bis.reset();      
      while (bis.ready() && count++ < 30) {        
        String line = bis.readLine();
        if (!foundDate && (line != null) && (line !="")) {          
          int index = line.indexOf("1TIDATETIME");
          if (index >= 0) {
            index = line.indexOf("Date:", index) + 5;
            parsedStartTime = line.substring(index).trim();            
            theLogger.finest("Timestamp:" + parsedStartTime);
            return;
          } 
        }
      }
    } catch(Exception e) { }
  }

  
  /**
   * @param bis the LineNumberReader
   */
  protected void parseJvmVersion(LineNumberReader bis) {
    
    String IBM_VM_TAG = "1CIJAVAVERSION";
    int len = IBM_VM_TAG.length();
    try {
      while (bis.ready()) {        
        String line = bis.readLine();
        if (line != null) {
          int index = line.indexOf(IBM_VM_TAG);
          if (index >= 0) {                        
            index += len; 
            super.setJvmVersion(line.substring(index).trim());
            theLogger.info("JVM Version:" + line);            
            return;
          }
        }
      }
    } catch(Exception e) { }
    
  }
  
  /*
   * public MutableTreeNode parseNext() { this.mmap = new MonitorMap(); return
   * super.parseNext(); }
   */

  /**
   * parse the next thread dump from the stream passed with the constructor.
   * 
   * @returns null if no more thread dumps were found.
   */
  public MutableTreeNode parseNext() {
    if (nextDump != null) {
      MutableTreeNode tmpDump = nextDump;
      nextDump = null;
      return (tmpDump);
    }
    boolean retry = false;
    boolean parsingMonitors;
    String line = null;
    String tempLine = null;
    mmap = new IBMMonitorMap();

    do {
      DefaultMutableTreeNode threadDump = null;
      ThreadDumpInfo overallTDI = null;
      DefaultMutableTreeNode catMonitors = null;
      DefaultMutableTreeNode catMonitorsLocks = null;
      DefaultMutableTreeNode catThreads = null;
      DefaultMutableTreeNode catLocking = null;
      DefaultMutableTreeNode catBlockingMonitors = null;
      DefaultMutableTreeNode catSleeping = null;
      DefaultMutableTreeNode catWaiting = null;

      try {
        Map threads = new HashMap();
        overallTDI = new ThreadDumpInfo("Dump No. " + counter++, 0);
        overallTDI.setIsIBMJVM();        
        overallTDI.setJvmType(this.getJvmVendor());
        overallTDI.setJvmVersion(this.getJvmVersion());
        
        if (parsedStartTime != null) {
          overallTDI.setStartTime(parsedStartTime);
        } else if (withCurrentTimeStamp) {
          overallTDI.setStartTime((new Date(System.currentTimeMillis())).toString());
        }
        
        threadDump = new DefaultMutableTreeNode(overallTDI);

        catThreads = new DefaultMutableTreeNode(new TableCategory("Threads", IconFactory.THREADS));
        threadDump.add(catThreads);

        catWaiting = new DefaultMutableTreeNode(new TableCategory("Threads waiting for Monitors",
            IconFactory.THREADS_WAITING));

        catSleeping = new DefaultMutableTreeNode(new TableCategory("Threads sleeping on Monitors",
            IconFactory.THREADS_SLEEPING));

        catLocking = new DefaultMutableTreeNode(new TableCategory("Threads locking Monitors",
            IconFactory.THREADS_LOCKING));

        // create category for monitors with disabled filtering.
        // NOTE: These strings are "magic" in that the methods
        // TDA#displayCategory and TreeCategory#getCatComponent both
        // checks these literal strings and the behavior differs.
        catMonitors = new DefaultMutableTreeNode(new TreeCategory("Monitors", IconFactory.MONITORS, false));
        catMonitorsLocks = new DefaultMutableTreeNode(new TreeCategory("Monitors without locking thread",
            IconFactory.MONITORS_NOLOCKS, false));
        catBlockingMonitors = new DefaultMutableTreeNode(new TreeCategory("Threads blocked by Monitors",
            IconFactory.THREADS_LOCKING, false));

        String title = null;
        String dumpKey = null;
        StringBuffer content = null;
        boolean inLocking = false;
        boolean inSleeping = false;
        boolean inWaiting = false;
        int threadCount = 0;
        int waiting = 0;
        int locking = 0;
        int sleeping = 0;
        boolean locked = true;
        boolean finished = false;
        Stack monitorStack = new Stack();
        long startTime = 0;
        int singleLineCounter = 0;
        boolean concurrentSyncsFlag = false;
        Matcher matched = getDm().getLastMatch();        
        String parsedStartTime = null;
        
        Hashtable<String, LockInfo> lockTable = new Hashtable<String, LockInfo>();

        while (getBis().ready() && !finished) {
          line = getNextLine();
          lineCounter++;
          singleLineCounter++;
          if (locked) {
            if (lineChecker.getFullDump(line) != null) {
              locked = false;
              if (!withCurrentTimeStamp) {
                overallTDI.setLogLine(lineCounter);

                if (startTime != 0) {
                  startTime = 0;
                } else if (matched != null && matched.matches()) {

                  parsedStartTime = matched.group(0);
                  // Just use the relevant bits
                  // 1TIDATETIME    Date:                 2011/01/12 at 17:14:40
                  int index = parsedStartTime.indexOf("Date:");
                  String tokens[] = parsedStartTime.substring(index + 5).trim().split(" ");
                  
                  parsedStartTime = tokens[0].replaceAll("/", "-") + " " + tokens[2];
                  theLogger.finest("ParsedStartTime:" + parsedStartTime);
                  if (!getDm().isDefaultMatches() && isMillisTimeStamp()) {
                    try {
                      // the factor is a hack for a bug in
                      // oc4j timestamp printing (pattern
                      // timeStamp=2342342340)
                      if (parsedStartTime.length() < 13) {
                        startTime = Long.parseLong(parsedStartTime)
                            * (long) Math.pow(10, 13 - parsedStartTime.length());
                      } else {
                        startTime = Long.parseLong(parsedStartTime);
                      }
                    } catch (NumberFormatException nfe) {
                      startTime = 0;
                    }
                    if (startTime > 0) {
                      overallTDI.setStartTime((new Date(startTime)).toString());
                    }
                  } else {
                    overallTDI.setStartTime(parsedStartTime);
                  }
                  parsedStartTime = null;
                  matched = null;
                  getDm().resetLastMatch();
                }
              }
              dumpKey = overallTDI.getName();
            } else if (!getDm().isPatternError() && (getDm().getRegexPattern() != null)) {
              Matcher m = getDm().checkForDateMatch(line);
              if (m != null) {
                matched = m;
              }
            }
          } else {
            if (line.indexOf("LOCKS subcomponent dump routine") >= 0) {
              String[] tokens = null;
              String lockedObject = null;
              while ((line = getNextLine()) != null) {
                lineCounter++;
                if (line.indexOf("JVM System Monitor Dump") >= 0) {
                  break;
                }

                //if ((tokens = parseMonitor(line)) != null) {
                if (line.indexOf(monObject) >= 0)  {
                  tokens = parseMonitor(line);
                  
                  // Check if the thread contains "Workmanager:" and ending with " ms" 
                  // In that case, rerun the pattern to get correct thread label
                  String additionalLines = readAheadForWMThreadLabels(line);
                  if (additionalLines.length() > line.length())
                    tokens = parseMonitor(additionalLines);
                  
                  // only care about tokens 1 and 3 (if 3 exists)
                  lockedObject = tokens[1]; // Object being locked
                  String lockingThread = tokens[3]; // Thread locking the object
                  if (tokens[3] != null) {
                    // Locked
                    mmap.addLockToMonitor(lockedObject, lockingThread, null); // We'll
                                                                              // add
                                                                              // thread
                                                                              // stacks
                                                                              // later
                    // theLogger.finest("1LockedMonitor[" + lockedObject +
                    // "], thread: " + lockingThread );

                    ThreadInfo lockOwner = ThreadInfo.createTempThreadInfo(lockingThread);
                    LockInfo lockInfo = new LockInfo(lockedObject, lockOwner);

                    if (lockTable.containsKey(lockedObject)) {
                      lockInfo = lockTable.get(lockedObject);
                    }

                    lockInfo.setLockOwner(lockOwner);
                    lockTable.put(lockedObject, lockInfo);

                    // locking++;
                  }
                } else if (lockedObject != null && line.indexOf(waiter) >= 0) {
                  // Check if the thread contains "Workmanager:" and ending with " ms" 
                  // In that case, rerun the pattern to get correct thread label
                  String additionalLines = readAheadForWMThreadLabels(line);
                  if (additionalLines.length() > line.length())
                    line = additionalLines;
                  
                  mmap.addWaitToMonitor(lockedObject, lineChecker.getWaitingTo(line), null);
                  // theLogger.finest("1BlockedForMonitor[" + lockedObject +
                  // "], thread: " + line + ", lineCheker : " +
                  // lineChecker.getWaitingTo(line));
                  String threadName = lineChecker.getWaitingTo(line);
                  if (threadName != null) {
                    ThreadInfo blockedTi = ThreadInfo.createTempThreadInfo(threadName);
                    LockInfo lockInfo = new LockInfo(lockedObject);

                    if (lockTable.containsKey(lockedObject)) {
                      lockInfo = lockTable.get(lockedObject);
                    }

                    lockInfo.addBlocker(blockedTi);
                    lockTable.put(lockedObject, lockInfo);
                  }

                  // waiting++;
                } else if (lockedObject != null && line.indexOf(waitNotify) >= 0) {
                  // Check if the thread contains "Workmanager:" and ending with " ms" 
                  // In that case, rerun the pattern to get correct thread label
                  String additionalLines = readAheadForWMThreadLabels(line);
                  if (additionalLines.length() > line.length())
                    line = additionalLines;
                  
                  mmap.addSleepToMonitor(lockedObject, lineChecker.getWaitingOn(line), null);
                  // theLogger.finest("1WaitingOnMonitor[" + lockedObject +
                  // "], thread: " + line + ", lineCheker : " +
                  // lineChecker.getWaitingOn(line));
                  // sleeping++;
                }
              }
            }

            if ((tempLine = lineChecker.getStackStart(line)) != null) {
              if (lineChecker.getGCThread(line) != null) {
                // skip GC Threads
                continue;
              }
              
              // Check if the thread contains "Workmanager:" and ending with " ms" 
              // In that case, rerun the pattern to get correct thread label
              String additionalLines = readAheadForWMThreadLabels(line);
              if (additionalLines.length() > line.length())
                tempLine = lineChecker.getStackStart(additionalLines);
              
              // We are starting a group of lines for a different
              // thread
              // First, flush state for the previous thread (if
              // any)
              String stringContent = content != null ? content.toString() : null;
              if (title != null) {
                threads.put(title, content.toString());
                content.append("</pre></pre>");
                addToCategory(catThreads, title, null, stringContent, singleLineCounter, true);
                threadCount++;
                Iterator iter = mmap.iterOfKeys();
                while (iter.hasNext()) {
                  String shortTitle = title.substring(0, title.indexOf("\"", 1) + 1);
                  //theLogger.finest("1Complete title: " + title 
                  // + ", Map Searching for shortTitle: " + shortTitle);
                  String monitor = (String) iter.next();
                  Map[] t = mmap.getFromMonitorMap(monitor);
                  Set lockMap = t[MonitorMap.LOCK_THREAD_POS].keySet();
                  if (lockMap.contains(shortTitle)) {
                    inLocking = true;
                    lockMap.remove(shortTitle);
                    mmap.addLockToMonitor(monitor, title, stringContent);
                    // theLogger.finest("2LockedMonitor[" + monitor +
                    // "], title: " + title + ", content: " + stringContent );
                  }
                  Set waitingMap = t[MonitorMap.WAIT_THREAD_POS].keySet();
                  if (waitingMap.contains(shortTitle)) {
                    inWaiting = true;
                    waitingMap.remove(shortTitle);
                    mmap.addWaitToMonitor(monitor, title, stringContent);
                    // theLogger.finest("2BlockedForMonitor[" + monitor +
                    // "], title: " + title + ", content: " + stringContent );
                  }
                  Set sleepingMap = t[MonitorMap.SLEEP_THREAD_POS].keySet();
                  if (sleepingMap.contains(shortTitle)) {
                    inSleeping = true;
                    sleepingMap.remove(shortTitle);
                    mmap.addSleepToMonitor(monitor, title, stringContent);
                    // theLogger.finest("2WaitOnMonitor[" + monitor +
                    // "], title: " + title + ", content: " + stringContent );
                  }
                }
              }

              if (inWaiting) {
                addToCategory(catWaiting, title, null, stringContent, singleLineCounter, true);
                inWaiting = false;
                waiting++;
              }
              if (inSleeping) {
                addToCategory(catSleeping, title, null, stringContent, singleLineCounter, true);
                inSleeping = false;
                sleeping++;
              }
              if (inLocking) {
                addToCategory(catLocking, title, null, stringContent, singleLineCounter, true);
                inLocking = false;
                locking++;
              }
              singleLineCounter = 0;

              // Second, initialize state for this new thread
              title = tempLine;
              content = new StringBuffer("<pre><font size=" + ThreadLogic.getFontSizeModifier(-1)
                  + ">");
              content.append(tempLine);
              content.append("\n");
            } else if ((tempLine = lineChecker.getAt(line)) != null) {
              content.append(tempLine);
              content.append("\n");
            }

            // last thread reached?
            if ((tempLine = lineChecker.getEndOfDump(line)) != null) {
              finished = true;
              getBis().mark(getMarkSize());
              if ((checkForDeadlocks(threadDump)) == 0) {
                // no deadlocks found, set back original
                // position.
                getBis().reset();
              }

              if (!checkThreadDumpStatData(overallTDI)) {
                // no statistical data found, set back original
                // position.
                getBis().reset();
              }

              getBis().mark(getMarkSize());
              if (!(foundClassHistograms = checkForClassHistogram(threadDump))) {
                getBis().reset();
              }
            }
          }
        }
        // last thread
        String stringContent = content != null ? content.toString() : null;
        if (title != null) {
          threads.put(title, content.toString());
          content.append("</pre></pre>");
          addToCategory(catThreads, title, null, stringContent, singleLineCounter, true);
          threadCount++;
          Iterator iter = mmap.iterOfKeys();
          while (iter.hasNext()) {
            String shortTitle = title.substring(0, title.indexOf("\"", 1) + 1);
            String monitor = (String) iter.next();
            Map[] t = mmap.getFromMonitorMap(monitor);
            Set lockMap = t[MonitorMap.LOCK_THREAD_POS].keySet();
            if (lockMap.contains(shortTitle)) {
              inLocking = true;
              lockMap.remove(shortTitle);
              //theLogger.finest("Lock map contained shortTitle: " + shortTitle 
              // + ", removing it... and adding to LockMonitor with title : " + title );
              mmap.addLockToMonitor(monitor, title, stringContent);
            }
            Set waitingMap = t[MonitorMap.WAIT_THREAD_POS].keySet();
            if (waitingMap.contains(shortTitle)) {
              inWaiting = true;
              waitingMap.remove(shortTitle);
              //theLogger.finest("Lock map contained shortTitle: " + shortTitle 
              // + ", removing it... and adding to waitMonitor with title : " + title );
              mmap.addWaitToMonitor(monitor, title, stringContent);
            }
            Set sleepingMap = t[MonitorMap.SLEEP_THREAD_POS].keySet();
            if (sleepingMap.contains(shortTitle)) {
              inSleeping = true;
              sleepingMap.remove(shortTitle);
              //theLogger.finest("Lock map contained shortTitle: " + shortTitle 
              // + ", removing it... and adding to SleepMonitor with title : " + title );
              mmap.addSleepToMonitor(monitor, title, stringContent);
            }
          }
        }
        if (inWaiting) {
          addToCategory(catWaiting, title, null, stringContent, singleLineCounter, true);
          inWaiting = false;
          waiting++;
        }
        if (inSleeping) {
          addToCategory(catSleeping, title, null, stringContent, singleLineCounter, true);
          inSleeping = false;
          sleeping++;
        }
        if (inLocking) {
          addToCategory(catLocking, title, null, stringContent, singleLineCounter, true);
          inLocking = false;
          locking++;
        }
        singleLineCounter = 0;

        int monitorCount = mmap.size();

        int monitorsWithoutLocksCount = 0;
        int contendedMonitors = 0;
        int blockedThreads = 0;
        // dump monitors
        if (mmap.size() > 0) {
          int[] result = dumpMonitors(catMonitors, catMonitorsLocks, mmap);
          monitorsWithoutLocksCount = result[0];
          overallTDI.setOverallThreadsWaitingWithoutLocksCount(result[1]);

          result = dumpBlockingMonitors(catBlockingMonitors, mmap);
          contendedMonitors = result[0];
          blockedThreads = result[1];
        }

        // display nodes with stuff to display
        if (waiting > 0) {
          overallTDI.setWaitingThreads((Category) catWaiting.getUserObject());
          threadDump.add(catWaiting);
        }

        if (sleeping > 0) {
          overallTDI.setSleepingThreads((Category) catSleeping.getUserObject());
          threadDump.add(catSleeping);
        }

        if (locking > 0) {
          overallTDI.setLockingThreads((Category) catLocking.getUserObject());
          threadDump.add(catLocking);
        }

        if (monitorCount > 0) {
          overallTDI.setMonitors((Category) catMonitors.getUserObject());
          threadDump.add(catMonitors);
        }

        if (contendedMonitors > 0) {
          overallTDI.setBlockingMonitors((Category) catBlockingMonitors.getUserObject());
          threadDump.add(catBlockingMonitors);
        }

        if (monitorsWithoutLocksCount > 0) {
          overallTDI.setMonitorsWithoutLocks((Category) catMonitorsLocks.getUserObject());
          threadDump.add(catMonitorsLocks);
        }
        overallTDI.setThreads((Category) catThreads.getUserObject());

        // Create relationship between LockInfo and Threads
        overallTDI.parseLocks(this);

        // Requires extra step as Locks are outside of threads in IBM Thread
        // dump
        overallTDI.linkThreadsWithLocks(lockTable);

        // Detect Deadlocks
        overallTDI.detectDeadlock();

        // Run Thread Dump Advisory
        overallTDI.runThreadsAdvisory();

        ((Category) catThreads.getUserObject()).setName(((Category) catThreads.getUserObject()) + " (" + threadCount
            + " Threads overall)");
        ((Category) catWaiting.getUserObject()).setName(((Category) catWaiting.getUserObject()) + " (" + waiting
            + " Threads waiting)");
        ((Category) catSleeping.getUserObject()).setName(((Category) catSleeping.getUserObject()) + " (" + sleeping
            + " Threads sleeping)");
        ((Category) catLocking.getUserObject()).setName(((Category) catLocking.getUserObject()) + " (" + locking
            + " Threads locking)");
        ((Category) catMonitors.getUserObject()).setName(((Category) catMonitors.getUserObject()) + " (" + monitorCount
            + " Monitors)");
        ((Category) catBlockingMonitors.getUserObject()).setName(((Category) catBlockingMonitors.getUserObject())
            + " (" + blockedThreads + " Threads blocked by " + contendedMonitors + " Monitors)");
        ((Category) catMonitorsLocks.getUserObject()).setName(((Category) catMonitorsLocks.getUserObject()) + " ("
            + monitorsWithoutLocksCount + " Monitors)");
        // add thread dump to passed dump store.
        if ((threadCount > 0) && (dumpKey != null)) {
          threadStore.put(dumpKey.trim(), threads);
        }

        // check custom categories
        addCustomCategories(threadDump);
        addCategories(threadDump);

        return (threadCount > 0 ? threadDump : null);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (StringIndexOutOfBoundsException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "Error during parsing of a found thread dump, skipping to next one!\n"
            + "Check for possible broken dumps, sometimes, stream flushing mixes the logged data.\n"
            + "Error Message is \"" + e.getLocalizedMessage() + "\". \n"
            + (line != null ? "Last line read was \"" + line + "\". \n" : ""), "Error during Parsing Thread Dump",
            JOptionPane.ERROR_MESSAGE);
        retry = true;
      } catch (IOException e) {
        e.printStackTrace();
      }
    } while (retry);

    return (null);
  }

  private String[] parseMonitor(String line) {
    String[] tokens = null;
    Pattern monitorPattern = Pattern.compile(monPattern);
    Matcher matcher = monitorPattern.matcher(line);
    if (matcher.matches()) {
      tokens = new String[matcher.groupCount() + 1];
      for (int i = 0; i <= matcher.groupCount(); i++) {
        tokens[i] = matcher.group(i);
      }
    }
    if (tokens != null)
      return tokens;
    
    // Above pattern does not work when there are no owners, so parse it the hard way...
    // Example:3LKMONOBJECT       weblogic/work/ExecuteThread@0x070000005787D288/0x070000005787D2A0: <unowned>
    tokens = new String[4];
    tokens[1] = line.split("\\s+")[1].replace(":", "");
    return tokens;
  }

  @Override
  public void parseLoggcFile(InputStream loggcFileStream, DefaultMutableTreeNode root) {
    // TODO Auto-generated method stub

  }

  protected String[] getThreadTokens(String name) {
    // 3XMTHREADINFO
    // "[ACTIVE] ExecuteThread: '2' for queue: 'weblogic.kernel.Default (self-tuning)'"
    // J9VMThread:0x000000011C692300, j9thread_t:0x000000011BB94AE0,
    // java/lang/Thread:0x07000000032B7618, state:B, prio=5
    String patternMask = "^.*\"([^\"]+)\".*:([^ ,]+),.*j9thread_t:([^ ,]+).*state:([^ ]+),.*prio=[^ ]+.*";
    String[] tokens = new String[] {};
    
    try {
      Pattern p = Pattern.compile(patternMask);
      Matcher m = p.matcher(name);

      if(m.matches()){      
	      tokens = new String[7];
	      tokens[0] = m.group(1); // name
	      tokens[1] = m.group(3); // tid
	      tokens[2] = m.group(2); // nid
	      tokens[3] = m.group(4); // State
      }
     } catch(Exception e) { 
      
      theLogger.warning("WARNING!! Unable to parse Thread Tokens with name:" + name);
      e.printStackTrace();
    }

    return (tokens);
  }

  // With IBM, what is passed in should be the full monitor so no parsing of the
  // line is really needed.
  protected String linkifyMonitor(String line) {
    if (line != null) {
      String monitor = "<a href=\"monitor://" + line + "\">" + line + "</a>";
      return monitor;
    } else {
      return (line);
    }
  }

  @Override
  boolean checkForClassHistogram(DefaultMutableTreeNode threadDump) throws IOException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean checkForLockChains(DefaultMutableTreeNode threadDump) throws IOException {
    return false;
  } 

  
  @Override
  String linkifyDeadlockInfo(String line) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * check if the passed logline contains the beginning of a Bea jdk thread
   * dump.
   * 
   * @param logLine
   *          the line of the logfile to test
   * @return true, if the start of a bea thread dump is detected.
   */
  public static boolean checkForSupportedThreadDump(String logLine) {
    return (logLine.trim().indexOf("0SECTION") >= 0);
  }

}