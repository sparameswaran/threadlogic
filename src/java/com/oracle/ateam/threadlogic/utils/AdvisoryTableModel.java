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
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;

import java.util.Vector;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * table model for displaying thread overview.
 * 
 * @author irockel
 */
public class AdvisoryTableModel extends ThreadsTableModel {

  /**
   * 
   * @param root
   */
  public AdvisoryTableModel(DefaultMutableTreeNode rootNode) {
    super(rootNode);
    columnNames = new String[] { "Name", "Health", "Keyword", "Description", "Advice" };
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    ThreadAdvisory advisory = ((ThreadAdvisory) elements.elementAt(rowIndex));

    switch (columnIndex) {
    case 0:
      return advisory.getPattern();
    case 1:
      return advisory.getHealth();
    case 2:
      int count = advisory.getKeywordList().length;
      if (count == 1)
        return advisory.getKeyword();
      
      String[] list = advisory.getKeywordList();
      StringBuffer sbuf = new StringBuffer(list[0]);
      for (int i = 1; i < count; i++)
        sbuf.append(", " + list[i] );
      return sbuf.toString();
      
    case 3:
      return advisory.getDescrp();
    case 4:
      return advisory.getAdvice();
    }

    return advisory.getKeyword();
  }

  /**
   * @inherited
   */
  public Class getColumnClass(int columnIndex) {
    return String.class;
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
      ThreadAdvisory tadv = (ThreadAdvisory) getInfoObjectAtRow(i++);
      if (tadv == null)
        continue;
      
      found = ((tadv.getKeyword().indexOf(name) >= 0) || (tadv.getPattern().indexOf(name) >= 0));
    }
    return (found ? i - 1 : -1);
  }

}
