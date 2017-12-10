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
 * $Id: Category.java,v 1.19 2008-03-09 06:36:50 irockel Exp $
 */
package com.oracle.ateam.threadlogic.categories;

import com.oracle.ateam.threadlogic.filter.FilterChecker;

import java.util.Comparator;
import java.util.EventListener;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 
 * @author irockel
 */
public interface Category {

  /**
   * add the passed node to the category tree
   */
  void addToCatNodes(DefaultMutableTreeNode node);

  /**
   * get the node at the given position
   * 
   * @param index
   *          the index to look up.
   * @return the node at the given index, null otherwise.
   */
  public DefaultMutableTreeNode getNodeAt(int index);

  /**
   * return category tree with filtered child nodes
   */
  JComponent getCatComponent(EventListener listener);

  /**
   * get the currently selected user object.
   * 
   * @return
   */
  Object getCurrentlySelectedUserObject();

  FilterChecker getFilterChecker();

  Icon getIcon();

  int getIconID();

  /**
   * get the last view if there is one, null otherwise.
   * 
   * @return the last view
   */
  JScrollPane getLastView();

  String getName();

  int getNodeCount();

  /**
   * return amount of filtered nodes
   */
  int howManyFiltered();

  void setLastView(JScrollPane view);

  void setName(String value);

  int showing();

  /**
   * sorts the category tree by the given comparator.
   * 
   * @param nodeComp
   */
  void sort(Comparator nodeComp);

  void setInfo(String info);

  String getInfo();

}
