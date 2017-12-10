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
 * ThreadsTableModel.java
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
 * $Id: ThreadsTableModel.java,v 1.6 2008-04-27 20:31:14 irockel Exp $
 */
package com.oracle.ateam.threadlogic.utils;


import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.ThreadDumpInfo;
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import java.awt.Color;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.ThreadState;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * table model for displaying difference between threads across thread dumps.
 * 
 * @author Sabha
 */
public class ThreadDiffsTableModel extends ThreadsTableModel {

  private static Logger theLogger = CustomLogger.getLogger(ThreadDiffsTableModel.class.getSimpleName());

  public enum STATE_CHANGE {

    EMPTY, NO_CHANGE, PROGRESS, NO_CHANGE_WARNING;

    private static DefaultTableCellRenderer whiteRenderer, greenRenderer;
    private static DefaultTableCellRenderer yellowRenderer, siennaRenderer;

    static {
      init();
    }
    
    private static void init() {
    
      whiteRenderer = new DefaultTableCellRenderer();
      whiteRenderer.setBackground(Color.WHITE);
      
      greenRenderer = new DefaultTableCellRenderer();
      greenRenderer.setBackground(new Color(148, 247, 49));
      
      yellowRenderer = new DefaultTableCellRenderer();
      yellowRenderer.setBackground(Color.YELLOW);
      
      siennaRenderer = new DefaultTableCellRenderer();
      siennaRenderer.setBackground(new Color(247, 181, 49));
    }
    
    public String toString() {
      switch (this) {
      case PROGRESS:
        return "Progress";
      case NO_CHANGE:
      case NO_CHANGE_WARNING:
        return "No Change";
      default:
        return "Empty";
      }
    }

    public Color getColor() {
      switch (this) {
      case NO_CHANGE_WARNING:
        // Sienna
        return new Color(247, 181, 49);
      case PROGRESS:
        // pale green
        return new Color(148, 247, 49);
      case NO_CHANGE:  
        return Color.YELLOW;
      default:
        return Color.WHITE;
      }
    }
    
    public DefaultTableCellRenderer getRenderer() {      
      
      switch (this) {
        case NO_CHANGE_WARNING:
          // Sienna
          return siennaRenderer;
        case PROGRESS:
          // pale green
          return greenRenderer;
        case NO_CHANGE:  
          return yellowRenderer;
      }      
      return whiteRenderer;
    }
  };

  ArrayList<ThreadDumpInfo> threadDumpArrList;
  Hashtable<String, ArrayList<STATE_CHANGE>> progressMatrix = new Hashtable<String, ArrayList<STATE_CHANGE>>();

  /**
   * 
   * @param root
   */
  public ThreadDiffsTableModel(DefaultMutableTreeNode rootNode, ArrayList<ThreadDumpInfo> threadDumpArrList) {
    super(rootNode);
    this.threadDumpArrList = threadDumpArrList;
    int noOfTDs = threadDumpArrList.size();    
    
    columnNames = new String[5 + noOfTDs];
    columnNames[0] = "Name";
    columnNames[1] = "Health Progress Bar";
    columnNames[2] = "Thread Group";
    columnNames[3] = "Last Known Health";

    columnNames[4] = "State";
    columnNames[5] = "Advisories";
    for (int i = 0; i < noOfTDs - 1; i++) {
      columnNames[i + 6] = threadDumpArrList.get(i).getName().replace("Dump ", "") + " Vs. "
          + threadDumpArrList.get(i + 1).getName().replace("Dump ", "");

      // Search for the time portion as in HH:MM:SS -> same format for time compared to day/dates across all JVM Thread Dumps
      // Sun Hotspot: 2012-02-17 10:35:02
      // JRockit: Thu May 26 10:50:43 2011
      // IBM: 2011/01/12 at 17:14:40

      String startTime =  threadDumpArrList.get(i).getStartTime();
      if (startTime == null) {
        startTime = "N/A";
      } else {
        String[] tokens = startTime.split(" ");
        for (String token: tokens)
          if ( (token.length() == 8) && token.contains(":")) {
            startTime = token;
            break;
          }
      }
      
      String endTime =  threadDumpArrList.get(i + 1).getStartTime();
      if (endTime == null) {
        endTime = "N/A";
      } else {
        String[] tokens = endTime.split(" ");
        for (String token: tokens)
          if ( (token.length() == 8) && token.contains(":")) {
            endTime = token;
            break;
          }
      }
      columnNames[i + 6] = columnNames[i + 6] + " [" + startTime + " to " + endTime + "]";
    }
    createProgressMatrixBetweenTDs();
  }

