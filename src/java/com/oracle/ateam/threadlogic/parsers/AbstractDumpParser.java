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
 * $Id: AbstractDumpParser.java,v 1.21 2010-01-03 14:23:09 irockel Exp $
 */
package com.oracle.ateam.threadlogic.parsers;

import com.oracle.ateam.threadlogic.HeapInfo;
import com.oracle.ateam.threadlogic.Logfile;
import com.oracle.ateam.threadlogic.ThreadLogic;
import com.oracle.ateam.threadlogic.ThreadDumpInfo;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup;
import com.oracle.ateam.threadlogic.categories.Category;
import com.oracle.ateam.threadlogic.categories.CustomCategory;
import com.oracle.ateam.threadlogic.categories.ExternalizedNestedThreadGroupsCategory;
import com.oracle.ateam.threadlogic.categories.NestedCategory;
import com.oracle.ateam.threadlogic.categories.TableCategory;
import com.oracle.ateam.threadlogic.categories.ThreadDiffsTableCategory;
import com.oracle.ateam.threadlogic.categories.TreeCategory;
import com.oracle.ateam.threadlogic.filter.Filter;
import com.oracle.ateam.threadlogic.monitors.MonitorMap;
import com.oracle.ateam.threadlogic.utils.CustomLogger;
import com.oracle.ateam.threadlogic.utils.DateMatcher;
import com.oracle.ateam.threadlogic.utils.IconFactory;
import com.oracle.ateam.threadlogic.utils.PrefManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.ListModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * abstract dump parser class, contains all generic dump parser stuff, which
 * doesn't have any jdk specific parsing code.
 *
 * All Dump Parser should extend from this class as it already provides a basic
 * parsing interface.
 *
 * @author irockel
 */
public abstract class AbstractDumpParser implements DumpParser, Serializable {

  private transient LineNumberReader bis = null;
  private int markSize = 16384;
  private int maxCheckLines = 10;
  private boolean millisTimeStamp = false;
  private transient DateMatcher dm = null;
  /**
   * this counter counts backwards for adding class histograms to the thread
   * dumps beginning with the last dump.
   */
  private int dumpHistogramCounter = -1;
  protected transient LineChecker lineChecker;
  protected MonitorMap mmap;
  protected MutableTreeNode nextDump = null;
  protected Map threadStore = null;
  protected int counter = 1;
  protected int lineCounter = 0;
  protected boolean foundLockChains = false;
  protected boolean foundClassHistograms = false;
  protected boolean withCurrentTimeStamp = false;
  protected String LOCKED;
  protected String BLOCKED_FOR_LOCK;
  protected String GENERAL_WAITING;
  // Adding support for ECID & Thread Context data as part of generated thread dump
  protected String THREAD_TIMING_STATISTICS = " THREAD TIMING STATISTICS";
  protected String THREAD_CONTEXT_INFO = " THREAD CONTEXT INFORMATION";
  protected String END_THREAD_CONTEXT_INFO = " END OF THREAD CONTEXT INFORMATION";
  public final static String DUMP_MARKER = "Associated Dump: ";
  protected static final int HOTSPOT_VM = 0;
  protected static final int JROCKIT_VM = 1;
  protected static final int IBM_VM = 2;
  protected static final int OPENJDK_VM = 3;
  protected static final int UNKNOWN_VM = 4;
  protected static final int[] VM_ID_LIST = {HOTSPOT_VM, JROCKIT_VM, IBM_VM, OPENJDK_VM, UNKNOWN_VM};
  protected static final String[] JVM_VENDOR_LIST = {"Sun Hotspot", "Oracle JRockit", "IBM", "OpenJDK", "Unknown"};
  protected String jvmVendor = JVM_VENDOR_LIST[JVM_VENDOR_LIST.length - 1];
  protected String jvmVersion;

  private static Logger theLogger = CustomLogger.getLogger(AbstractDumpParser.class.getSimpleName());

  // Used for deserialization
  public AbstractDumpParser() {
    lineChecker = new LineChecker();
  }

  protected AbstractDumpParser(LineNumberReader bis, DateMatcher dm) {
    maxCheckLines = PrefManager.get().getMaxRows();
    markSize = PrefManager.get().getStreamResetBuffer();
    millisTimeStamp = PrefManager.get().getMillisTimeStamp();
    setBis(bis);
    setDm(dm);
    initVars();
  }

  protected void initVars() {
    LOCKED = "- locked";
    BLOCKED_FOR_LOCK = "- waiting to lock";
    GENERAL_WAITING = "- waiting on ";
  }

  /**
   * strip the dump string from a given path
   *
   * @param path
   *          the treepath to check
   * @return dump string, if proper tree path, null otherwise.
   */
  protected String getDumpStringFromTreePath(TreePath path) {
    String[] elems = path.toString().split(",");
    if (elems.length > 1) {
      return (elems[elems.length - 1].substring(0, elems[elems.length - 1].lastIndexOf(']')).trim());
    } else {
      String pathStr = path.toString();
      return (pathStr.substring(1, pathStr.lastIndexOf(']')).trim());
    }
  }

  /**
   * find long running threads.
   *
   * @param root
   *          the root node to use for the result.
   * @param dumpStore
   *          the dump store to use
   * @param paths
   *          paths to the dumps to check
   * @param minOccurence
   *          the min occurrence of a long running thread
   * @param regex
   *          regex to be applied to the thread titles.
   */
  public void findLongRunningThreads(DefaultMutableTreeNode root, Map dumpStore, TreePath[] paths, int minOccurence,
          String regex) {
    diffDumps("Long running thread detection", root, dumpStore, paths, minOccurence, regex);
  }

  /**
   * merge the given dumps.
   *
   * @param root
   *          the root node to use for the result.
   * @param dumpStore
   *          the dump store tu use
   * @param dumps
   *          paths to the dumps to check
   * @param minOccurence
   *          the min occurrence of a long running thread
   * @param regex
   *          regex to be applied to the thread titles.
   */
  public void mergeDumps(DefaultMutableTreeNode root, Map dumpStore, TreePath[] dumps, int minOccurence, String regex) {
    diffDumps("Merge", root, dumpStore, dumps, minOccurence, regex);
  }

