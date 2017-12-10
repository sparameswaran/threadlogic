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

import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * table model for displaying thread groups overview.
 * 
 * @author Sabha
 */
public class ThreadGroupsTableModel extends ThreadsTableModel {

  /**
   * 
   * @param root
   */
  public ThreadGroupsTableModel(DefaultMutableTreeNode rootNode) {
    super(rootNode);
    columnNames = new String[] { "Thread Group Name", "Health", "Threads", "Blocked", "Running", "Other states",
        "Advisories Found" };
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    ThreadGroup tg = ((ThreadGroup) elements.elementAt(rowIndex));

    switch (columnIndex) {
    case 0:
      return tg.getThreadGroupName();
    case 1:
      return tg.getHealth();
    case 2:
      return tg.getThreads().size();
    case 3:
      return tg.getNoOfBlockedThreads();
    case 4:
      return tg.getNoOfRunningThreads();
    case 5:
      return tg.getThreads().size() - (tg.getNoOfBlockedThreads() + tg.getNoOfRunningThreads());
    case 6:
      StringBuffer sbuf = new StringBuffer();
      boolean firstEntry = true;
      for (ThreadAdvisory tdadv : tg.getAdvisories()) {
        // if (tdadv.getHealth().ordinal() >= HealthLevel.WATCH.ordinal()) {
        if (!firstEntry)
          sbuf.append(", ");
        sbuf.append(tdadv.getPattern());
        firstEntry = false;
        // }
      }
      return sbuf.toString();
    }

    return tg.getThreadGroupName();
  }

  /**
   * @inherited
   */
  public Class getColumnClass(int columnIndex) {
    if (columnIndex > 1 && columnIndex < 6) {
      return Integer.class;
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
    int i = startRow;
    boolean found = false;
    while (!found && (i < getRowCount())) {
      ThreadGroup tg = (ThreadGroup) getInfoObjectAtRow(i++);
      if (tg == null)
        continue;
      
      found = tg.getThreadGroupName().indexOf(name) >= 0;
    }
    return (found ? i - 1 : -1);
  }

}