  /**
   * @inherited
   */
  public Class getColumnClass(int columnIndex) {
    return String.class;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    ThreadData tiData = ((ThreadData) elements.elementAt(rowIndex));
    String nameId = tiData.getNameId();
    String filteredName = tiData.getName();
    int noOfThreadDumps = threadDumpArrList.size();
    ThreadInfo actualThreadFromLastTDI = threadDumpArrList.get(noOfThreadDumps - 1).getThreadMap().get(nameId);

    switch (columnIndex) {
    case 0:
      return filteredName.replaceAll("\\[.*\\] ", "").replaceAll("\"", "");
    case 1:
      return returnHealthProgressionColumn(nameId);
    case 2:      
      return actualThreadFromLastTDI.getThreadGroup().getThreadGroupName();
    case 3:
      return determineHealth(nameId);
    case 4:
      return tiData.getState();
    case 5:
      return returnAdvisoryColumn(nameId);

    default:
      return checkForDiffBetweenTDs(nameId, columnIndex - 6);
    }

  }

  
  public String returnHealthProgressionColumn(String nameId) {
    // Only column left is the Advisory section..
    // Get the advisories for the thread from the last thread dump
    int noOfThreadDumps = threadDumpArrList.size();
    
    StringBuffer sbuf = new StringBuffer("<html><body>");
    for(int i = 0; i < noOfThreadDumps; i++) {
      ThreadInfo ti = threadDumpArrList.get(i).getThreadMap().get(nameId);
      HealthLevel health = ti.getHealth();      
      sbuf.append("<font bgcolor=\"" + health.getBackgroundRGBCode() + "\" ><b>&nbsp;&nbsp;|</b></font>");      
    }    
    sbuf.append("</body></html>");
    return sbuf.toString();

  }
  
  public String returnAdvisoryColumn(String nameId) {
    // Only column left is the Advisory section..
    // Get the advisories for the thread from the last thread dump
    int noOfThreadDumps = threadDumpArrList.size();
    ThreadInfo actualThreadFromLastTDI = threadDumpArrList.get(noOfThreadDumps - 1).getThreadMap().get(nameId);

    boolean firstEntry = true;
    
    StringBuffer sbuf = new StringBuffer();
    for (ThreadAdvisory tdadv : actualThreadFromLastTDI.getAdvisories()) {
      if (!firstEntry)
        sbuf.append(", ");
      sbuf.append(tdadv.getPattern());
      firstEntry = false;
    }    
    return sbuf.toString();

  }

  /**
   * search for the specified (partial) name in thread names
   * 
   * @param startRow
   *          row to start the search
   * @param name
   *          the (partial) name
   * @return the index of the row or -1 if not found.
   *
  public int searchRowWithName(int startRow, String name) {
    int i = (startRow >= 0 )? startRow : 0;
    boolean found = false;
    while (!found && (i < getRowCount())) {
      ThreadInfo ti = ((ThreadData) getInfoObjectAtRow(i++)).getAssocThreadInfo();
      if (ti == null)
        continue;
      found = ti.getName().indexOf(name) >= 0;
    }

    return (found ? i - 1 : -1);  
  }
  
  public int searchRowWithContent(int startRow, String searchContent) {
    int i = (startRow >= 0 )? startRow : 0;
    boolean found = false;
    while (!found && (i < getRowCount())) {
      ThreadInfo ti = ((ThreadData) getInfoObjectAtRow(i++)).getAssocThreadInfo();      
      if (ti == null)
        continue;
      found = ti.getContent().indexOf(searchContent) >= 0;      
      theLogger.finest("Found matching Thread Name: " + ti.getFilteredName());
    }
    
    return (found ? i - 1 : -1);
  }
   */

  private STATE_CHANGE checkForDiffBetweenTDs(String nameId, int columnIndex) {

    // Thread name is "ExecuteThread: '1' for queue: 'weblogic.socket.Muxer'"
    // id=29 idx=0xc0 tid=19931 prio=5 alive, blocked, native_blocked, daemon
    // Filtered Thread name is just
    // "ExecuteThread: '1' for queue: 'weblogic.socket.Muxer'" without rest of
    // the labels

    ArrayList<STATE_CHANGE> progressIndicatorList = progressMatrix.get(nameId);

    /*
     * 
     * ThreadInfo ti1 = threadDumpArrList.get(columnIndex -
     * 1).getThread(filteredThreadName); ThreadInfo ti2 =
     * threadDumpArrList.get(columnIndex).getThread(filteredThreadName);
     * 
     * threadContent = ti1.getContent(); compareAgainst = ti2.getContent();
     * 
     * if (threadContent == null || compareAgainst == null) return EMPTY;
     * 
     * // The thread priority can change or state can change, creating an
     * ignorable difference // strip off the thread name from the rest of the
     * content... threadContent = threadContent.replace(ti1.getName(),
     * "").trim(); compareAgainst = compareAgainst.replace(ti2.getName(),
     * "").trim();
     * 
     * if (!threadContent.equals(compareAgainst)) return PROGRESS;
     */

    return progressIndicatorList.get(columnIndex);
  }