  protected void diffDumps(String prefix, DefaultMutableTreeNode root, Map dumpStore, TreePath[] dumps,
          int minOccurence, String regex) {

    boolean diffAcrossLogs = false;
    Vector keys = new Vector(dumps.length);
    Hashtable<String, String> tdKeyMapper = new Hashtable<String, String>();

    Vector<String> tdiKeys = new Vector<String>(dumps.length);
    Map<String, ThreadDumpInfo> tdiMap = new HashMap<String, ThreadDumpInfo>();

    String prevLogFilePath = null;
    for (int i = 0; i < dumps.length; i++) {
      String dumpName = getDumpStringFromTreePath(dumps[i]);
      String parentDump = getDumpStringFromTreePath(dumps[i].getParentPath());
      theLogger.finest("1-Dumps path: " + dumps[i] + ", parentPath: " + dumps[i].getParentPath() );
      theLogger.finest("2-Dumps path: " + dumpName + ", parentPath: " + parentDump);

      String currentLogFilePath = (dumpName.startsWith("Dump No")) ? parentDump : dumpName;
      if (prevLogFilePath == null) {
        prevLogFilePath = currentLogFilePath;
      }

      theLogger.finest("LogFile: " + currentLogFilePath);

      // Check if we are comparing against different log files
      if (!currentLogFilePath.equals(prevLogFilePath)) {
        diffAcrossLogs = true;
      }

      // Dont strip date/time if its a dump created using JMX connection
      if ((dumpName.indexOf(" at") > 0)
              && !(dumpName.startsWith("JMX Thread Dump") || dumpName.startsWith("Clipboard"))) {
        dumpName = dumpName.substring(0, dumpName.indexOf(" at")).trim();
      } else if (dumpName.indexOf(" around") > 0) {
        dumpName = dumpName.substring(0, dumpName.indexOf(" around")).trim();
      }

      theLogger.finest("Trimmed DumpName: " + dumpName);

      Integer tdiID = null;
      if (dumpName.contains("Dump No.")) {
        tdiID = Integer.parseInt(dumpName.replaceAll("Dump No.", "").trim());

      } else if (dumpName.contains(File.separator)) {

        // This is a comparison across multiple thread dump files that have no individual Dumps selected
        // Search for pattern like: javacore.20120130.103911.2883810.0001.txt
        int index = dumpName.lastIndexOf('.');
        String strippedName = dumpName;
        if (index > 0) {
          strippedName = dumpName.substring(0, index);
        }

        index = strippedName.lastIndexOf('.');
        if (index > 0) {
          strippedName = strippedName.substring(index + 1);
        }

        try {
          tdiID = Integer.parseInt(strippedName);
        } catch (Exception e) {
          tdiID = new Integer(i);
        }
      } else if (dumpName.startsWith("JMX Thread Dump") || dumpName.startsWith("Clipboard")) {
        // Dump generated via jmx mbean server connection in JConsole Plugin function
        // Format of generated dump:
        // JMX Thread Dump of com.jrockit.mc.rjmx.internal.MCMBeanServerConnection@341c80d4
        // at 2013-10-06 16:17:50
        tdiID = Integer.parseInt(dumpName.substring(dumpName.lastIndexOf(" ") + 1).replaceAll(":", "").trim());
      }


      // Its possible the thread dump got expanded
      // and its internal threads/threadgroups also got selected
      // Ignore such selection
      if (tdiID == null) {
        theLogger.finest("### Ignoring .. " + dumpName);
        continue;
      }
      tdKeyMapper.put(currentLogFilePath + tdiID, dumpName);
      tdiKeys.add(currentLogFilePath + tdiID);
      ThreadDumpInfo tdi = null;
      Object userObj = ((DefaultMutableTreeNode) dumps[i].getLastPathComponent()).getUserObject();
      if (userObj instanceof ThreadDumpInfo) {
        tdi = (ThreadDumpInfo) userObj;
      } else if (userObj instanceof Logfile) {
        Logfile logFile = (Logfile) userObj;
        tdi = logFile.getThreadDumps().get(0);
      }
      theLogger.finest("Saving in tdiMap: " + currentLogFilePath + tdiID + " = TdI: " + tdi);
      tdiMap.put(currentLogFilePath + tdiID, tdi);
    }

    // Sort the ordering by the thread dump ids along with log file names...
    if (diffAcrossLogs) {
      Collections.sort(tdiKeys);
    } else {
      // Thread dumps all belong to the same log file
      // Strip off the log file name and sort by dump instances...

      Vector<Integer> tdiIntKeys = new Vector<Integer>();
      for (String olderTdiKey : tdiKeys) {
        Integer tdiId = Integer.parseInt(olderTdiKey.substring(prevLogFilePath.length()));
        tdiIntKeys.add(tdiId);
      }
      Collections.sort(tdiIntKeys);
      tdiKeys.clear();
      for (Integer sortedTdiId : tdiIntKeys) {
        tdiKeys.add(prevLogFilePath + sortedTdiId);
      }
    }

    ArrayList<ThreadDumpInfo> tdiArrList = new ArrayList<ThreadDumpInfo>();
    for (String tdiKey : tdiKeys) {
      String dumpName = tdKeyMapper.get(tdiKey);
      keys.add(dumpName);
      tdiArrList.add(tdiMap.get(tdiKey));
    }

    int noOfValidDumps = tdiArrList.size();

    String info = prefix + " between " + keys.get(0) + " and " + keys.get(keys.size() - 1);
    if (diffAcrossLogs) {
      String startingLog = tdiMap.get(tdiKeys.get(0)).getLogFile().getName();
      String endingLog = tdiMap.get(tdiKeys.get(tdiKeys.size() - 1)).getLogFile().getName();

      // If the dump files are from clipboard, then the keys and logname are same...
      if (!keys.get(0).equals(startingLog)) {
        info = prefix + " between " + keys.get(0) + " of " + startingLog;
      } else {
        info = prefix + " between " + keys.get(0);
      }

      // If the dump files are from clipboard, then the keys and logname are same...
      if (!keys.get(keys.size() - 1).equals(endingLog)) {
        info = info + " and " + keys.get(keys.size() - 1) + " of " + endingLog;
      } else {
        info = info + " and " + keys.get(keys.size() - 1);
      }
    }

    ThreadDiffsTableCategory threadDiffsTableCategory = new ThreadDiffsTableCategory(info, IconFactory.DIFF_DUMPS);
    threadDiffsTableCategory.setThreadDumps(tdiArrList);

    DefaultMutableTreeNode catMerge = new DefaultMutableTreeNode(threadDiffsTableCategory);

    // If we are doing merge across different Log files, then add it as a new top node
    if (diffAcrossLogs) {
      ThreadLogic.getTopNodes().add(catMerge);
    } else {
      // diff of dumps within same log file, add it as child node
      root.add(catMerge);
    }

    int threadCount = 0;

    if (tdiArrList.get(0) != null) {

      Map<String, ThreadInfo> threadMap0 = tdiArrList.get(0).getThreadMap();

      ThreadInfo[] sortedThreads0 = threadMap0.values().toArray(new ThreadInfo[]{});
      Collection<ThreadInfo> sortedThreadCol0 = ThreadInfo.sortByHealth(sortedThreads0);

      Iterator dumpIter = sortedThreadCol0.iterator();
      while (dumpIter.hasNext()) {

        // The original dumpStore uses the full thread label as key which includes
        // the thread state
        // If the thread becomes blocked or unblocked, the state changes and so the
        // same thread key cannot be used across thread dumps..
        // Thread name is "ExecuteThread: '1' for queue: 'weblogic.socket.Muxer'"
        // id=29 idx=0xc0 tid=19931 prio=5 alive, blocked, native_blocked, daemon
        // Thread name to do searches across TDs should be just
        // "ExecuteThread: '1' for queue: 'weblogic.socket.Muxer'" without rest of
        // the labels
        // So use filtered Thread names for searches of threads across TDIs

        // Need to use a filtered thread name that does not carry any of the
        // states....
        ThreadInfo ti0 = (ThreadInfo) dumpIter.next();
        String threadNameId = ti0.getNameId();
        String originalThreadKey = ti0.getName();

        int occurence = 0;

        if (regex == null || regex.equals("") || threadNameId.matches(regex)) {
          for (int i = 1; i < tdiArrList.size(); i++) {
            Map<String, ThreadInfo> threadMap = tdiArrList.get(i).getThreadMap();
            if (threadMap.containsKey(threadNameId)) {
              occurence++;
            } else {
              theLogger.warning("Thread " + threadNameId + ", missing from Dump: " + i + " !!");
            }
          }

          if (occurence >= (noOfValidDumps - 1)) {
            threadCount++;

            String timeTaken0 = (tdiArrList.get(0).getStartTime() != null) ? tdiArrList.get(0).getStartTime() : "N/A";

            // Add Dump as keyword to recognize the dump individually later
            String dumpKey0 = keys.get(0).toString();
            if (!dumpKey0.startsWith(DUMP_MARKER)) {
              dumpKey0 = DUMP_MARKER + dumpKey0;
            }

            StringBuffer content = new StringBuffer("<b><font size=").append(ThreadLogic.getFontSizeModifier(-1)).append(">").append(dumpKey0 + ", Timestamp: " + timeTaken0).append("</b></font><hr><pre><font size=").append(ThreadLogic.getFontSizeModifier(-1)).append(">");

            // Embed health for the given thread from each of the dumps
            content.append("<font size=4>Health: ");
            ThreadLogic.appendHealth(content, ti0);
            content.append("</font><br>");

            if (ti0.getAdvisories().size() > 0) {
              content.append("<font size=4>Advisories: ");
              for (Iterator<ThreadAdvisory> iter = ti0.getAdvisories().iterator(); iter.hasNext();) {
                ThreadAdvisory adv = iter.next();
                ThreadLogic.appendAdvisoryLink(content, adv);
              }
              content.append("</font><br><br>");
            }

            if (ti0.getCtxData() != null) {
              content.append("<font size=4>Context Data: </font><font size=3>");
              String[] ctxDataSet = ti0.getCtxData().split(ThreadInfo.CONTEXT_DATA_SEPARATOR);
              for (String contextData : ctxDataSet) {
                content.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;" + contextData);
              }
              content.append("</font><br>");
            }
            content.append(fixMonitorLinks(ti0.getContent(), (String) keys.get(0)));

            int maxLines = 0;
            ThreadInfo lastThreadInMerge = ti0;

            for (int i = 1; i < tdiArrList.size(); i++) {
              if (tdiArrList.get(i).getThreadMap().containsKey(threadNameId)) {
                String timeTaken = (tdiArrList.get(i).getStartTime() != null) ? tdiArrList.get(i).getStartTime() : "N/A";
                Map<String, ThreadInfo> cmpThreadMap = tdiArrList.get(i).getThreadMap();
                ThreadInfo cmpThreadInfo = cmpThreadMap.get(threadNameId);

                // Add Dump as keyword to recognize the dump individually later
                String dumpKey = keys.get(i).toString();
                if (!dumpKey.startsWith(DUMP_MARKER)) {
                  dumpKey = DUMP_MARKER + dumpKey;
                }

                content.append("\n\n</pre><b><font size=");
                content.append(ThreadLogic.getFontSizeModifier(-1));
                content.append(">");
                content.append(dumpKey + ", Timestamp: " + timeTaken);
                content.append("</font></b><hr><pre><font size=");
                content.append(ThreadLogic.getFontSizeModifier(-1));
                content.append(">");

                // Embed health for the given thread from each of the dumps
                content.append("<font size=4>Health: ");
                ThreadLogic.appendHealth(content, cmpThreadInfo);
                content.append("</font><br>");

                // Embed advisories for the given thread from each of the dumps
                if (cmpThreadInfo.getAdvisories().size() > 0) {
                  content.append("<font size=4>Advisories: ");
                  for (Iterator<ThreadAdvisory> iter = cmpThreadInfo.getAdvisories().iterator(); iter.hasNext();) {
                    ThreadAdvisory adv = iter.next();
                    ThreadLogic.appendAdvisoryLink(content, adv);
                  }
                  content.append("</font><br><br>");
                }

                if (cmpThreadInfo.getCtxData() != null) {
                  content.append("<font size=4>Context Data: </font><font size=3>");
                  String[] ctxDataSet = cmpThreadInfo.getCtxData().split(ThreadInfo.CONTEXT_DATA_SEPARATOR);
                  for (String contextData : ctxDataSet) {
                    content.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;" + contextData);
                  }
                  content.append("</font><br>");
                }

                content.append(fixMonitorLinks(cmpThreadInfo.getContent(), (String) keys.get(i)));
                int countLines = countLines(cmpThreadInfo.getContent());
                maxLines = maxLines > countLines ? maxLines : countLines;

                lastThreadInMerge = cmpThreadInfo;
              } else {
                theLogger.warning("Unable to find Thread with name:id as : " + threadNameId);
              }
            }

            ThreadInfo threadInfo = new ThreadInfo(originalThreadKey, null, content.toString(), maxLines, getThreadTokens(originalThreadKey));
            threadInfo.setHealth(lastThreadInMerge.getHealth());
            threadInfo.setState(lastThreadInMerge.getState());
            addToCategory(catMerge, threadInfo);
          }
        }
      }
    }

    StringBuffer diffInfo = new StringBuffer(getStatInfo(keys, prefix, noOfValidDumps, threadCount));
    diffInfo.append(ThreadDumpInfo.getThreadDumpsOverview(tdiArrList));

    ((Category) catMerge.getUserObject()).setInfo(diffInfo.toString());
  }

  /**
   * count lines of input string.
   *
   * @param input
   * @return line count
   */
  private int countLines(String input) {
    int pos = 0;
    int count = 0;
    while (input.indexOf('\n', pos) > 0) {
      count++;
      pos = input.indexOf('\n', pos) + 1;
    }

    return (count);
  }

  /**
   * generate statistical information concerning the merge on long running
   * thread detection.
   *
   * @param keys
   *          the dump node keys
   * @param prefix
   *          the prefix of the run (either "Merge" or
   *          "Long running threads detection"
   * @param minOccurence
   *          the minimum occurence of threads
   * @param threadCount
   *          the overall thread count of this run.
   * @return
   */
  private String getStatInfo(Vector keys, String prefix, int minOccurence, int threadCount) {
    StringBuffer statData = new StringBuffer("<font face=System><b><font face=System> ");

    statData.append("<b>" + prefix + "</b><hr><p><i>");
    for (int i = 0; i < keys.size(); i++) {
      statData.append(keys.get(i));
      if (i < keys.size() - 1) {
        statData.append(", ");
      }
    }
    statData.append("</i></p><br>" + "<table border=0><tr bgcolor=\"#dddddd\"><td><font face=System "
            + ">Overall Thread Count</td><td width=\"150\"></td><td><b><font face=System>");
    statData.append(threadCount);
    statData.append("</b></td></tr>");
    statData.append("<tr bgcolor=\"#eeeeee\"><td><font face=System "
            + ">Minimum Occurence of threads</td><td width=\"150\"></td><td><b><font face=System>");
    statData.append(minOccurence);
    statData.append("</b></td></tr>");

    if (threadCount != 0) {

      statData.append("<tr bgcolor=\"#dddddd\"><td><font face=System "
              + ">Advisories and Health reported are based on the last captured thread dump</td><td width=\"150\"></td><td><b><font face=System>");
      statData.append(keys.get(keys.size() - 1));
      statData.append("</b></td></tr>");

    } else {
      statData.append("<tr bgcolor=\"#ffffff\"<td></td></tr>");
      statData.append("<tr bgcolor=\"#cccccc\"><td colspan=2><font face=System "
              + "><p>No threads were found which occured at least " + minOccurence + " times.<br>"
              + "You should check your dumps for long running threads " + "or adjust the minimum occurence.</p>");
    }

    statData.append("</table>");

    return statData.toString();
  }

  /**
   * fix the monitor links for proper navigation to the monitor in the right
   * dump.
   *
   * @param fixString
   *          the string to fix
   * @param dumpName
   *          the dump name to reference
   * @return the fixed string.
   */
  private String fixMonitorLinks(String fixString, String dumpName) {
    if (fixString.indexOf("monitor://") > 0) {
      fixString = fixString.replaceAll("monitor://", "monitor://" + dumpName + "/");
    }
    return (fixString);
  }

  /**
   * create a tree node with the provided information
   *
   * @param top
   *          the parent node the new node should be added to.
   * @param title
   *          the title of the new node
   * @param info
   *          the info part of the new node
   * @param content
   *          the content part of the new node
   * @see ThreadInfo
   */
  protected void createNode(DefaultMutableTreeNode top, String title, String info, String content, int lineCount) {
    DefaultMutableTreeNode threadInfo = null;
    String[] tokens = getThreadTokens(title);
    if (tokens == null || tokens.length == 0) {
      return;
    }

    threadInfo = new DefaultMutableTreeNode(new ThreadInfo(title, info, content, lineCount, tokens));
    top.add(threadInfo);
  }

