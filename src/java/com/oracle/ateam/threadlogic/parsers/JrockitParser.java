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
 * $Id: BeaJDKParser.java,v 1.12 2010-01-03 14:23:09 irockel Exp $
 */

package com.oracle.ateam.threadlogic.parsers;

import com.oracle.ateam.threadlogic.ThreadDumpInfo;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.categories.Category;
import com.oracle.ateam.threadlogic.monitors.JRockitMonitorMap;
import com.oracle.ateam.threadlogic.parsers.AbstractDumpParser.LineChecker;
import com.oracle.ateam.threadlogic.utils.CustomLogger;
import com.oracle.ateam.threadlogic.utils.DateMatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 * Parses Bea/JRockit Thread Dumps
 * 
 * @author irockel
 */
public class JrockitParser extends AbstractDumpParser {
  // private boolean foundClassHistograms = false;
  // private boolean withCurrentTimeStamp = false;

  private static Logger theLogger = CustomLogger.getLogger(JrockitParser.class.getSimpleName());
  
  private static Pattern jrockitDatePattern = Pattern.compile("[MTWFSa-z]{3}\\s[a-zA-Z]{3}\\s*\\d{1,2}\\s\\d\\d:\\d\\d:\\d\\d\\s\\d\\d\\d\\d");  
  
  /**
   * constructs a new instance of a bea jdk parser
   * 
   * @param dumpFileStream
   *          the dump file stream to read.
   * @param threadStore
   *          the thread store to store the thread informations in.
   */
  public JrockitParser(LineNumberReader bis, Map threadStore, int lineCounter, DateMatcher dm) {
    super(bis, dm);
    this.threadStore = threadStore;
    this.lineCounter = lineCounter;
    this.lineChecker = new LineChecker();
    this.lineChecker.setFullDumpPattern("(.*FULL THREAD DUMP.*)");
    this.lineChecker.setAtPattern("(.*at.*)");
    this.lineChecker.setWaitingOnPattern("(.*-- Waiting for notification on.*)");
    this.lineChecker.setParkingToWaitPattern("(.*-- Parking to wait for.*)");
    this.lineChecker.setWaitingToPattern("(.*-- Blocked trying to get lock.*)");
    this.lineChecker.setLockedPattern("(.*-- (Holding lock).*)");
    this.lineChecker.setEndOfDumpPattern("(.*(^[MTWFSa-z]{3}[a-zA-Z]{3}\\s*d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}\\s\\d\\d\\d\\d"
            + "|END OF THREAD DUMP| lock chains|Open lock chains|FULL THREAD DUMP).*)");    
    this.lineChecker.setExactEndOfDumpPattern("(.*END OF THREAD DUMP.*)"); 
    this.setJvmVendor(JVM_VENDOR_LIST[JROCKIT_VM]);
    
    //resetDmPattern(bis, dm);
    dm.setDefaultPattern(jrockitDatePattern);
    parseJvmVersion(bis);
  }

  protected void initVars() {
    LOCKED = "-- Holding lock:";
    BLOCKED_FOR_LOCK = "-- Blocked trying to get lock:";
    GENERAL_WAITING = "- Waiting for notification:";
  }  
  