  private void createProgressMatrixBetweenTDs() {

    Hashtable<String, ThreadInfo> threadMap0 = threadDumpArrList.get(0).getThreadMap();

    for (String threadId : threadMap0.keySet()) {

      ArrayList<STATE_CHANGE> progressIndicatorList = new ArrayList<STATE_CHANGE>();
      for (int i = 1; i < threadDumpArrList.size(); i++) {

        ThreadDumpInfo predTDI = threadDumpArrList.get(i - 1);
        ThreadDumpInfo sucTDI = threadDumpArrList.get(i);

        ThreadInfo oldFrame = predTDI.getThread(threadId);
        ThreadInfo newFrame = sucTDI.getThread(threadId);

        if (oldFrame == null || newFrame == null) {
          progressIndicatorList.add(STATE_CHANGE.EMPTY);
          continue;
        }

        String threadContent = oldFrame.getContent();
        String compareAgainst = newFrame.getContent();

        if (threadContent == null || compareAgainst == null) {
          progressIndicatorList.add(STATE_CHANGE.EMPTY);
          continue;
        }

        // The thread priority can change or state can change, creating an
        // ignorable difference
        // strip off the thread name from the rest of the content...
        // Also, strip the whitespaces/newlines just to be safer before comparing
        threadContent = threadContent.replace(oldFrame.getName(), "").replaceAll("\\s", "").trim();
        compareAgainst = compareAgainst.replace(newFrame.getName(), "").replaceAll("\\s", "").trim();
        
        if (!threadContent.equals(compareAgainst))
          progressIndicatorList.add(STATE_CHANGE.PROGRESS);
        else {
          // Highlight in a different color if thread is already in watch or higher level
          // compared to threads in normal/ignore levels
          if (newFrame.getHealth().ordinal() >= HealthLevel.WATCH.ordinal())
            progressIndicatorList.add(STATE_CHANGE.NO_CHANGE_WARNING);
          else
            progressIndicatorList.add(STATE_CHANGE.NO_CHANGE);
        }
      }

      progressMatrix.put(threadId, progressIndicatorList);

    }
  }

  public HealthLevel determineHealth(String nameId) {
    
    int noOfThreadDumps = threadDumpArrList.size();

    // Use the health of the last or latest thread dump for reporting
    ThreadInfo actualThreadFromLastTDI = threadDumpArrList.get(noOfThreadDumps - 1).getThreadMap().get(nameId);
    HealthLevel health = actualThreadFromLastTDI.getHealth();
    String threadGrpName = actualThreadFromLastTDI.getThreadGroup().getThreadGroupName();

    ArrayList<STATE_CHANGE> progressIndicatorList = progressMatrix.get(nameId);

    // Check if the thread health is already in a watch or higher level
    // If the thread state has not progressed in the thread dumps for the last
    // comparison,
    // then upgrade the health to one level up
    if ((health.ordinal() >= HealthLevel.WATCH.ordinal()) && !nameId.toLowerCase().contains("muxer")
            && !threadGrpName.toLowerCase().contains("jvm")
            && !actualThreadFromLastTDI.getContent().contains("Thread.sleep") //Ignore sleeping threads...
        && progressIndicatorList.get(progressIndicatorList.size() - 1) != STATE_CHANGE.PROGRESS) {

      switch (health) {
      case FATAL:
      case WARNING:
        health = HealthLevel.FATAL;
        break;
      case WATCH:
        health = HealthLevel.WARNING;
        break;
      }
    } else if ((actualThreadFromLastTDI.getState() == ThreadState.RUNNING) && !nameId.toLowerCase().contains("muxer")
            && !threadGrpName.toLowerCase().contains("jvm")           
        && progressIndicatorList.get(progressIndicatorList.size() - 1) != STATE_CHANGE.PROGRESS) {

      // Even if the thread appears in Running state, but shows no progress, mark it under WATCH
      return HealthLevel.WATCH;
    }  

    return health;
  }
}