  /**
   * create a category entry for a category (categories are "Monitors",
   * "Threads waiting", e.g.). A ThreadInfo instance will be created with the
   * passed information.
   *
   * @param category
   *          the category the node should be added to.
   * @param title
   *          the title of the new node
   * @param info
   *          the info part of the new node
   * @param content
   *          the content part of the new node
   * @param lineCount
   *          the line count of the thread stack, 0 if not applicable for this
   *          element.
   * @see ThreadInfo
   */
  protected void addToCategory(DefaultMutableTreeNode category, String title, StringBuffer info, String content,
          int lineCount, boolean parseTokens) {
    DefaultMutableTreeNode threadInfo = null;
    threadInfo = new DefaultMutableTreeNode(new ThreadInfo(title, info != null ? info.toString() : null, content,
            lineCount, parseTokens ? getThreadTokens(title) : null));
    ((Category) category.getUserObject()).addToCatNodes(threadInfo);
  }

  protected void addToCategory(DefaultMutableTreeNode category, ThreadInfo info) {
    DefaultMutableTreeNode threadInfo = new DefaultMutableTreeNode(info);
    ((Category) category.getUserObject()).addToCatNodes(threadInfo);
  }

  /**
   * create a category entry for a category (categories are "Monitors",
   * "Threads waiting", e.g.). A ThreadInfo instance will be created with the
   * passed information.
   *
   * @param category
   *          the category the node should be added to.
   * @param tdi
   *          the associated thread dump.
   * @param title
   *          the title of the new node
   * @param info
   *          the info part of the new node
   * @param content
   *          the content part of the new node
   * @param lineCount
   *          the line count of the thread stack, 0 if not applicable for this
   *          element.
   * @see ThreadInfo
   */
  protected void addToCategory(DefaultMutableTreeNode category, ThreadDumpInfo tdi, String title, StringBuffer info,
          String content, int lineCount, boolean parseTokens) {
    DefaultMutableTreeNode threadInfo = null;
    String[] tokens = parseTokens ? getThreadTokens(title) : null;
    if (parseTokens && ((tokens == null) || (tokens.length == 0))) {
      return;
    }

    ThreadInfo newThread = new ThreadInfo(title, info != null ? info.toString() : null, content, lineCount,
            tokens);
    newThread.setParentThreadDump(tdi);

    threadInfo = new DefaultMutableTreeNode(newThread);
    ((Category) category.getUserObject()).addToCatNodes(threadInfo);
  }

  /**
   * get the stream to parse
   *
   * @return stream or null if none is set up
   */
  protected LineNumberReader getBis() {
    return bis;
  }

  /**
   * parse the thread tokens for table display.
   *
   * @param title
   */
  protected abstract String[] getThreadTokens(String title);

  /**
   * set the stream to parse
   *
   * @param bis
   *          the stream
   */
  protected void setBis(LineNumberReader bis) {
    this.bis = bis;
  }

  /**
   * set the dump histogram counter to the specified value to force to start
   * (bottom to top) from the specified thread dump.
   */
  public void setDumpHistogramCounter(int value) {
    dumpHistogramCounter = value;
  }

  /**
   * retrieve the next node for adding histogram information into the tree.
   *
   * @param root
   *          the root to use for search.
   * @return node to use for append.
   */
  protected DefaultMutableTreeNode getNextDumpForHistogram(DefaultMutableTreeNode root) {
    if (dumpHistogramCounter == -1) {
      // -1 as index starts with 0.
      dumpHistogramCounter = root.getChildCount() - 1;
    }
    DefaultMutableTreeNode result = null;

    if (dumpHistogramCounter > 0) {
      result = (DefaultMutableTreeNode) root.getChildAt(dumpHistogramCounter);
      dumpHistogramCounter--;
    }

    return result;
  }

  /**
   * close this dump parser, also closes the passed dump stream
   */
  public void close() throws IOException {
    if (getBis() != null) {
      getBis().close();
    }
  }

  /**
   * get the maximum size for the mark buffer while reading the log file stream.
   *
   * @return size, default is 16KB.
   */
  protected int getMarkSize() {
    return markSize;
  }

  /**
   * set the maximum mark size.
   *
   * @param markSize
   *          the size to use, default is 16KB.
   */
  protected void setMarkSize(int markSize) {
    this.markSize = markSize;
  }

  /**
   * specifies the maximum amounts of lines to check if the dump is followed by
   * a class histogram or a deadlock.
   *
   * @return the amount of lines to check, defaults to 10.
   */
  protected int getMaxCheckLines() {
    return maxCheckLines;
  }

  public void setMaxCheckLines(int maxCheckLines) {
    this.maxCheckLines = maxCheckLines;
  }

  /**
   * @return true, if the time stamp is in milliseconds.
   */
  public boolean isMillisTimeStamp() {
    return millisTimeStamp;
  }

  public void setMillisTimeStamp(boolean millisTimeStamp) {
    this.millisTimeStamp = millisTimeStamp;
  }

  public DateMatcher getDm() {
    return dm;
  }

  public void setDm(DateMatcher dm) {
    this.dm = dm;
  }

  /**
   * dump the monitor information
   *
   * @param catMonitors
   * @param catMonitorsLocks
   * @param mmap
   * @return
   */
  protected int[] dumpMonitors(DefaultMutableTreeNode catMonitors, DefaultMutableTreeNode catMonitorsLocks,
          MonitorMap mmap) {
    Iterator iter = mmap.iterOfKeys();
    int monitorsWithoutLocksCount = 0;
    int overallThreadsWaiting = 0;
    while (iter.hasNext()) {
      String monitor = (String) iter.next();
      Map[] threads = mmap.getFromMonitorMap(monitor);
      ThreadInfo mi = new ThreadInfo(monitor, null, "", 0, null);
      DefaultMutableTreeNode monitorNode = new DefaultMutableTreeNode(mi);

      // first the locks
      Iterator iterLocks = threads[MonitorMap.LOCK_THREAD_POS].keySet().iterator();
      int locks = 0;
      int sleeps = 0;
      int waits = 0;
      while (iterLocks.hasNext()) {
        String thread = (String) iterLocks.next();
        String stackTrace = (String) threads[MonitorMap.LOCK_THREAD_POS].get(thread);
        if (threads[MonitorMap.SLEEP_THREAD_POS].containsKey(thread)) {
          createNode(monitorNode, "locks and sleeps on monitor: " + thread, null, stackTrace, 0);
          sleeps++;
        } else if (threads[MonitorMap.WAIT_THREAD_POS].containsKey(thread)) {
          createNode(monitorNode, "locks and waits on monitor: " + thread, null, stackTrace, 0);
          sleeps++;
        } else {
          createNode(monitorNode, "locked by " + thread, null, stackTrace, 0);
        }
        locks++;
      }

      Iterator iterWaits = threads[MonitorMap.WAIT_THREAD_POS].keySet().iterator();
      while (iterWaits.hasNext()) {
        String thread = (String) iterWaits.next();
        if (thread != null && !threads[MonitorMap.LOCK_THREAD_POS].containsKey(thread)) {
          createNode(monitorNode, "waits on monitor: " + thread, null,
                  (String) threads[MonitorMap.WAIT_THREAD_POS].get(thread), 0);
          waits++;
        }
      }

      Iterator iterSleeps = threads[MonitorMap.SLEEP_THREAD_POS].keySet().iterator();
      while (iterSleeps.hasNext()) {
        String thread = (String) iterSleeps.next();
        if (thread != null && !threads[MonitorMap.LOCK_THREAD_POS].containsKey(thread)) {
          createNode(monitorNode, "sleeps on monitor: " + thread, null,
                  (String) threads[MonitorMap.SLEEP_THREAD_POS].get(thread), 0);
          sleeps++;
        }
      }

      mi.setContent(ThreadDumpInfo.getMonitorInfo(locks, waits, sleeps));
      mi.setName(mi.getName() + ":    " + (sleeps) + " Thread(s) sleeping, " + (waits) + " Thread(s) waiting, "
              + (locks) + " Thread(s) locking");
      if (ThreadDumpInfo.areALotOfWaiting(waits)) {
        mi.setALotOfWaiting(true);
      }
      mi.setChildCount(monitorNode.getChildCount());

      ((Category) catMonitors.getUserObject()).addToCatNodes(monitorNode);
      if (locks == 0) {
        monitorsWithoutLocksCount++;
        overallThreadsWaiting += waits;
        ((Category) catMonitorsLocks.getUserObject()).addToCatNodes(monitorNode);
      }
    }
    return new int[]{monitorsWithoutLocksCount, overallThreadsWaiting};
  }

  protected int[] dumpBlockingMonitors(DefaultMutableTreeNode catLockingTree, MonitorMap mmap) {
    Map directChildMap = new HashMap(); // Top level of our display model

    // ******************************************************************
    // Figure out what threads are blocking and what threads are blocked
    // ******************************************************************
    int blockedThreads = fillBlockingThreadMaps(mmap, directChildMap);
    int contendedLocks = directChildMap.size();

    // ********************************************************************
    // Renormalize this from a flat tree (depth==1) into a structured tree
    // ********************************************************************
    renormalizeBlockingThreadTree(mmap, directChildMap);

    // ********************************************************************
    // Recalculate the number of blocked threads and add remaining top-level
    // threads to our display model
    // ********************************************************************
    for (Iterator iter = directChildMap.entrySet().iterator(); iter.hasNext();) {
      DefaultMutableTreeNode threadNode = (DefaultMutableTreeNode) ((Map.Entry) iter.next()).getValue();

      updateChildCount(threadNode, true);
      ((Category) catLockingTree.getUserObject()).addToCatNodes(threadNode);
    }

    directChildMap.clear();
    return new int[]{contendedLocks, blockedThreads};
  }

  /**
   * check threads in given thread dump and add appropriate custom categories
   * (if any defined).
   *
   * @param tdi
   *          the thread dump info object.
   */
  public void addCustomCategories(DefaultMutableTreeNode threadDump) {
    ThreadDumpInfo tdi = (ThreadDumpInfo) threadDump.getUserObject();
    Category threads = tdi.getThreads();
    ListModel cats = PrefManager.get().getCategories();
    for (int i = 0; i < cats.getSize(); i++) {
      Category cat = new TableCategory(((CustomCategory) cats.getElementAt(i)).getName(), IconFactory.CUSTOM_CATEGORY);
      for (int j = 0; j < threads.getNodeCount(); j++) {
        Iterator filterIter = ((CustomCategory) cats.getElementAt(i)).iterOfFilters();
        boolean matches = true;
        ThreadInfo ti = (ThreadInfo) ((DefaultMutableTreeNode) threads.getNodeAt(j)).getUserObject();
        while (matches && filterIter.hasNext()) {
          Filter filter = (Filter) filterIter.next();
          matches = filter.matches(ti, true);
        }

        if (matches) {
          cat.addToCatNodes(new DefaultMutableTreeNode(ti));
        }
      }
      if (cat.getNodeCount() > 0) {
        cat.setName(cat.getName() + " (" + cat.getNodeCount() + " Threads)");
        threadDump.add(new DefaultMutableTreeNode(cat));
      }
    }
  }

