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
 * $Id: TableCategory.java,v 1.7 2008-03-09 06:36:51 irockel Exp $
 */
package com.oracle.ateam.threadlogic.categories;

import com.oracle.ateam.threadlogic.ThreadDumpInfo;
import com.oracle.ateam.threadlogic.filter.FilterChecker;
import com.oracle.ateam.threadlogic.utils.ColoredTable;
import com.oracle.ateam.threadlogic.utils.PrefManager;
import com.oracle.ateam.threadlogic.utils.TableSorter;
import com.oracle.ateam.threadlogic.utils.ThreadDiffsTableModel;
import com.oracle.ateam.threadlogic.utils.ThreadsTableModel;
import com.oracle.ateam.threadlogic.utils.ThreadsTableSelectionModel;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * table category type, displays its content in a table.
 *
 * @author irockel
 */
public class ThreadDiffsTableCategory extends TableCategory {

  private ArrayList<ThreadDumpInfo> threadDumpArrList;

  /**
   * Creates a new instance of TableCategory
   */
  public ThreadDiffsTableCategory(String name, int iconID) {
    super(name, iconID, true);
  }

  public void setThreadDumps(ArrayList<ThreadDumpInfo> threadDumpArrList) {
    this.threadDumpArrList = threadDumpArrList;
  }

  /**
   * @inherited
   */
  public JComponent getCatComponent(EventListener listener) {
    if (isFilterEnabled()
        && ((filteredTable == null) || (getLastUpdated() < PrefManager.get().getFiltersLastChanged()))) {
      // first refresh filter checker with current filters
      setFilterChecker(FilterChecker.getFilterChecker());

      // apply new filter settings.
      DefaultMutableTreeNode filteredRootNode = filterNodes(getRootNode());
      if (filteredRootNode != null && filteredRootNode.getChildCount() > 0) {
        ThreadsTableModel ttm = new ThreadDiffsTableModel(filterNodes(getRootNode()), threadDumpArrList);

        // create table instance (filtered)
        setupTable(ttm, listener);
      } else {
        // just an empty table
        filteredTable = new JTable();
      }

      setLastUpdated();
    } else if (!isFilterEnabled()
        && ((filteredTable == null) || (getLastUpdated() < PrefManager.get().getFiltersLastChanged()))) {
      // create unfiltered table view.
      DefaultMutableTreeNode rootNode = getRootNode();
      if (rootNode.getChildCount() > 0) {
        ThreadsTableModel ttm = new ThreadDiffsTableModel(rootNode, threadDumpArrList);

        // create table instance (unfiltered)
        setupTable(ttm, listener);
      }
    }
    return (filteredTable);
  }

  /**
   * setup the table instance with the specified table model (either filtered or
   * none-filtered).
   *
   * @param ts
   *          the table sorter/model to use.
   * @param listener
   *          the event listener to add to the table
   */
  protected void setupTable(TableModel tm, EventListener listener) {
    TableSorter ts = new TableSorter(tm);
    filteredTable = new ColoredTable(ts);
    ts.setTableHeader(filteredTable.getTableHeader());
    filteredTable.setSelectionModel(new ThreadsTableSelectionModel(filteredTable));
    filteredTable.getSelectionModel().addListSelectionListener((ListSelectionListener) listener);

    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
    renderer.setHorizontalAlignment(JLabel.RIGHT);
    int width = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;

    int noOfTDs = threadDumpArrList.size();
    int healthProgressBarWidth = (noOfTDs * 10 < 70)? 70:(noOfTDs * 10);
    //int progressColumnWidth = (noOfTDs < 5)? 60:25;
    int progressColumnWidth = 60;

    filteredTable.getColumnModel().getColumn(0).setPreferredWidth(width/7);
    filteredTable.getColumnModel().getColumn(0).setMinWidth(width/12);
    filteredTable.getColumnModel().getColumn(1).setPreferredWidth(width/9);
    filteredTable.getColumnModel().getColumn(1).setMinWidth(width/27);
    filteredTable.getColumnModel().getColumn(2).setPreferredWidth(width/24);
    filteredTable.getColumnModel().getColumn(3).setPreferredWidth(width/38);
    filteredTable.getColumnModel().getColumn(4).setPreferredWidth(65);
    filteredTable.getColumnModel().getColumn(5).setPreferredWidth(width/9);
    filteredTable.getColumnModel().getColumn(5).setMinWidth(width/12);
    for (int i = 0; i < threadDumpArrList.size() - 1; i++)
      filteredTable.getColumnModel().getColumn(i + 6).setPreferredWidth(width/32);

    //filteredTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
  }
}