  /**
   * @param bis the LineNumberReader
   */  
  protected void resetDmPattern(LineNumberReader bis, DateMatcher dm) {
    boolean foundDate = false;
    String dateEntry = "";
    Pattern jrockitDatePattern = Pattern.compile("[MTWFSa-z]{3}\\s[a-zA-Z]{3}\\s*\\d{1,2}\\s\\d\\d:\\d\\d:\\d\\d\\s\\d\\d\\d\\d");  
    dm.setDefaultPattern(jrockitDatePattern);
    
    try {
      bis.reset();
      while (bis.ready()) {        
        String line = bis.readLine();
        if (!foundDate && (line != null) && (line !="")) {          
          Matcher m = dm.checkForDateMatch(line);
          if (m != null) {
            dateEntry = line;
            foundDate = true;
            theLogger.finest("Timestamp:" + dateEntry);
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
    int count = 0;
    try {
      bis.reset();
      while (bis.ready() && count++ < 10) {        
        String line = bis.readLine();
        if (line != null) {
          int index = line.indexOf(" JRockit(R) ");
          if (index >= 0) {            
            theLogger.info("JVM Version:" + line);
            super.setJvmVersion(line.substring(index).trim());
            return;
          }
        }
      }
    } catch(Exception e) { }
    
  }

  
  /**
   * parse the next thread dump from the stream passed with the constructor.
   * 
   * @returns null if no more thread dumps were found.
   */

  public MutableTreeNode parseNext() {
    this.mmap = new JRockitMonitorMap();
    return super.parseNext();
  }

  protected String linkifyMonitor(String line) {
    if (line != null) {
      int colon = line.indexOf(":");
      int last = line.indexOf('[') >= 0 ? line.indexOf('[') : line.length();
      String begin = colon != -1 ? line.substring(0, colon + 2) : "";
      String monitor = colon != -1 ? line.substring(colon + 2, last) : line;
      String end = line.substring(last);
      monitor = "<a href=\"monitor://" + monitor + "\">" + monitor;
      monitor = monitor.substring(0, monitor.length()) + "</a>";
      return (begin + monitor + end);
    } else {
      return (line);
    }
  }

  public boolean isFoundClassHistograms() {
    // bea parser doesn't support class histograms
    return false;
  }
  
  protected boolean checkThreadDumpStatData(ThreadDumpInfo tdi) throws IOException {
    // bea parser doesn't support heap data
    return false;
  }

  public void parseLoggcFile(InputStream loggcFileStream, DefaultMutableTreeNode root) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void setDumpHistogramCounter(int value) {
    throw new UnsupportedOperationException("Not supported yet.");
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
    return (logLine.trim().indexOf("FULL THREAD DUMP") >= 0);
  }

  protected String[] getThreadTokens(String name) {
    String patternMask = "^.*\"([^\"]+)\".*id=([^ ]+).*tid=([^ ]+).*"
        + "prio=[^ ]+ ([^,]+,? ?[^,]+?,? ?[^,]+?,? ?[^,]+?)(, daemon)?$";
    
    String[] tokens = new String[] {};
    
    try {
      Pattern p = Pattern.compile(patternMask);
      Matcher m = p.matcher(name);

      m.matches();

      tokens = new String[7];
      tokens[0] = m.group(1); // name
      tokens[1] = m.group(3); // tid
      tokens[2] = m.group(2); // nid
      tokens[3] = m.group(4); // State

    } catch(Exception e) { 

      theLogger.warning("WARNING!! Unable to parse Thread Tokens with name:" + name  );
      //e.printStackTrace();
      return doHardParsing(name);
    }

    return (tokens);
  }

  /**
   * check and parse manually the thread label
   * 
   * @param nameEntry
   *          the thread name line
   */
   private String[] doHardParsing(String nameEntry) {
     
      String[] tokens = new String[4];
      int index = nameEntry.indexOf("\"", 1);
      if (index > 1) {
        tokens[0] = nameEntry.substring(1, index);
      } else {
        tokens[0] = nameEntry.substring(1);
        return tokens;
      }
      
      String[] remainingTokens = nameEntry.substring(index + 1).trim().split(" ");
      for(int i = 0; i < remainingTokens.length; i++) {
        
        if (i == 3)
          break;
        
        String label = remainingTokens[i].replaceAll(".*=", "");
        if (i == 0) // nid
          tokens[2] = label;
        else if (i == 2) // tid
          tokens[1] = label;
      }
      
      tokens[3] = " " + remainingTokens[5];
      return tokens;
  }
   
  @Override
  public boolean checkForClassHistogram(DefaultMutableTreeNode threadDump) throws IOException {
    return false;
  }
  
  /**
   * checks for the Lock Chains and adds it to the tree node passed
   * Useful to figure out thread holding ReentrantLock which is not visible in the thread stack traces
   * 
   * @param threadDump
   *          which tree node to add the lock chain info.
   */
  public boolean checkForLockChains(DefaultMutableTreeNode catThreadNode) throws IOException {
    
    boolean finished = false;
    boolean found = false;
    int lines = 0;
    
    
    String blockedThreadPatternMask = "^.*\"([^\\\"]+)\".*waiting for ([^ ]+) held by:.*";
    String ownerThreadPatternMask = "^.*\"([^\\\"]+)\".*";

    Pattern blockedThreadPattern = Pattern.compile(blockedThreadPatternMask);
    Pattern ownerThreadPattern = Pattern.compile(ownerThreadPatternMask);

    Category catThreads = (Category)catThreadNode.getUserObject();
    String blockedThread, prevBlockedThread, lockObj, prevLockObj, ownerThread;
    blockedThread = prevBlockedThread = prevLockObj = lockObj = ownerThread = null;
    
    // Save the thread info in a hashtable for quick retreival of the thread stack later based on thread name
    Hashtable<String, ThreadInfo> threadMap = new Hashtable();
    for (int i = 0; i < catThreads.getNodeCount(); i++) {
      ThreadInfo ti = (ThreadInfo) ((DefaultMutableTreeNode) catThreads.getNodeAt(i)).getUserObject();
      threadMap.put(ti.getFilteredName(), ti);         
    }
    
    while (getBis().ready() && !finished) {
      String line = getNextLine();
      if (!found && !line.equals("")) {
        if (line.trim().contains(" lock chains") ) {
          found = true;
        } else if (lines >= getMaxCheckLines()) {
          finished = true;
        } else {
          lines++;
        }
      } else if (found) {
        
        if ( this.lineChecker.getExactEndOfDump(line) != null) {
          finished = true;
          getBis().mark(getMarkSize());
        } 
      
        
        // Ignore lines with 'Chain' or '========='
        if (line.startsWith("==========") || line.startsWith("Chain ")) {
    
          // Reset all saved threads, lock objects
          prevBlockedThread = blockedThread = lockObj = prevLockObj = ownerThread = null;
          
        } else {
          
          Matcher m = blockedThreadPattern.matcher(line);
          if (m.matches()) {

            blockedThread = "\"" + m.group(1).replaceAll("\\[.*\\] ", "") + "\"";
            lockObj = m.group(2);            

            // If there was previously another blocked thread waiting for a resource held by current thread
            /*
             * "[ACTIVE] ExecuteThread: '35' for queue: 'weblogic.kernel.Default (self-tuning)'" id=264 idx=0x50 tid=6408 waiting for com/hyperion/atf/internal/services/naming/NamingEntry@0x0000000003173FD8 held by:
              "[STUCK] ExecuteThread: '14' for queue: 'weblogic.kernel.Default (self-tuning)'" id=243 idx=0x3b0 tid=8208 waiting for java/util/Collections$SynchronizedSortedMap@0x000000003C8762E0 held by:

              "[STUCK] ExecuteThread: '0' for queue: 'weblogic.kernel.Default (self-tuning)'" id=14 idx=0x38 tid=7456 in chain 2
             * 
             */
            if ((prevBlockedThread != null) && (prevLockObj != null)) {

                // The current blocked thread is owner of the previous lock object
                ThreadInfo ownerThreadInfo = threadMap.get(blockedThread);
                if (ownerThreadInfo == null)
                  continue;

                // We already know the thread is in waiting state, so no need to call addSleepToMonitor()
                // Call addLockToMonitor() just to cover cases like threads that are holding locks 
                // that are not visible in the thread stack trace (like ReentrantLocks)
                mmap.addLockToMonitor(prevLockObj, ownerThreadInfo.getName(), ownerThreadInfo.getContent()); 
                //theLogger.finest("Added owner: " + blockedThread + ", for lock: " + prevLockObj);
            }          

            prevLockObj = lockObj;
            prevBlockedThread = blockedThread;
              
          } else {
            // Just the owner thread line
            m = ownerThreadPattern.matcher(line);
            if (m.matches()) {
              ownerThread = "\"" + m.group(1).replaceAll("\\[.*\\] ", "") + "\"";
              
              //theLogger.finest("Blocked Thread: " + blockedThread);
              //theLogger.finest("Owner Thread: " + ownerThread);
              //theLogger.finest("Lock Obj: " + lockObj);
              
              
              ThreadInfo ownerThreadInfo = threadMap.get(ownerThread);
              if (ownerThreadInfo == null)
                continue;
              
              // We already know the thread is in waiting state, so no need to call addSleepToMonitor()
              // Call addLockToMonitor() just to cover cases like threads that are holding locks 
              // that are not visible in the thread stack trace (like ReentrantLocks)
              mmap.addLockToMonitor(lockObj, ownerThreadInfo.getName(), ownerThreadInfo.getContent());
              
              // Reset all previously saved threads/locks...
              prevBlockedThread = blockedThread = lockObj = prevLockObj = ownerThread = null;
            }
          }
        }
      }
    }

    if (found)
      getBis().mark(getMarkSize());
    
    return (found);     
  }
  

  @Override
  public String linkifyDeadlockInfo(String line) {
    // TODO Auto-generated method stub
    return null;
  }
}