  protected void addCategories(DefaultMutableTreeNode threadDump) {
    ThreadDumpInfo tdi = (ThreadDumpInfo) threadDump.getUserObject();
    Category threads = tdi.getThreads();
    if (threads.getNodeCount() == 0) {
      return;
    }

    Category advisoryMapCategory = new TableCategory("Advisory Map",
            IconFactory.CUSTOM_CATEGORY, false);

    StringBuffer advisoryMapBuf = new StringBuffer();
    advisoryMapBuf.append("<font face=System ").append("><table border=0><tr bgcolor=\"#dddddd\" ><td><font face=System ").append("><b>Advisory Map</b></td></tr><tr></tr><tr><td><font face=System>");

    advisoryMapBuf.append("All known advisories loaded inside ThreadLogic are listed in the map.<br>").append("These are based on Advisory.xml packaged within ThreadLogic and also from any user specified advisories.<br>").append("Known advisories will be matched against threads and reported.<br><br>");

    advisoryMapBuf.append("CAUTION!! This map shows only list of possible advisories to match against").append(" and does not imply all of them were found in the thread dump.<br>").append("Check the logged messages at ThreadLogic startup on how-to add custom advisories or thread groups.<br><br>");

    advisoryMapBuf.append("Select <a href=\"threadgroups://\"><b>Thread Groups Summary</b></a> node").append(" to view the overall thread group categories and details along with matched top-level advisories.<br>").append("Expand the Thread Groups Summary ").append("node to view the categorization of threads into WebLogic and Non-WebLogic groups.<br>");

    advisoryMapBuf.append("There will be nested thread groups within each of the WLS & Non-WLS groups.<br><br>");

    advisoryMapBuf.append("Advisories would be reported at both individual thread level and Thread Group level.<br>").append("Select individual thread entries within the Thread Groups for detailed analysis of each thread.").append("</td></tr></table>");

    advisoryMapCategory.setInfo(advisoryMapBuf.toString());
    DefaultMutableTreeNode advisoryNode = new DefaultMutableTreeNode(advisoryMapCategory);

    Collection<ThreadAdvisory> advisoryList = ThreadAdvisory.getAdvisoryList();
    for (ThreadAdvisory advisory : advisoryList) {

      DefaultMutableTreeNode advisoryInfo = new DefaultMutableTreeNode(advisory);
      ((Category) advisoryNode.getUserObject()).addToCatNodes(advisoryInfo);
    }
    threadDump.add(advisoryNode);

    ExternalizedNestedThreadGroupsCategory threadGroupsCat = new ExternalizedNestedThreadGroupsCategory();
    threadGroupsCat.setThreads(threads);

    Collection<ThreadGroup> tgList = threadGroupsCat.getThreadGroups();
    tdi.setThreadGroups(tgList);

    if (tdi.hasDeadlock()) {
      Category deadlockCategory = new TableCategory("Deadlock", IconFactory.DEADLOCKS, false);
      deadlockCategory.setInfo(tdi.getDeadlockedInfo());
      DefaultMutableTreeNode deadlockNode = new DefaultMutableTreeNode(deadlockCategory);

      for (ThreadInfo deadlockedTi : tdi.getDeadlockedThreads()) {
        DefaultMutableTreeNode deadlockedThread = new DefaultMutableTreeNode(deadlockedTi);
        ((Category) deadlockNode.getUserObject()).addToCatNodes(deadlockedThread);
      }
      threadDump.add(deadlockNode);
    }

    DefaultMutableTreeNode threadGroupNode = createCustomTree(threadDump, threadGroupsCat, threads);
    Category threadGrpCategory = new TableCategory("Thread Groups Summary", IconFactory.THREADS, true);
    threadGrpCategory.setInfo(tdi.getTGSummaryOverview());
    threadGroupNode.setUserObject(threadGrpCategory);

    for (ThreadGroup tg : threadGroupsCat.getThreadGroups()) {
      DefaultMutableTreeNode tgInfo = new DefaultMutableTreeNode(tg);
      ((Category) threadGroupNode.getUserObject()).addToCatNodes(tgInfo);
    }

  }

  private DefaultMutableTreeNode createCustomTree(DefaultMutableTreeNode rootNode, NestedCategory category,
          Category threads) {
    DefaultMutableTreeNode leafNode = new DefaultMutableTreeNode(new TreeCategory(category.getName(),
            category.getIconID(), true));

    Category parentCategory = null;
    NestedCategory parent = category.getParent();

    if (parent != null) {
      parentCategory = new TableCategory(category.getName(), category.getIconID());
      parentCategory.setInfo((parent.getFilter(category.getName()).getInfo()));
      leafNode = applyFilter(parentCategory, parent.getFilter(category.getName()), threads, rootNode);
    } else {
      parentCategory = threads;
      rootNode.add(leafNode);
    }

    for (Iterator<Filter> filterIter = category.iterOfFilters(); filterIter != null && filterIter.hasNext();) {
      Filter filter = (Filter) filterIter.next();
      Category cat = new TableCategory(filter.getName(), category.getIconID());

      cat.setInfo(filter.getInfo());
      DefaultMutableTreeNode newNode = applyFilter(cat, filter, parentCategory, leafNode);

      if (cat.getNodeCount() > 0) {
        NestedCategory nestedCat = category.getSubCategory(filter.getName());
        if (nestedCat != null) {
          for (Iterator<NestedCategory> subCatIter = category.getSubCategory(filter.getName()).getSubCategoriesIterator(); subCatIter != null && subCatIter.hasNext();) {
            NestedCategory nestedCat1 = (NestedCategory) subCatIter.next();
            createCustomTree(newNode, nestedCat1, cat);
          }
        }
      }
    }
    return leafNode;
  }

  private DefaultMutableTreeNode applyFilter(Category cat, Filter filter, Category threads,
          DefaultMutableTreeNode leafNode) {
    DefaultMutableTreeNode newNode = null;
    for (int i = 0; i < threads.getNodeCount(); i++) {
      boolean matches = true;
      ThreadInfo ti = (ThreadInfo) ((DefaultMutableTreeNode) threads.getNodeAt(i)).getUserObject();
      matches = filter.matches(ti, true);

      if (matches) {
        cat.addToCatNodes(new DefaultMutableTreeNode(ti));
      }
    }
    if (cat.getNodeCount() > 0) {
      cat = sortThreadsByHealth(cat);
      cat.setName(cat.getName() + " (" + cat.getNodeCount() + " Threads)");
      newNode = new DefaultMutableTreeNode(cat);
      DefaultMutableTreeNode lockedNode = createLockedThreadsNode(cat);
      if (lockedNode != null) {
        newNode.add(lockedNode);
      }

      leafNode.add(newNode);
    }
    return newNode;
  }

  public Category sortThreadsByHealth(Category unsortedThreadsCat) {
    ArrayList<ThreadInfo> threadList = new ArrayList<ThreadInfo>();
    for (int i = 0; i < unsortedThreadsCat.getNodeCount(); i++) {
      ThreadInfo ti = (ThreadInfo) ((DefaultMutableTreeNode) unsortedThreadsCat.getNodeAt(i)).getUserObject();
      threadList.add(ti);
    }

    // Sort the thread groups and nested threads by health
    threadList = ThreadInfo.sortByHealth(threadList);

    Category sortedThreadsCat = new TableCategory(unsortedThreadsCat.getName(), unsortedThreadsCat.getIconID());
    sortedThreadsCat.setInfo(unsortedThreadsCat.getInfo());
    for (int i = 0; i < threadList.size(); i++) {
      ThreadInfo ti = threadList.get(i);
      DefaultMutableTreeNode node = new DefaultMutableTreeNode();
      node.setUserObject(ti);
      sortedThreadsCat.addToCatNodes(node);
    }
    return sortedThreadsCat;
  }

  private void checkAndMarkWithBlockedIcon(NestedCategory cat) {
    String nameLower = cat.getName();
    Pattern problematicFilterPattern = Pattern.compile("(blocked)|(warning)|(stuck)|(hogging)");
    Matcher m = problematicFilterPattern.matcher(nameLower);
    if (m.find()) {
      cat.setAsBlockedIcon();
    }
  }

  private DefaultMutableTreeNode createLockedThreadsNode(Category threads) {
    // Create node for locked monitors
    // Add locked section based on threads for this group (cat)

    DefaultMutableTreeNode lockedThreadNode = null;
    Category lockedCategory = null;
    int count = 0;
    int blockedThread = 0;
    for (Iterator monitorMapiIter = mmap.iterOfKeys(); monitorMapiIter.hasNext();) {
      String monitor = (String) monitorMapiIter.next();
      Map[] monitorThreads = mmap.getFromMonitorMap(monitor);

      String lockedThread = getLockingThread(monitorThreads);
      for (int i = 0; i < threads.getNodeCount(); i++) {
        ThreadInfo ti = (ThreadInfo) ((DefaultMutableTreeNode) threads.getNodeAt(i)).getUserObject();
        if (ti.getName().equals(lockedThread) && monitorThreads[MonitorMap.WAIT_THREAD_POS].size() > 0) {
          count++;

          if (lockedThreadNode == null) {
            lockedCategory = new TreeCategory("Holding Locks", IconFactory.CUSTOM_CATEGORY, false);
            lockedThreadNode = new DefaultMutableTreeNode(lockedCategory);
          }

          ThreadInfo tmi = new ThreadInfo("Thread - " + lockedThread, null, "", 0, null);
          DefaultMutableTreeNode threadNode = new DefaultMutableTreeNode(tmi);

          ThreadInfo mmi = new ThreadInfo("Monitor - " + monitor, null, "", 0, null);
          DefaultMutableTreeNode monitorNode = new DefaultMutableTreeNode(mmi);
          threadNode.add(monitorNode);

          // Look over all threads blocked on this monitor
          for (Iterator iterWaits = monitorThreads[MonitorMap.WAIT_THREAD_POS].keySet().iterator(); iterWaits.hasNext();) {
            String thread = (String) iterWaits.next();
            // Skip the thread that has this monitor locked
            if (thread != null && !monitorThreads[MonitorMap.LOCK_THREAD_POS].containsKey(thread)) {
              blockedThread++;
              createNode(monitorNode, "Thread - " + thread, null,
                      (String) monitorThreads[MonitorMap.WAIT_THREAD_POS].get(thread), 0);
            }
          }

          String blockingStackFrame = (String) monitorThreads[MonitorMap.LOCK_THREAD_POS].get(lockedThread);
          tmi.setContent(blockingStackFrame);
          mmi.setContent("This monitor (" + linkifyMonitor(monitor) + ") is held in the following stack frame:\n\n"
                  + blockingStackFrame);
          ((Category) lockedThreadNode.getUserObject()).addToCatNodes(threadNode);
        }
      }
    }
    if (lockedCategory != null) {
      lockedCategory.setName("Holding Locks " + "(" + blockedThread + " threads blocked by " + count + " monitors)");
    }
    return lockedThreadNode;
  }

  protected void renormalizeThreadDepth(DefaultMutableTreeNode threadNode1) {
    for (Enumeration e = threadNode1.children(); e.hasMoreElements();) {
      DefaultMutableTreeNode monitorNode2 = (DefaultMutableTreeNode) e.nextElement();
      for (int ii = 0; ii < monitorNode2.getChildCount(); ii++) {
        renormalizeMonitorDepth(monitorNode2, ii);
      }
    }
  }

  private void renormalizeMonitorDepth(DefaultMutableTreeNode monitorNode, int index) {
    // First, remove all duplicates of the item at index "index"
    DefaultMutableTreeNode threadNode1 = (DefaultMutableTreeNode) monitorNode.getChildAt(index);
    ThreadInfo mi1 = (ThreadInfo) threadNode1.getUserObject();
    int i = index + 1;
    while (i < monitorNode.getChildCount()) {
      DefaultMutableTreeNode threadNode2 = (DefaultMutableTreeNode) monitorNode.getChildAt(i);
      ThreadInfo mi2 = (ThreadInfo) threadNode2.getUserObject();
      if (mi1.getName().equals(mi2.getName())) {
        if (threadNode2.getChildCount() > 0) {
          threadNode1.add((DefaultMutableTreeNode) threadNode2.getFirstChild());
          monitorNode.remove(i);
          continue;
        }
      }
      i++;
    }

    // Second, recurse into item "index"
    renormalizeThreadDepth(threadNode1);
  }

