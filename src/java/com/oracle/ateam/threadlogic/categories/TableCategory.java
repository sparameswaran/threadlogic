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

import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup;
import com.oracle.ateam.threadlogic.filter.FilterChecker;
import com.oracle.ateam.threadlogic.utils.AdvisoryTableModel;
import com.oracle.ateam.threadlogic.utils.ColoredTable;
import com.oracle.ateam.threadlogic.utils.PrefManager;
import com.oracle.ateam.threadlogic.utils.TableSorter;
import com.oracle.ateam.threadlogic.utils.ThreadDiffsTableModel;
import com.oracle.ateam.threadlogic.utils.ThreadGroupsTableModel;
import com.oracle.ateam.threadlogic.utils.ThreadsTableModel;
import com.oracle.ateam.threadlogic.utils.ThreadsTableSelectionModel;

import java.util.EventListener;
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
public class TableCategory extends AbstractCategory {
  protected transient JTable filteredTable;

  /**
   * Creates a new instance of TableCategory
   */
  public TableCategory(String name, int iconID) {
    this(name, iconID, true);
  }

  /**
   * Creates a new instance of TableCategory
   */
  public TableCategory(String name, int iconID, boolean filtering) {
    setName(name);
    setFilterEnabled(filtering);
    setIconID(iconID);
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
        ThreadsTableModel ttm = null;
        DefaultMutableTreeNode firstChildNode = (DefaultMutableTreeNode) filteredRootNode.getChildAt(0);
        Object usrObj = firstChildNode.getUserObject();
        if (firstChildNode.getUserObject() instanceof ThreadAdvisory) {
          ttm = new AdvisoryTableModel(filterNodes(getRootNode()));
        } else if (firstChildNode.getUserObject() instanceof ThreadGroup) {
          ttm = new ThreadGroupsTableModel(filterNodes(getRootNode()));
        } else {
          ttm = new ThreadsTableModel(filterNodes(getRootNode()));
        }

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
        ThreadsTableModel ttm = null;
        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) rootNode.getChildAt(0);
        if (((DefaultMutableTreeNode) rootNode.getChildAt(0)).getUserObject() instanceof ThreadAdvisory) {
          ttm = new AdvisoryTableModel(rootNode);
        } else if (((DefaultMutableTreeNode) rootNode.getChildAt(0)).getUserObject() instanceof ThreadGroup) {
          ttm = new ThreadGroupsTableModel(rootNode);
        } else {
          ttm = new ThreadsTableModel(rootNode);
        }
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

    // Earlier only two different views have to be dealt with,
    // with more the model should be subclassed.
    // Now with Thread Advisory also...
    int columnCount = tm.getColumnCount();
    if (tm instanceof AdvisoryTableModel) {
      // This is for the ThreadAdvisory table ...
      filteredTable.getColumnModel().getColumn(0).setPreferredWidth(200);
      filteredTable.getColumnModel().getColumn(1).setPreferredWidth(70);
      filteredTable.getColumnModel().getColumn(2).setPreferredWidth(300);
      filteredTable.getColumnModel().getColumn(3).setPreferredWidth(300);
      filteredTable.getColumnModel().getColumn(4).setPreferredWidth(500);

      filteredTable.getColumnModel().getColumn(0).setCellRenderer(renderer);
      filteredTable.getColumnModel().getColumn(1).setCellRenderer(renderer);

    } else if (tm instanceof ThreadGroupsTableModel) {
      // This is for the ThreadGroups table ...

      filteredTable.getColumnModel().getColumn(0).setPreferredWidth(200);
      filteredTable.getColumnModel().getColumn(1).setPreferredWidth(70);

      for (int i = 2; i < columnCount - 1; i++) {
        filteredTable.getColumnModel().getColumn(i).setPreferredWidth(30);
      }
      filteredTable.getColumnModel().getColumn(columnCount - 1).setPreferredWidth(300);

      filteredTable.getColumnModel().getColumn(0).setCellRenderer(renderer);
      filteredTable.getColumnModel().getColumn(1).setCellRenderer(renderer);

    } else {

      if (tm.getColumnCount() > 3) {
        filteredTable.getColumnModel().getColumn(0).setPreferredWidth(300); //name
        filteredTable.getColumnModel().getColumn(1).setPreferredWidth(80); //thread group
        filteredTable.getColumnModel().getColumn(2).setPreferredWidth(70); //health
        filteredTable.getColumnModel().getColumn(3).setPreferredWidth(250); //Advisories
        filteredTable.getColumnModel().getColumn(4).setPreferredWidth(80); // Composite/FlowID
        filteredTable.getColumnModel().getColumn(5).setPreferredWidth(40); //ECID
        filteredTable.getColumnModel().getColumn(6).setPreferredWidth(30); //Thread ID
        filteredTable.getColumnModel().getColumn(7).setPreferredWidth(20); //Native ID
        filteredTable.getColumnModel().getColumn(8).setPreferredWidth(40); //State
        
        filteredTable.getColumnModel().getColumn(4).setCellRenderer(renderer);
        // filteredTable.getColumnModel().getColumn(5).setCellRenderer(renderer);
        // filteredTable.getColumnModel().getColumn(6).setCellRenderer(renderer);
      } else {
        filteredTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        filteredTable.getColumnModel().getColumn(1).setPreferredWidth(30);
        filteredTable.getColumnModel().getColumn(2).setPreferredWidth(50);

        filteredTable.getColumnModel().getColumn(1).setCellRenderer(renderer);
      }
    }
  }

  /**
   * get the currently selected user object.
   * 
   * @return the selected object or null otherwise.
   */
  public Object getCurrentlySelectedUserObject() {
    return (filteredTable == null || filteredTable.getSelectedRow() < 0 ? null
        : ((DefaultMutableTreeNode) getRootNode().getChildAt(filteredTable.getSelectedRow())).getUserObject());
  }

}
