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
 * TreeRenderer.java
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
 * $Id: TreeRenderer.java,v 1.10 2008-03-13 21:16:08 irockel Exp $
 */

package com.oracle.ateam.threadlogic.utils;

import com.oracle.ateam.threadlogic.HistogramInfo;
import com.oracle.ateam.threadlogic.LogFileContent;
import com.oracle.ateam.threadlogic.Logfile;
import com.oracle.ateam.threadlogic.ThreadLogic;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.categories.Category;

import java.awt.Color;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * adds icons to tda root tree
 * 
 * @author irockel
 */
public class TreeRenderer extends DefaultTreeCellRenderer {

  public TreeRenderer() {
    // empty constructor
  }

  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
      int row, boolean hasFocus) {

    super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    if (leaf && isCategory(value)) {
      setIcon(getIconFromCategory(value));
    } else if (leaf && isThreadInfo(value)) {
      setIcon(ThreadLogic.createImageIcon("Thread.gif"));
    } else if (leaf && isHistogramInfo(value)) {
      setIcon(ThreadLogic.createImageIcon("Histogram.gif"));
    } else if (leaf && isLogfile(value)) {
      setIcon(ThreadLogic.createImageIcon("Root.gif"));
    } else if (leaf && isLogFileContent(value)) {
      setIcon(ThreadLogic.createImageIcon("LogfileContent.gif"));
    } else if (!leaf) {
      if (((DefaultMutableTreeNode) value).isRoot() || isLogfile(value)) {
        setIcon(ThreadLogic.createImageIcon("Root.gif"));
      } else if (isThreadInfo(value)) {
        if (((ThreadInfo) ((DefaultMutableTreeNode) value).getUserObject()).areALotOfWaiting()) {
          setIcon(ThreadLogic.createImageIcon("MonitorRed.gif"));
        } else {
          setIcon(ThreadLogic.createImageIcon("Monitor.gif"));
        }
      } else {
        setIcon(ThreadLogic.createImageIcon("ThreadDump.gif"));
      }
    }
    this.setBackgroundNonSelectionColor(new Color(0, 0, 0, 0));

    return this;
  }

  protected boolean isCategory(Object value) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    return (node.getUserObject() instanceof Category);
  }

  protected Icon getIconFromCategory(Object value) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    Category nodeInfo = (Category) node.getUserObject();

    return (nodeInfo.getIcon());
  }

  private boolean isHistogramInfo(Object value) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    return (node.getUserObject() instanceof HistogramInfo);
  }

  private boolean isThreadInfo(Object value) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    return ((node.getUserObject() instanceof ThreadInfo));
  }

  private boolean isLogfile(Object value) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    return (node.getUserObject() instanceof Logfile);
  }

  private boolean isLogFileContent(Object value) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    return (node.getUserObject() instanceof LogFileContent);
  }
}