  private int fillBlockingThreadMaps(MonitorMap mmap, Map directChildMap) {
    int blockedThread = 0;
    for (Iterator iter = mmap.iterOfKeys(); iter.hasNext();) {
      String monitor = (String) iter.next();
      Map[] threads = mmap.getFromMonitorMap(monitor);

      // Only one thread can really be holding this monitor, so find the thread
      String threadLine = getLockingThread(threads);
      ThreadInfo tmi = new ThreadInfo("Thread - " + threadLine, null, "", 0, null);
      DefaultMutableTreeNode threadNode = new DefaultMutableTreeNode(tmi);

      ThreadInfo mmi = new ThreadInfo("Monitor - " + monitor, null, "", 0, null);
      DefaultMutableTreeNode monitorNode = new DefaultMutableTreeNode(mmi);
      threadNode.add(monitorNode);

      // Look over all threads blocked on this monitor
      for (Iterator iterWaits = threads[MonitorMap.WAIT_THREAD_POS].keySet().iterator(); iterWaits.hasNext();) {
        String thread = (String) iterWaits.next();
        // Skip the thread that has this monitor locked
        if (thread != null && !threads[MonitorMap.LOCK_THREAD_POS].containsKey(thread)) {
          blockedThread++;
          createNode(monitorNode, "Thread - " + thread, null, (String) threads[MonitorMap.WAIT_THREAD_POS].get(thread),
                  0);
        }
      }

      String blockingStackFrame = (String) threads[MonitorMap.LOCK_THREAD_POS].get(threadLine);
      if (blockingStackFrame != null) {
        tmi.setContent(blockingStackFrame);
        mmi.setContent("This monitor (" + linkifyMonitor(monitor) + ") is held in the following stack frame:\n\n"
                + blockingStackFrame);
      } else {
        mmi.setContent("Owner of this monitor (" + linkifyMonitor(monitor) + ") could not be located");
      }

      // If no-one is blocked on or waiting for this monitor, don't show it
      if (monitorNode.getChildCount() > 0) {
        directChildMap.put(monitor, threadNode);
      }
    }
    return blockedThread;
  }

  protected abstract String linkifyMonitor(String monitor);

  private void updateChildCount(DefaultMutableTreeNode threadOrMonitorNode, boolean isThreadNode) {
    int count = 0;
    for (Enumeration e = threadOrMonitorNode.depthFirstEnumeration(); e.hasMoreElements();) {
      Object element = e.nextElement();
      ThreadInfo mi = (ThreadInfo) (((DefaultMutableTreeNode) element).getUserObject());
      if (mi.getName().startsWith("Thread")) {
        count++;
      }
    }

    ThreadInfo mi = (ThreadInfo) threadOrMonitorNode.getUserObject();

    // Don't mark the thread category as having Lot of Waiters
    // if the threads are waiting for java.util.concurrent.locks.AbstractQueuedSynchronizer
    /*
     *
     * "pool-3-thread-8" id=91 idx=0x168 tid=22940 prio=5 alive, parked, native_blocked
    -- Parking to wait for: java/util/concurrent/locks/AbstractQueuedSynchronizer$ConditionObject@0x140211ee0
    at jrockit/vm/Locks.park0(J)V(Native Method)
    at jrockit/vm/Locks.park(Locks.java:2230)
    at sun/misc/Unsafe.park(ZJ)V(Native Method)
    at java/util/concurrent/locks/LockSupport.park(LockSupport.java:156)
    at java/util/concurrent/locks/AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1987)
    at java/util/concurrent/LinkedBlockingQueue.take(LinkedBlockingQueue.java:399)
    at java/util/concurrent/ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:947)
    at java/util/concurrent/ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:907)
     *
     */
    if (!mi.getName().contains("java/util/concurrent/locks/AbstractQueuedSynchronizer") && ThreadDumpInfo.areALotOfWaiting(count)) {
      mi.setALotOfWaiting(true);
    }
    if (isThreadNode) {
      count--;
    }

    mi.setChildCount(count);
    if (count > 1) {
      mi.setName(mi.getName() + ":    " + count + " Blocked threads");
    } else if (count == 1) {
      mi.setName(mi.getName() + ":    " + count + " Blocked thread");
    }

    // Recurse
    for (Enumeration e = threadOrMonitorNode.children(); e.hasMoreElements();) {
      updateChildCount((DefaultMutableTreeNode) e.nextElement(), !isThreadNode);
    }
  }

