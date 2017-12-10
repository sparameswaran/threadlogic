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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.ateam.threadlogic.filter;

import com.oracle.ateam.threadlogic.ThreadInfo;

import java.util.ArrayList;

/**
 * 
 * @author saparam
 */
public class CompositeFilter extends Filter {

  String name;
  ArrayList<Filter> inclusionFilters = new ArrayList<Filter>();
  ArrayList<Filter> exclusionFilters = new ArrayList<Filter>();

  public CompositeFilter(String name) {
    setName(name);
  }

  public void addFilter(Filter filter, boolean match) {
    if (match)
      inclusionFilters.add(filter);
    else
      exclusionFilters.add(filter);
  }

  public ArrayList<Filter> getFilters(boolean inclusion) {
    if (inclusion)
      return inclusionFilters;
    else
      return exclusionFilters;
  }

  public boolean matches(ThreadInfo ti, boolean forceEnabled) {

    boolean matchFound = false;
    for (Filter includeFilter : inclusionFilters) {
      matchFound = includeFilter.matches(ti);
      if (matchFound)
        break;
    }

    if (inclusionFilters.size() > 0 && !matchFound)
      return false;

    for (Filter excludeFilter : exclusionFilters) {
      matchFound = excludeFilter.matches(ti);
      if (matchFound)
        return false;
    }

    return true;
  }

}
