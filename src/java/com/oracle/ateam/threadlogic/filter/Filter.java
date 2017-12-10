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
 * Filter.java
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
 * $Id: Filter.java,v 1.11 2008-03-09 06:36:51 irockel Exp $
 */
package com.oracle.ateam.threadlogic.filter;

import com.oracle.ateam.threadlogic.advisories.ThreadGroup;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.ThreadLogicElement;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * represents a filter for filtering threads or monitors to display
 * 
 * @author irockel
 */
public class Filter {

  // static defines for filter rules.
  public static final int HAS_IN_TITLE_RULE = 0;
  public static final int MATCHES_TITLE_RULE = 1;
  public static final int HAS_IN_STACK_RULE = 2;
  public static final int MATCHES_STACK_RULE = 3;
  public static final int WAITING_ON_RULE = 4;
  public static final int WAITING_FOR_RULE = 5;
  public static final int LOCKING_RULE = 6;
  public static final int SLEEPING_RULE = 7;
  public static final int STACK_IS_LONGER_THAN_RULE = 8;

  /**
   * name of this filter, just something describing for this filter
   */
  private String name = null;

  /**
   * a regular expression of the filter
   */
  private String filterExpression = null;

  /**
   * the precompiled pattern.
   */
  private Pattern filterExpressionPattern = null;

  /**
   * true, if filter is a general filter, which should be applied to all thread
   * infos
   */
  private boolean generalFilter = false;

  /**
   * specifies if this filter is a exclusion filter
   */
  private boolean exclusionFilter = false;

  /**
   * specifies if this filter is currently active
   */
  private boolean enabled = false;

  /**
   * specifies the filter rule which the filter expression applies to
   */
  private int filterRule = 0;

  /**
   * specifies the filter details (description)
   */

  private String info = null;

  /**
   * specifies the Advisories to be excluded 
   */

  private ArrayList<String> excludedAdvisories = null;

  /**
   * empty default constructor
   */
  public Filter() {
  }

  /**
   * Creates a new instance of Filter
   * 
   * @param name
   *          the name of the filter
   * @param regEx
   *          the reg ex of the filter
   * @param gf
   *          true, if filter is general filter
   */
  public Filter(String name, String regEx, int fr, boolean gf, boolean exf, boolean enabled) {
    setName(name);
    setFilterExpression(regEx);
    setGeneralFilter(gf);
    setExclusionFilter(exf);
    setFilterRule(fr);
    setEnabled(enabled);
  }

  /**
   * Copy Constructor
   * @param copyFilter 
   */
  public Filter(Filter copyFilter) {
    setName(copyFilter.name);
    setFilterExpression(copyFilter.filterExpression);
    setGeneralFilter(copyFilter.generalFilter);
    setExclusionFilter(copyFilter.exclusionFilter);
    setFilterRule(copyFilter.filterRule);
    setEnabled(enabled);
    setInfo(copyFilter.getInfo());
    setExcludedAdvisories(copyFilter.excludedAdvisories);
  }

  /**
   * set the name of this filter
   */
  public void setName(String value) {
    name = value;
  }

  /**
   * get filter name
   */
  public String getName() {
    return (name);
  }

  /**
   * get the filter expression as string
   */
  public String getFilterExpression() {
    return (filterExpression);
  }

  public void setFilterExpression(String regEx) {
    filterExpression = regEx;
    // reset any precompiled data.
    filterExpressionPattern = null;
  }

  /**
   * get the filter expression as precompiled pattern
   */
  public Pattern getFilterExpressionPattern() {
    if (filterExpressionPattern == null) {
      filterExpressionPattern = Pattern.compile(getFilterExpression(), Pattern.DOTALL);
    }

    return (filterExpressionPattern);
  }

  /**
   * set general filter flag
   */
  public void setGeneralFilter(boolean value) {
    generalFilter = value;
  }

  /**
   * @return true, if filter is a general filter
   */
  public boolean isGeneralFilter() {
    return (generalFilter);
  }

  /**
   * set exclusion filter flag
   */
  public void setExclusionFilter(boolean value) {
    exclusionFilter = value;
  }

  /**
   * @return true, if filter is a exclusion filter
   */
  public boolean isExclusionFilter() {
    return (exclusionFilter);
  }

  public int getFilterRule() {
    return filterRule;
  }

  public void setFilterRule(int filterRule) {
    this.filterRule = filterRule;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean matches(ThreadLogicElement tle) {
    if (tle instanceof ThreadInfo) {
      return (matches((ThreadInfo) tle, true));
    }
    if (tle instanceof ThreadGroup) {
      return (matches((ThreadGroup) tle, true));
    }
    return true;
  }

  private boolean matches(ThreadGroup tle, boolean forceEnabled) {
    return true;
  }

  public boolean matches(ThreadInfo ti, boolean forceEnabled) {
    boolean result = true;
    if (forceEnabled || isEnabled()) {
      switch (getFilterRule()) {
      case HAS_IN_TITLE_RULE:
        result = getFilterExpressionPattern().matcher(ti.getName()).find();
        break;
      case MATCHES_TITLE_RULE:
        result = getFilterExpressionPattern().matcher(ti.getName()).matches();
        break;
      case HAS_IN_STACK_RULE:
        result = getFilterExpressionPattern().matcher(ti.getContent()).find();
        break;
      case MATCHES_STACK_RULE:
        result = getFilterExpressionPattern().matcher(ti.getContent()).matches();
        break;
      case WAITING_ON_RULE:
        result = (ti.getContent().indexOf("- waiting on") >= 0) && checkLine(ti, "- waiting on", '<', ')');
        break;
      case WAITING_FOR_RULE:
        result = (ti.getName().indexOf("waiting for monitor entry") >= 0)
            && checkLine(ti, "- waiting to lock", '<', ')');
        break;
      case LOCKING_RULE:
        result = (ti.getContent().indexOf("- locked") >= 0) && checkLine(ti, "- locked", '<', ')');
        break;
      case SLEEPING_RULE:
        result = (ti.getName().indexOf("Object.wait()") >= 0);
        break;
      case STACK_IS_LONGER_THAN_RULE:
        result = (ti.getStackLines() == 0) || ((ti.getStackLines() - 2) > Integer.parseInt(filterExpression));
        break;
      }

      // invert if it is exclusion filter
      if (isExclusionFilter()) {
        result = !result;
      }
    }
    return (result);
  }

  /**
   * checks a sub line for a lock handler (for waiting, locking, monitor entry)
   */
  private boolean checkLine(ThreadInfo ti, String contains, char beginChar, char endChar) {
    int beginFrom = ti.getContent().indexOf(contains);
    int beginIndex = ti.getContent().indexOf(beginChar, beginFrom);
    int endIndex = ti.getContent().indexOf(endChar, beginIndex);
    String matchLine = ti.getContent().substring(beginIndex, endIndex);

    return getFilterExpressionPattern().matcher(matchLine).matches();
  }

  public String toString() {
    // (general) removed atm.
    return (getName() + (isGeneralFilter() ? "" : "") + (isEnabled() ? " (default)" : ""));
  }

  public String getInfo() {
    return info;
  }

  public void setInfo(String info) {
    this.info = info;
  }
  
  /**
   * @return the excludedAdvisories
   */
  public ArrayList<String> getExcludedAdvisories() {
    return excludedAdvisories;
  }

  /**
   * @param excludedAdvisories the excludedAdvisories to set
   */
  public void setExcludedAdvisories(ArrayList<String> excludedAdvisories) {
    this.excludedAdvisories = excludedAdvisories;
  }
}