  private void renormalizeBlockingThreadTree(MonitorMap mmap, Map directChildMap) {
    Map allBlockingThreadsMap = new HashMap(directChildMap); // All threads that
    // are blocking at
    // least one other
    // thread

    // First, renormalize based on monitors to get our unique tree
    // Tree will be unique as long as there are no deadlocks aka monitor loops
    for (Iterator iter = mmap.iterOfKeys(); iter.hasNext();) {
      String monitor1 = (String) iter.next();
      Map[] threads1 = mmap.getFromMonitorMap(monitor1);

      DefaultMutableTreeNode thread1Node = (DefaultMutableTreeNode) allBlockingThreadsMap.get(monitor1);
      if (thread1Node == null) {
        continue;
      }

      // Get information on the one thread holding this lock
      Iterator it = threads1[MonitorMap.LOCK_THREAD_POS].keySet().iterator();
      if (!it.hasNext()) {
        continue;
      }
      String threadLine1 = (String) it.next();

      for (Iterator iter2 = mmap.iterOfKeys(); iter2.hasNext();) {
        String monitor2 = (String) iter2.next();
        if (monitor1 == monitor2) {
          continue;
        }

        Map[] threads2 = mmap.getFromMonitorMap(monitor2);
        if (threads2[MonitorMap.WAIT_THREAD_POS].containsKey(threadLine1)) {

          // Get the node of the thread that is holding this lock
          DefaultMutableTreeNode thread2Node = (DefaultMutableTreeNode) allBlockingThreadsMap.get(monitor2);
          // Get the node of the monitor itself
          DefaultMutableTreeNode monitor2Node = (DefaultMutableTreeNode) thread2Node.getFirstChild();


          // If a redundant node for thread2 exists with no children, remove it
          // To compare, we have to remove "Thread - " from the front of display
          // strings
          for (int i = 0; i < monitor2Node.getChildCount(); i++) {
            DefaultMutableTreeNode child2 = (DefaultMutableTreeNode) monitor2Node.getChildAt(i);
            if (child2.toString().substring(9).equals(threadLine1) && child2.getChildCount() == 0) {
              monitor2Node.remove(i);
              break;
            }
          }

          // Thread1 is blocked by monitor2 held by thread2, so move thread1
          // under thread2
          try {
            monitor2Node.insert(thread1Node, 0);
          } catch (IllegalArgumentException iae) {
            // This means we have a deadlock... ignore for now.
            // Lets not remove the node, blocked lock chain information gets lost
            break;
          }
          directChildMap.remove(monitor1);
          break;
        }
      }
    }

    allBlockingThreadsMap.clear();

    // Second, renormalize top level based on threads for cases where one thread
    // holds multiple monitors
    boolean changed = false;
    do {
      changed = false;
      for (Iterator iter = directChildMap.entrySet().iterator(); iter.hasNext();) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) ((Map.Entry) iter.next()).getValue();
        if (checkForDuplicateThreadItem(directChildMap, node)) {
          changed = true;
          break;
        }
      }
    } while (changed);

    // Third, renormalize lower levels of the tree based on threads for cases
    // where one thread holds multiple monitors
    for (Iterator iter = directChildMap.entrySet().iterator(); iter.hasNext();) {
      renormalizeThreadDepth((DefaultMutableTreeNode) ((Map.Entry) iter.next()).getValue());
    }
  }

  String getLockingThread(Map[] threads) {
    int lockingThreadCount = threads[MonitorMap.LOCK_THREAD_POS].keySet().size();
    if (lockingThreadCount == 1) {
      return (String) threads[MonitorMap.LOCK_THREAD_POS].keySet().iterator().next();
    }

    for (Iterator iterLocks = threads[MonitorMap.LOCK_THREAD_POS].keySet().iterator(); iterLocks.hasNext();) {
      String thread = (String) iterLocks.next();
      if (!threads[MonitorMap.SLEEP_THREAD_POS].containsKey(thread)) {
        return thread;
      }
    }

    return "";
  }

  boolean checkForDuplicateThreadItem(Map directChildMap, DefaultMutableTreeNode node1) {
    ThreadInfo mi1 = (ThreadInfo) node1.getUserObject();
    String name1 = mi1.getName();

    for (Iterator iter2 = directChildMap.entrySet().iterator(); iter2.hasNext();) {
      DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) ((Map.Entry) iter2.next()).getValue();
      if (node1 == node2) {
        continue;
      }

      ThreadInfo mi2 = (ThreadInfo) node2.getUserObject();
      if (name1.equals(mi2.getName()) && node2.getChildCount() > 0) {
        node1.add((MutableTreeNode) node2.getFirstChild());
        iter2.remove();
        return true;
      }
    }

    return false;
  }

  protected String getNextLine() throws IOException {
    return getBis().readLine();
  }

  /**
   * returns true if at least one more dump available, already loads it (this
   * will be returned on next call of parseNext)
   */
  public boolean hasMoreDumps() {
    nextDump = parseNext();
    return (nextDump != null);
  }

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
    String line = null;
    String tempLine = null;

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
        if (withCurrentTimeStamp) {
          overallTDI.setStartTime((new Date(System.currentTimeMillis())).toString());
        }
        overallTDI.setJvmVersion(this.getJvmVersion());

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
        // ThreadLogic#displayCategory and TreeCategory#getCatComponent both
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

        while (getBis().ready() && !finished) {
          line = getNextLine();
          lineCounter++;
          singleLineCounter++;

          if ((line != null) && (line.trim().length() == 0)) {
            continue;
          }

          if (locked) {

            if (lineChecker.getFullDump(line) != null) {
              locked = false;
              if (!withCurrentTimeStamp) {
                overallTDI.setLogLine(bis.getLineNumber());

                if (startTime != 0) {
                  startTime = 0;
                } else if (matched != null && matched.matches()) {

                  parsedStartTime = matched.group(0);

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
                      nfe.printStackTrace();
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
                } else if (matched == null) {
                  matched = getDm().checkForDateMatch(line);
                  if (matched != null) {
                    parsedStartTime = ((matched.groupCount() == 1) ? matched.group(1) : matched.group(0));
                    overallTDI.setStartTime(parsedStartTime);
                    parsedStartTime = null;
                    matched = null;
                    getDm().resetLastMatch();
                  }
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
            // Problem with JRockit is the Timestamp occurs after the FULL THREAD DUMP tag
            // So the above logic fails as we wont get to parse for the date as its reverse for Hotspot (time occurs before Full Thread Dump marker)
            // So parse the timestamp here for jrockit....
            if ((this instanceof JrockitParser) && (parsedStartTime == null)
                    && !getDm().isPatternError() && (getDm().getRegexPattern() != null)) {
              Matcher m = getDm().checkForDateMatch(line);
              if (m != null) {
                parsedStartTime = m.group(0);
                overallTDI.setStartTime(parsedStartTime);
              }
            }


            if ((tempLine = lineChecker.getStackStart(line)) != null) {

              // SABHA - Commenting off the GC thread portion, we want to know
              // how many GC threads are in the jvm.. so we can provide relevant
              // advisory.
              /*
               * if (lineChecker.getGCThread(line) != null) { // skip GC Threads
               * continue; }
               */

              /**
               * Check for Badly formatted threads starting with '"Workmanager' and ending with ' ms'
               * "Workmanager: , Version: 0, Scheduled=false, Started=false, Wait time: 0 ms
               * " id=1509 idx=0x84 tid=10346 prio=10 alive, sleeping, native_waiting, daemon
               */
              // Check if the thread contains "Workmanager:" and ending with " ms"
              // In that case, rerun the pattern to get correct thread label
              String additionalLines = readAheadForWMThreadLabels(line);
              if (additionalLines.length() > line.length()) {
                tempLine = additionalLines;
              }

              // We are starting a group of lines for a different
              // thread
              // First, flush state for the previous thread (if
              // any)

              concurrentSyncsFlag = false;
              if (content != null) {
                content.append("</font></pre><br>");
              }
              String stringContent = content != null ? content.toString() : null;
              if (title != null) {
                threads.put(title, content.toString());

                addToCategory(catThreads, overallTDI, title, null, stringContent, singleLineCounter, true);
                threadCount++;
              }
              if (inWaiting) {
                addToCategory(catWaiting, overallTDI, title, null, stringContent, singleLineCounter, true);
                inWaiting = false;
                waiting++;
              }
              if (inSleeping) {
                addToCategory(catSleeping, overallTDI, title, null, stringContent, singleLineCounter, true);
                inSleeping = false;
                sleeping++;
              }
              if (inLocking) {
                addToCategory(catLocking, overallTDI, title, null, stringContent, singleLineCounter, true);
                inLocking = false;
                locking++;
              }
              singleLineCounter = 0;
              while (!monitorStack.empty()) {
                mmap.parseAndAddThread((String) monitorStack.pop(), title, content.toString());
              }

              // Second, initialize state for this new thread
              title = tempLine;
              content = new StringBuffer("<pre><font size=" + ThreadLogic.getFontSizeModifier(-1)
                      + ">");
              content.append(tempLine);
              content.append("\n");
            } else if ((tempLine = lineChecker.getThreadState(line)) != null) {
              content.append(tempLine);
              content.append("\n");
              if (title.indexOf("t@") > 0) {
                // in this case the title line is missing state
                // informations
                String state = tempLine.substring(tempLine.indexOf(':') + 1).trim();
                if (state.indexOf(' ') > 0) {
                  title += " nid=none " + state.substring(0, state.indexOf(' '));
                } else {
                  title += " nid=none " + state;
                }
              }
              //} else if (content != null && (tempLine = lineChecker.getLockedOwnable(line)) != null) {
              //  concurrentSyncsFlag = true;
              //  content.append(tempLine);
              //  content.append("\n");
            } else if (content != null && (tempLine = lineChecker.getWaitingOn(line)) != null) {
              content.append(linkifyMonitor(tempLine));
              monitorStack.push(tempLine);
              inSleeping = true;
              content.append("\n");
            } else if (content != null && (tempLine = lineChecker.getParkingToWait(line)) != null) {
              content.append(linkifyMonitor(tempLine));
              monitorStack.push(tempLine);
              inSleeping = true;
              content.append("\n");
            } else if (content != null && (tempLine = lineChecker.getWaitingTo(line)) != null) {
              content.append(linkifyMonitor(tempLine));
              monitorStack.push(tempLine);
              inWaiting = true;
              content.append("\n");
            } else if (content != null && (tempLine = lineChecker.getLocked(line)) != null) {
              content.append(linkifyMonitor(tempLine));
              inLocking = true;
              monitorStack.push(tempLine);
              content.append("\n");
            } else if (content != null && (tempLine = lineChecker.getAt(line)) != null) {
              content.append(tempLine);
              content.append("\n");
            }
            /*
             * } else if (line.indexOf("- ") >= 0) { if (concurrentSyncsFlag) {
             * content.append(linkifyMonitor(line)); monitorStack.push(line); }
             * else { content.append(line); } content.append("\n"); }
             */
            // last thread reached?
            if ((tempLine = lineChecker.getEndOfDump(line)) != null) {
              finished = true;
              //getBis().mark(getMarkSize());
              /*
              if ((checkForDeadlocks(threadDump)) == 0) {
              // no deadlocks found, set back original
              // position.
              getBis().reset();
              }
               *
               */

              //getBis().mark(getMarkSize());

              if (!checkThreadDumpStatData(overallTDI)) {
                // no statistical data found, set back original
                // position.
                getBis().reset();
              }


              if (!(foundClassHistograms = checkForClassHistogram(threadDump))) {
                getBis().reset();
              }

              // Support for parsing Lock Chains in JRockit
              if (!(foundLockChains = checkForLockChains(catThreads))) {
                getBis().reset();
              }

              // Check for ECID & Thread Context Data
              if (!checkThreadDumpContextData(overallTDI)) {
                // If no thread context data found, set back original position.
                try {
                  getBis().reset();
                } catch (IOException ioe) {
                  // Dont let it block further processing
                  ioe.printStackTrace();
                }
              }
            } else {

              // Mark the point as we have successfuly parsed the thread
              getBis().mark(getMarkSize());
            }
          }
        }

        if (content != null) {
          content.append("</font></pre><br>");
        }
        String stringContent = content != null ? content.toString() : null;
        if (title != null) {
          threads.put(title, content.toString());
          addToCategory(catThreads, overallTDI, title, null, stringContent, singleLineCounter, true);
          threadCount++;
        }
        if (inWaiting) {
          addToCategory(catWaiting, overallTDI, title, null, stringContent, singleLineCounter, true);
          inWaiting = false;
          waiting++;
        }
        if (inSleeping) {
          addToCategory(catSleeping, overallTDI, title, null, stringContent, singleLineCounter, true);
          inSleeping = false;
          sleeping++;
        }
        if (inLocking) {
          addToCategory(catLocking, overallTDI, title, null, stringContent, singleLineCounter, true);
          inLocking = false;
          locking++;
        }
        singleLineCounter = 0;
        while (!monitorStack.empty()) {
          mmap.parseAndAddThread((String) monitorStack.pop(), title, content.toString());
        }

        int monitorCount = mmap.size();

        int monitorsWithoutLocksCount = 0;
        int contendedMonitors = 0;
        int blockedThreads = 0;

        Iterator iter = mmap.iterOfKeys();
        while (iter.hasNext()) {
          String monitor = (String) iter.next();
          Map[] threadsInMap = mmap.getFromMonitorMap(monitor);

          // first the locks
          // We need to reset the stack trace for those threads that are holders of locks
          // previously we set the stack trace to be empty as the ownership info was present in a different thread
          // that is blocked and not directly in the owner thread
          Iterator iterLocks = threadsInMap[MonitorMap.LOCK_THREAD_POS].keySet().iterator();
          while (iterLocks.hasNext()) {
            String threadOwner = (String) iterLocks.next();
            theLogger.finest("ThreadOwner :" + threadOwner);
            String stackTrace = (String) threadsInMap[MonitorMap.LOCK_THREAD_POS].get(threadOwner);
            if (stackTrace == null) {
              theLogger.finest("ThreadOwner :" + threadOwner + ", owner stack is null");

              // Search for the owner of the lock
              ThreadInfo ownerThread = overallTDI.getThreadByName(threadOwner);
              if (ownerThread != null) {
                threadsInMap[MonitorMap.LOCK_THREAD_POS].put(threadOwner, ownerThread.getContent());
              }
            }
          }
        }

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

        overallTDI.setJvmType(this.getJvmVendor());

        Category unsortedThreadCategory = (Category) catThreads.getUserObject();
        Category sortedThreads = sortThreadsByHealth(unsortedThreadCategory);
        overallTDI.setThreads(sortedThreads);

        // Create relationship between LockInfo and Threads
        overallTDI.parseLocks(this);

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
      } catch (InterruptedIOException e) {
      } catch (IOException e) {
        e.printStackTrace();
      }
    } while (retry);

    return (null);
  }

  /**
   * Check for Badly formatted threads starting with "Workmanager and ending with ms
   * "Workmanager: , Version: 0, Scheduled=false, Started=false, Wait time: 0 ms
   * " id=1509 idx=0x84 tid=10346 prio=10 alive, sleeping, native_waiting, daemon
   * @param line
   * @return
   * @throws java.io.IOException
   *
   */
  protected String readAheadForWMThreadLabels(String line) throws IOException {
    String currentLine = line.trim();
    if (!(currentLine.contains("\"Workmanager:") && currentLine.endsWith(" ms"))) {
      return line;
    }

    // Read further the next line and add it to the current thread title line
    String newLine = null;
    do {
      lineCounter++;
      newLine = getNextLine();
    } while (newLine.trim().equals(""));

    currentLine = line + newLine;

    return currentLine;
  }

  abstract boolean checkForClassHistogram(DefaultMutableTreeNode threadDump) throws IOException;

  abstract boolean checkForLockChains(DefaultMutableTreeNode threadDump) throws IOException;

  /**
   * Heap PSYoungGen total 6656K, used 3855K [0xb0850000, 0xb0f50000,
   * 0xb4130000) eden space 6144K, 54% used [0xb0850000,0xb0b97740,0xb0e50000)
   * from space 512K, 97% used [0xb0ed0000,0xb0f4c5c0,0xb0f50000) to space 512K,
   * 0% used [0xb0e50000,0xb0e50000,0xb0ed0000) PSOldGen total 15552K, used
   * 13536K [0x94130000, 0x95060000, 0xb0850000) object space 15552K, 87% used
   * [0x94130000,0x94e68168,0x95060000) PSPermGen total 16384K, used 13145K
   * [0x90130000, 0x91130000, 0x94130000) object space 16384K, 80% used
   * [0x90130000,0x90e06610,0x91130000)
   *
   * @param threadDump
   * @return
   * @throws java.io.IOException
   */
  protected boolean checkThreadDumpStatData(ThreadDumpInfo tdi) throws IOException {
    boolean finished = false;
    boolean found = false;
    StringBuffer hContent = new StringBuffer();
    int heapLineCounter = 0;
    int lines = 0;

    while (getBis().ready() && !finished) {
      String line = getNextLine();
      lines++;
      if (!found && !line.equals("")) {
        if (line.trim().startsWith("Heap")) {
          found = true;
        } else if (lines >= getMaxCheckLines()) {
          finished = true;
        }
      } else if (found) {
        if (!line.equals("")) {
          hContent.append(line).append("\n");
        } else {
          finished = true;
          getBis().reset();
        }
        heapLineCounter++;
      }
    }
    if (hContent.length() > 0) {
      tdi.setHeapInfo(new HeapInfo(hContent.toString()));
      theLogger.finest("Found heap info:" + hContent.toString());
    }

    return (found);
  }

  /**
   * id         ECID                                               RID   Context Values
   * ---------------------------------------------------------------------------------
   * id=21  11d1def534ea1be0:6e9ce0e7:13882a8ef89:-8000-0000000000000099 0  WEBSERVICE_PORT.name=execute_pt
   *                                                                        dbRID=0:8
   *                                                                        composite_name=DiagProject
   *                                                                        component_name=TestProject_Mediator1
   *                                                                        J2EE_MODULE.name=fabric
   *                                                                        component_instance_id=5184D160CD4211E18F02CB6
   *                                                                        WEBSERVICE_NAMESPACE.name=http:/TestProject
   *                                                                        activity_name=AQ_Java_Embedding1
   *                                                                        J2EE_APP.name=soa-infra
   *                                                                        WEBSERVICE.name=TestProject
   *                                                                        composite_instance_id=60004
   * id=286 11d1def534ea1be0:6e9ce0e7:13882a8ef89:-8000-0000000000000024 0  dbRID=0:12
   * id=712 11d1def534ea1be0:6e9ce0e7:13882a8ef89:-8000-0000000000000003 0  dbRID=0:7
   *
   * @param threadDump
   * @return
   * @throws java.io.IOException
   */
  protected boolean checkThreadDumpContextData(ThreadDumpInfo tdi) throws IOException {
	    boolean finished = false;
	    boolean reachedExactEndOfDump = false;
	    boolean foundTimingStatistics = false;
	    boolean foundContextData = false;

	    int lines = 0;
	    String threadId = null;
	    StringBuffer contextValBuf = null;


	    // The Thread Context Info line might have got eaten earlier...
	    // so check for next occuring lines like:
	    // ===== THREAD CONTEXT INFORMATION =====
	    //
	    //id         ECID      RID   Context Values
	    Matcher threadContextInfoMatcher = null;

	    //Derek Kam: 12c has an addional elapse time column, the common columns between 12c and 11g are id, ECID and RID,
	    //so I removed the Context Values, it should be enough to identify the context info header.
	    Pattern threadContextInfoPattern = Pattern.compile(".*(" + THREAD_CONTEXT_INFO
	            + "|id\\s*ECID\\s*RID).*");

	    while (getBis().ready() && !finished) {
	      String line = getNextLine();
	      if (line == null) {
	        return false;
	      }

	      line = line.trim();

	      // We have reached start of a new thread dump, so stop
	      if (this.lineChecker.getFullDump(line) != null) {
	        theLogger.finest("Breaking now as we hit Full Dump for Current Line: " + line);
	        return false;
	      }

	      // The Thread Context Data section occurs after thread dump
	      // In JRockit, the lock chain info would have been parsed already which ends with ExactEndOfDump marker (END OF THREAD DUMP)
	      // So no need to check if we have reached End of Dump
	      /*
	      if (!reachedExactEndOfDump) {
		      if ( this.lineChecker.getExactEndOfDump(line) == null) {
		      	continue;
		      } else {
		      	reachedExactEndOfDump = true;
		      	getBis().mark(getMarkSize());
		      }
	      }
	       */

	      // Start count of lines that came after actual End of the thread Dump
	      lines++;
	      if (!foundTimingStatistics) {
	        // Sometimes the Timing Statistics Section might not be present ahead of the Thread Context Section
	        // So check for both
	        threadContextInfoMatcher = threadContextInfoPattern.matcher(line);

	        if (line.indexOf(THREAD_TIMING_STATISTICS) > 0) {
	          foundTimingStatistics = true;
	        } else if (threadContextInfoMatcher.matches()) {
	          foundContextData = foundTimingStatistics = true;
	        } else if (lines >= getMaxCheckLines()) {
	          finished = true;
	        }
	      } else {
	        // Found Timing Statistics section that occurs before Thread Context Info section
	        if (line.trim().equals("") || line.startsWith("----")) {
	          continue;
	        }

	        // There will be one statistic line per thread
	        // Keep going till we reach Thread Context Info banner
	        // then start parsing ECID and other Context data per thread
	        if (!foundContextData) {
	          // Found begining marker of Thread Context Info
	          foundContextData = (line.indexOf(THREAD_CONTEXT_INFO) > 0);

	          //skip just found Thread Context Info banner and proceed to actual data...
	          if (foundContextData) {
	            continue;
	          }

	        } else if (line.indexOf(END_THREAD_CONTEXT_INFO) > 0 || line.indexOf(THREAD_CONTEXT_INFO) > 0) {
	          // We have reached Thread Context Info marker again
	          // Treat that as end of processing and return
	          finished = true;
	        }

	        // We are in Thread Context Info Section
	        // start processing line entries
	        if (foundContextData && !finished) {

	          // Parse the line entry

	          // If its start of a new thread context info
	          if (line.startsWith("id=")) {

	            // Save previous thread context
	            if (threadId != null) {

	              tdi.addThreadContextData(threadId, contextValBuf.toString());
	              threadId = null;
	            }


	            // Parse new thread context info
	            String[] entries = line.trim().split("\\s+");
	            contextValBuf = new StringBuffer();

	            //Derek Kam:  12c has five column, Elapsed/ms is new in 12c
				if (entries.length==5) {
					switch (entries.length) {
					case (5):
						// The 5th column contains the context data
						contextValBuf.append(entries[4] + ThreadInfo.CONTEXT_DATA_SEPARATOR);
					case (4):
						// The 4th column contains the elapsed time
						contextValBuf.append("Elapsed/ms=" + entries[3] + ThreadInfo.CONTEXT_DATA_SEPARATOR);
					case (3):
						// The 3rd column contains the RID
						contextValBuf.append("RID=" + entries[2] + ThreadInfo.CONTEXT_DATA_SEPARATOR);
					case (2):
						// The 2nd column contains the ECID
						contextValBuf.append("ECID=" + entries[1] + ThreadInfo.CONTEXT_DATA_SEPARATOR);
					default:
						// The 1st column contains the Thread Id
						threadId = entries[0].substring(3);
					}
				} else {
					switch (entries.length) {
					case (4):
						// The 4th column contains the context data
						contextValBuf.append(entries[3] + ThreadInfo.CONTEXT_DATA_SEPARATOR);
					case (3):
						// The 3rd column contains the RID
						contextValBuf.append("RID=" + entries[2] + ThreadInfo.CONTEXT_DATA_SEPARATOR);
					case (2):
						// The 2nd column contains the ECID
						contextValBuf.append("ECID=" + entries[1] + ThreadInfo.CONTEXT_DATA_SEPARATOR);
					default:
						// The 1st column contains the Thread Id
						threadId = entries[0].substring(3);
					}
				}

	          } else {
	            // This line entry only contains context data and is part of current thread context
	            if (contextValBuf == null) {
	              contextValBuf = new StringBuffer();
	            }

	            contextValBuf.append(line + ThreadInfo.CONTEXT_DATA_SEPARATOR);
	          }
	        }
	      }
	    }

	    // Save the last entry
	    if (threadId != null && contextValBuf != null) {
	      tdi.addThreadContextData(threadId, contextValBuf.toString());
	    }

	    // We have hit the end marker for the Thread Context Info
	    // finish parsing
	    finished = true;
	    return foundContextData;
	  }


  /**
   * check if any dead lock information is logged in the stream
   *
   * @param threadDump
   *          which tree node to add the histogram.
   */
  int checkForDeadlocks(DefaultMutableTreeNode threadDump) throws IOException {
    boolean finished = false;
    boolean found = false;
    int deadlocks = 0;
    int lineCounter = 0;
    StringBuffer dContent = new StringBuffer();
    TreeCategory deadlockCat = new TreeCategory("Deadlocks", IconFactory.DEADLOCKS);
    DefaultMutableTreeNode catDeadlocks = new DefaultMutableTreeNode(deadlockCat);
    boolean first = true;

    while (getBis().ready() && !finished) {
      String line = getNextLine();

      if (!found && !line.equals("")) {
        if (line.trim().startsWith("Found one Java-level deadlock")) {
          found = true;
          dContent.append("<font size=").append(ThreadLogic.getFontSizeModifier(-1)).append("><b>");
          dContent.append("Found one Java-level deadlock");
          dContent.append("</b><hr></font><pre>\n");
        } else if (lineCounter >= getMaxCheckLines()) {
          finished = true;
        } else {
          lineCounter++;
        }
      } else if (found) {
        if (line.startsWith("Found one Java-level deadlock")) {
          if (dContent.length() > 0) {
            deadlocks++;
            addToCategory(catDeadlocks, "Deadlock No. " + (deadlocks), null, dContent.toString(), 0, false);
          }
          dContent = new StringBuffer();
          dContent.append("</pre><b><font size=").append(ThreadLogic.getFontSizeModifier(-1)).append(">");
          dContent.append("Found one Java-level deadlock");
          dContent.append("</b><hr></font><pre>\n");
          first = true;
        } else if ((line.indexOf("Found") >= 0) && (line.endsWith("deadlocks.") || line.endsWith("deadlock."))) {
          finished = true;
        } else if (line.startsWith("=======")) {
          // ignore this line
        } else if (line.indexOf(" monitor 0x") >= 0) {
          dContent.append(linkifyDeadlockInfo(line));
          dContent.append("\n");
        } else if (line.indexOf("Java stack information for the threads listed above") >= 0) {
          dContent.append("</pre><br><font size=").append(ThreadLogic.getFontSizeModifier(-1)).append("><b>");
          dContent.append("Java stack information for the threads listed above");
          dContent.append("</b><hr></font><pre>");
          first = true;
        } else if ((line.indexOf("- waiting on") >= 0) || (line.indexOf("- waiting to") >= 0)
                || (line.indexOf("- locked") >= 0) || (line.indexOf("- parking to wait") >= 0)) {

          dContent.append(linkifyMonitor(line));
          dContent.append("\n");

        } else if (line.trim().startsWith("\"")) {
          dContent.append("</pre>");
          if (first) {
            first = false;
          } else {
            dContent.append("<br>");
          }
          dContent.append("<b><font size=").append(ThreadLogic.getFontSizeModifier(-1)).append("><code>");
          dContent.append(line);
          dContent.append("</font></code></b><pre>");
        } else {
          dContent.append(line);
          dContent.append("\n");
        }
      }
    }
    if (dContent.length() > 0) {
      deadlocks++;
      addToCategory(catDeadlocks, "Deadlock No. " + (deadlocks), null, dContent.toString(), 0, false);
    }

    if (deadlocks > 0) {
      threadDump.add(catDeadlocks);
      ((ThreadDumpInfo) threadDump.getUserObject()).setDeadlocks((TreeCategory) catDeadlocks.getUserObject());
      deadlockCat.setName("Deadlocks (" + deadlocks + (deadlocks == 1 ? " deadlock)" : " deadlocks)"));
    }

    return (deadlocks);
  }

  /**
   * @returns true, if a class histogram was found and added during parsing.
   */
  public boolean isFoundClassHistograms() {
    return (foundClassHistograms);
  }

  abstract String linkifyDeadlockInfo(String line);

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
   * @return the jvmVendor
   */
  public String getJvmVendor() {
    return jvmVendor;
  }

  /**
   * @param jvmVendor the jvmVendor to set
   */
  public void setJvmVendor(String jvmVendor) {
    this.jvmVendor = jvmVendor;
  }

  public class LineChecker implements DumpParser.lineChecker {

    Pattern fullDumpPattern;
    Pattern stackStartPattern = createPattern("\\s*(\".*)");
    Pattern atPattern;
    Pattern threadStatePattern;
    Pattern lockedOwnablePattern;
    Pattern waitingOnPattern;
    Pattern parkingToWaitPattern;
    Pattern waitingToPattern;
    Pattern lockedPattern;
    Pattern endOfDumpPattern;
    Pattern exactEndOfDumpPattern;
    Pattern lockReleasedPattern;
    Pattern gcThreadPattern = createPattern(".*(\".*G[Cc].*hread.*\").*");
    Pattern endOfTitlePattern;

    @Override
    public String getFullDump(String line) {
      if (fullDumpPattern != null) {
        Matcher matcher = fullDumpPattern.matcher(line);
        if (matcher.matches()) {
          return matcher.group(1);
        }
      }
      return null;
    }

    public String getStackStart(String line) {
      if (stackStartPattern != null) {
        Matcher matcher = stackStartPattern.matcher(line);
        if (matcher.matches()) {
          return matcher.group(1);
        }
      }
      return null;
    }

    public String getGCThread(String line) {
      if (gcThreadPattern != null) {
        Matcher matcher = gcThreadPattern.matcher(line);
        if (matcher.matches()) {
          return format(matcher.group(1));
        }
      }
      return null;
    }

    @Override
    public String getAt(String line) {
      if (atPattern != null) {
        Matcher matcher = atPattern.matcher(line);
        if (matcher.matches()) {
          //return format(matcher.group(1));
          return format(matcher.groupCount() == 1 ? matcher.group(1) : matcher.group(0));
        }
      }
      return null;
    }

    @Override
    public String getThreadState(String line) {
      if (threadStatePattern != null) {
        Matcher matcher = threadStatePattern.matcher(line);
        if (matcher.matches()) {
          return matcher.group(1);
        }
      }
      return null;
    }

    @Override
    public String getLockedOwnable(String line) {
      if (lockedOwnablePattern != null) {
        Matcher matcher = lockedOwnablePattern.matcher(line);
        if (matcher.matches()) {
          return format(matcher.group(1));
        }
      }
      return null;
    }

    @Override
    public String getWaitingOn(String line) {
      if (waitingOnPattern != null) {
        Matcher matcher = waitingOnPattern.matcher(line);
        if (matcher.matches()) {
          return format(matcher.group(1));
        }
      }
      return null;
    }

    @Override
    public String getParkingToWait(String line) {
      if (parkingToWaitPattern != null) {
        Matcher matcher = parkingToWaitPattern.matcher(line);
        if (matcher.matches()) {
          return format(matcher.group(1));
        }
      }
      return null;
    }

    @Override
    public String getWaitingTo(String line) {
      if (waitingToPattern != null) {
        Matcher matcher = waitingToPattern.matcher(line);
        if (matcher.matches()) {
          return format(matcher.group(1));
        }
      }
      return null;
    }

    @Override
    public String getLocked(String line) {
      if (lockedPattern != null) {
        Matcher matcher = lockedPattern.matcher(line);
        if (matcher.matches()) {
          return format(matcher.group(1));
        }
      }
      return null;
    }

    @Override
    public String getEndOfDump(String line) {
      if (endOfDumpPattern != null) {
        Matcher matcher = endOfDumpPattern.matcher(line);
        if (matcher.matches()) {
          return matcher.group(1);
        }
      }
      return null;
    }

    @Override
    public String getExactEndOfDump(String line) {
      if (endOfDumpPattern != null) {
        Matcher matcher = exactEndOfDumpPattern.matcher(line);
        if (matcher.matches()) {
          return matcher.group(1);
        }
      }
      return null;
    }

    @Override
    public String getLockReleased(String line) {
      if (lockReleasedPattern != null) {
        Matcher matcher = lockReleasedPattern.matcher(line);
        if (matcher.matches()) {
          return format(matcher.group(1));
        }
      }
      return null;
    }

    @Override
    public void setFullDumpPattern(String pattern) {
      fullDumpPattern = createPattern(pattern);
    }

    public void setStackStartPattern(String pattern) {
      stackStartPattern = createPattern(pattern);
    }

    @Override
    public void setAtPattern(String pattern) {
      atPattern = createPattern(pattern);
    }

    @Override
    public void setThreadStatePattern(String pattern) {
      threadStatePattern = createPattern(pattern);
    }

    @Override
    public void setLockedOwnablePattern(String pattern) {
      lockedOwnablePattern = createPattern(pattern);
    }

    @Override
    public void setWaitingOnPattern(String pattern) {
      waitingOnPattern = createPattern(pattern);
    }

    @Override
    public void setParkingToWaitPattern(String pattern) {
      parkingToWaitPattern = createPattern(pattern);
    }

    @Override
    public void setWaitingToPattern(String pattern) {
      waitingToPattern = createPattern(pattern);
    }

    @Override
    public void setLockedPattern(String pattern) {
      lockedPattern = createPattern(pattern);
    }

    @Override
    public void setEndOfDumpPattern(String pattern) {
      endOfDumpPattern = createPattern(pattern);
    }

    @Override
    public void setExactEndOfDumpPattern(String pattern) {
      exactEndOfDumpPattern = createPattern(pattern);
    }

    @Override
    public void setLockReleasedPattern(String pattern) {
      lockReleasedPattern = createPattern(pattern);
    }

    @Override
    public void setGCThreadPattern(String pattern) {
      gcThreadPattern = createPattern(pattern);
    }

    private Pattern createPattern(String pattern) {
      if (pattern != null || pattern.length() != 0) {
        return Pattern.compile(pattern);
      }
      return null;
    }

    @Override
    public String getEndOfTitlePattern(String line) {
      if (endOfTitlePattern != null) {
        Matcher matcher = endOfTitlePattern.matcher(line);
        if (matcher.matches()) {
          return format(matcher.group(0));
        }
      }
      return null;
    }

    @Override
    public void setEndOfTitlePattern(String pattern) {
      this.endOfTitlePattern = createPattern(pattern);
    }

    private String format(String s) {
      return s.startsWith(" ") ? "\t" + s.trim() : s;
    }
  }

  public void createLockInfo(ThreadInfo thread) {

    String blockedLockId = null;
    String stack = thread.getContent();
    if (stack == null) {
      return;
    }

    ArrayList<String> ownedLockIds = getTrailingEntryAfterPattern(stack, LOCKED);

    ArrayList<String> blockedLockIds = getTrailingEntryAfterPattern(stack, BLOCKED_FOR_LOCK);
    if (blockedLockIds.size() > 0) {
      blockedLockId = blockedLockIds.get(0);

      // Make sure its not blocked on its own lock, saw a edge case of thread waiting to lock something it owned
      /*
       * "ExecuteThread: '2' for queue: 'weblogic.socket.Muxer'"  ... waiting for monitor entry [0xfffffffe37f3f000]
       * java.lang.Thread.State: BLOCKED (on object monitor)
       * at weblogic.socket.DevPollSocketMuxer.processSockets(DevPollSocketMuxer.java:94)
       * - locked <0xfffffffe800550d0> (a java.lang.String)
       * at weblogic.socket.SocketReaderRequest.run(SocketReaderRequest.java:29)
       * at weblogic.socket.SocketReaderRequest.execute(SocketReaderRequest.java:42)
       * at weblogic.kernel.ExecuteThread.execute(ExecuteThread.java:145)
       * at weblogic.kernel.ExecuteThread.run(ExecuteThread.java:117)
       */
      // Verify we are not marking a lock owned by the same thread as the blocking lock
      if (!ownedLockIds.contains(blockedLockId)) {
        thread.setBlockedForLock(blockedLockId);
        theLogger.finest("Thread: " + thread.getFilteredName() +
         ", blocked for lock: " + blockedLockId);
      }
    }

    // In Sun Hotspot, if the thread enters a synchronized block owning the lock
    // but then goes into a lock.wait() or lock.wait(xxx) timed wait,
    // it will still appear as holding the lock
    // Here Thread-0 got the lock first but released it going into a wait state,
    // the lock was then obtained by Thread-1

    /*
     * "Thread-1" prio=6 tid=0x186f4c00 nid=0x1318 waiting on condition
     * [0x189ff000] java.lang.Thread.State: TIMED_WAITING (sleeping)
     * at java.lang.Thread.sleep(Native Method)
     * at MyTest.doSomething(MyTest.java:24)
     * at MyTest.run(MyTest.java:18)
     * - locked <0x04292a30> (a java.lang.Object)
     * at java.lang.Thread.run(Thread.java:619)
     *
     * "Thread-0" prio=6 tid=0x186f0400 nid=0x2b28 in Object.wait() [0x1896f000]
     * java.lang.Thread.State: WAITING (on object monitor)
     * java.lang.Object.wait(Native Method)
     * - waiting on <0x04292a30> (a java.lang.Object)
     * at java.lang.Object.wait(Object.java:485)
     * at MyTest.run(MyTest.java:15)
     * - locked <0x04292a30> (a java.lang.Object)
     * at java.lang.Thread.run(Thread.java:619)
     */

    // So, check if the lock was released - as in its waiting on the same lock
    ArrayList<String> waitingOnLockIds = getTrailingEntryAfterPattern(stack, GENERAL_WAITING);

    // There can be only 1 lock that can be waited upon at a time by a given
    // thread..
    if (waitingOnLockIds.size() > 0) {
      String waitingOnLockId = waitingOnLockIds.get(0);

      // remove this lock Id from the owned list... as the lock is now occupied
      // by someone else...
      ownedLockIds.remove(waitingOnLockId);
    }

    // In case of JRockit, the lock will appear as released.., here Thread-0 got
    // the lock first but released it which was then obtained by Thread-1
    /*
     * "Thread-0" id=12 idx=0x50 tid=9704 prio=5 alive, waiting, native_blocked
     * -- Waiting for notification on: java/lang/Object@0x101F0B40[fat lock]
     * at jrockit/vm/Threads.waitForNotifySignal(JLjava/lang/Object;)Z(Native Method)
     * at java/lang/Object.wait(J)V(Native Method)
     * at java/lang/Object.wait(Object.java:485) at MyTest.run(MyTest.java:15)
     * ^-- Lock released while waiting: java/lang/Object@0x101F0B40[fat lock]
     * at java/lang/Thread.run(Thread.java:619)
     * at jrockit/vm/RNI.c2java(IIIII)V(Native Method)
     * -- end of trace
     *
     * "Thread-1" id=13 idx=0x54 tid=7608 prio=5 alive, sleeping, native_waiting
     * at java/lang/Thread.sleep(J)V(Native Method)
     * at MyTest.doSomething(MyTest.java:24)
     * at MyTest.run(MyTest.java:18)
     * ^-- Holding lock: java/lang/Object@0x101F0B40[fat lock]
     * at java/lang/Thread.run(Thread.java:619)
     * at jrockit/vm/RNI.c2java(IIIII)V(Native Method)
     * -- end of trace
     */
    // But we dont have to discount it from the owned locks as JRockit logs it
    // as released already...

    thread.addOwnedLocks(ownedLockIds);
    theLogger.finest("Thread: " + thread.getFilteredName() +
     ", owning locks: " + ownedLockIds);
    theLogger.finest("Thread: " + thread.getFilteredName() +
     ", waiting on locks: " + waitingOnLockIds);
  }

  public static ArrayList<String> getTrailingEntryAfterPattern(String data, String pattern) {

    Pattern vmPattern = Pattern.compile("(" + pattern + ")");
    Matcher m = vmPattern.matcher(data);

    ArrayList<String> trailingEntries = new ArrayList<String>();

    while (m.find()) {

      int offset = m.end();
      int maxLength = data.length();
      int endOffset = (offset + 400);
      if (endOffset > maxLength) {
        endOffset = maxLength;
      }

      String entry = data.substring(offset, endOffset);
      entry = entry.trim().replaceAll("\\n.*", "").trim();

      // Trim off the trailing "owned by " Thread info appened by VisualVM
      /* Sample created by VisualVM
      "ExecuteThread: '3' for queue: 'weblogic.socket.Muxer'" - Thread t@243
      java.lang.Thread.State: BLOCKED
      at weblogic.socket.DevPollSocketMuxer.processSockets(DevPollSocketMuxer.java:92)
      - waiting to lock <4de8b404> (a java.lang.String) owned by "ExecuteThread: '2' for queue: 'weblogic.socket.Muxer'" t@242
       *
       */
      entry = entry.replaceAll(" owned by .*", "").trim();


      // JRockit has an extra entry at end showing type of lock - locked or
      // unlocked or fat lock...
      // strip that entry...
      entry = entry.replaceAll("\\[.*\\]", "").trim();
      trailingEntries.add(entry);

      if (endOffset == maxLength) {
        break;
      }
    }

    return trailingEntries;
  }
}
