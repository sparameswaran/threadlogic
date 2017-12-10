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
import com.oracle.ateam.threadlogic.ThreadInfo;

import com.oracle.ateam.threadlogic.ThreadState;
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup;

import java.math.BigInteger;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * table model for displaying thread overview.
 * 
 * @author irockel
 */
public class ThreadsTableModel extends AbstractTableModel {
  
  private static Logger theLogger = CustomLogger.getLogger(ThreadsTableModel.class.getSimpleName());
  
  public static class ThreadData {

    private String scrubbedName, nameId;
    private String threadGroupName;  
    private HealthLevel health;  
    private String advisoryNames;        
    private ThreadState state;
    private BigInteger tid;
    private BigInteger nid;
    private String ecid;
    private String compositeFlowID;
    private ThreadInfo assocThreadInfo;

    public ThreadData(ThreadInfo ti) {
        scrubbedName = ti.getFilteredName();
        nameId = ti.getNameId();
        assocThreadInfo = ti;
        
        ThreadGroup tg = ti.getThreadGroup();
        if (tg != null) {
          threadGroupName = tg.getThreadGroupName();  
        }
        
        health = ti.getHealth();                  
        state = ti.getState();
        ecid = ti.getEcid();
        compositeFlowID = ti.getCompositeFlowID();
        
        advisoryNames = getAdvisoryNames(ti);
        String[] columns = ti.getTokens();
        try {
        tid = parseNumbers(columns[1]);
        nid = parseNumbers(columns[2]);
        } catch(Exception e) {
          theLogger.warning("Error in parsing Thread tid/nid: " + ti.getFilteredName());
          e.printStackTrace();
        }
    }
    
    private BigInteger parseNumbers(String val) {
      if (val == null || val.equals("?") || val.equals("none")) {
        return null;          
      }
      
      if (val.startsWith("0x")) {
        return new BigInteger(val.substring(2), 16);
      } else if (val.matches(".*[A-Fa-f].*")) {
        return new BigInteger(val, 16);
      }
    
      return new BigInteger(val);            
    }
    
    private String getAdvisoryNames(ThreadInfo ti) {
      StringBuffer sbuf = new StringBuffer();
      boolean firstEntry = true;
      for (ThreadAdvisory tdadv : ti.getAdvisories()) {
        // if (tdadv.getHealth().ordinal() >= HealthLevel.WATCH.ordinal()) {
        if (!firstEntry)
          sbuf.append(", ");
        sbuf.append(tdadv.getPattern());
        firstEntry = false;
        // }
      }
      return sbuf.toString();    
    }

    /**
     * @return the scrubbedName
     */
    public String getName() {
      return scrubbedName;
    }

    /**
     * @return the threadGroupName
     */
    public String getThreadGroupName() {
      return threadGroupName;
    }

    /**
     * @return the health
     */
    public HealthLevel getHealth() {
      return health;
    }

    /**
     * @return the advisoryNames
     */
    public String getAdvisoryNames() {
      return advisoryNames;
    }

    /**
     * @return the state
     */
    public ThreadState getState() {
      return state;
    }

    /**
     * @return the tid
     */
    public BigInteger getTid() {
      return tid;
    }

    /**
     * @return the nid
     */
    public BigInteger getNid() {
      return nid;
    }

    /**
     * @return the nameId
     */
    public String getNameId() {
      return nameId;
    }

    /**
     * @return the assocThreadInfo
     */
    public ThreadInfo getAssocThreadInfo() {
      return assocThreadInfo;
    }

    /**
     * @param assocThreadInfo the assocThreadInfo to set
     */
    public void setAssocThreadInfo(ThreadInfo assocThreadInfo) {
      this.assocThreadInfo = assocThreadInfo;
    }

    /**
     * @return the ecid
     */
    public String getEcid() {
      return ecid;
    }

    /**
     * @param ecid the ecid to set
     */
    public void setEcid(String ecid) {
      this.ecid = ecid;
    }

    /**
     * @return the compositeFlowID
     */
    public String getCompositeFlowID() {
      return compositeFlowID;
    }

    /**
     * @param compositeFlowID the compositeFlowID to set
     */
    public void setCompositeFlowID(String compositeFlowID) {
      this.compositeFlowID = compositeFlowID;
    }
    
    
  }
  

  protected Vector elements;

  protected String[] columnNames;

  /**
   * 
   * @param root
   */
  public ThreadsTableModel(DefaultMutableTreeNode rootNode) {
    // transform child nodes in proper vector.
    if (rootNode != null) {
      elements = new Vector();
      
      for (int i = 0; i < rootNode.getChildCount(); i++) {
        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
        Object entry = childNode.getUserObject();
        if (entry instanceof ThreadInfo) {
          ThreadInfo ti = (ThreadInfo) entry;
          columnNames = new String[] { "Name", "Thread Group", "Health", "Advisories", "Composite/Flow ID", "ECID", "Thread-ID", "Native-ID", "State"};
          
          // Create the data once inside ThreadData isntead of repeatedly parsing and recreating data...from advisories/tid/nids...
          elements.add(new ThreadData(ti));  
        } else {
          elements.add(childNode.getUserObject());
        }
      }
    }    
  }

  public String getColumnName(int col) {
    return columnNames[col];
  }

  public int getRowCount() {
    return (elements.size());
  }

  public int getColumnCount() {
    return (columnNames.length);
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    ThreadData tidata = ((ThreadData) elements.elementAt(rowIndex));

    switch(columnIndex) {
      case (0): return tidata.getName();
      case (1): return tidata.getThreadGroupName();  
      case (2): return tidata.getHealth();  
      case (3): return tidata.getAdvisoryNames(); 
      case (4): return tidata.getCompositeFlowID();
      case (5): return tidata.getEcid();
      case (6): return tidata.getTid(); 
      case (7): return tidata.getNid(); 
      case (8): return tidata.getState();
      default:
        return null;
    } 
  }

  
  
  /**
   * get the thread info object at the specified line
   * 
   * @param rowIndex
   *          the row index
   * @return thread info object at this line.
   */
  public Object getInfoObjectAtRow(int rowIndex) {
    return (rowIndex >= 0 && rowIndex < getRowCount() ? elements.get(rowIndex) : null);
  }

  /**
   * @inherited
   */
  public Class getColumnClass(int columnIndex) {
    if ((columnIndex == 6) || (columnIndex == 7)) {
      return BigInteger.class;
    } else {
      return String.class;
    }
  }

  /**
   * search for the specified (partial) name in thread names
   * 
   * @param startRow
   *          row to start the search
   * @param name
   *          the (partial) name
   * @return the index of the row or -1 if not found.
   */
  public int searchRowWithName(int startRow, String name) {
    if (name == null || name.equals(""))
      return -1;

    name = name.replaceAll("/", ".");    
    Pattern p = Pattern.compile(name, Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
    
    int count = 0;
    int maxCount = getRowCount();
    int index = (startRow >= 0 )? startRow : 0;
    boolean found = false;
    Matcher m = null;
    while (!found && (count < maxCount)) {
     
      ThreadInfo ti = ((ThreadData) getInfoObjectAtRow((index+count) % maxCount)).getAssocThreadInfo();      
      if (ti == null)
        continue;
      
      m = p.matcher(ti.getName());
      found = m.find();
      count++;
    }

    return (found ? ((index+count) % maxCount) -1 : -1);
  }

  public int searchRowWithContent(int startRow, String searchContent) {
    if (searchContent == null || searchContent.equals(""))
      return -1;
    
    searchContent = searchContent.replaceAll("/", ".");    
    Pattern p = Pattern.compile(searchContent, Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
    
    int count = 0;
    int maxCount = getRowCount();
    int index = (startRow >= 0 )? startRow : 0;
    boolean found = false;
    Matcher m = null;
    while (!found && (count < maxCount)) {
     
      ThreadInfo ti = ((ThreadData) getInfoObjectAtRow((index+count) % maxCount)).getAssocThreadInfo();      
      if (ti == null)
        continue;
      
      m = p.matcher(ti.getContent());
      found = m.find();
      count++;
    }

    return (found ? ((index+count) % maxCount) -1 : -1);
  }
}
