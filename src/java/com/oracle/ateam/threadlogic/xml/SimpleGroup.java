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

package com.oracle.ateam.threadlogic.xml;

import com.oracle.ateam.threadlogic.filter.Filter;
import java.util.ArrayList;

/**
 *
 * @author saparam
 */
public class SimpleGroup {
  
  private String name, matchLocation;
  private boolean visible, inclusion;
  private int filterRuleToApply = Filter.HAS_IN_STACK_RULE;
  
  private ArrayList<String> patternList;
  private ArrayList<String> excludedAdvisories;
  
  private SimpleGroup(String name) {
    this.name = name;
  }
  
  public SimpleGroup(String name, boolean visible, boolean inclusionType, String matchLocation) {
    this.name = name;
    this.visible = visible;
    this.inclusion = inclusionType;
    this.matchLocation = matchLocation.toLowerCase();
    if (this.matchLocation.startsWith("name"))
      this.filterRuleToApply = Filter.HAS_IN_TITLE_RULE;
    
    this.patternList = new ArrayList<String>();
    this.excludedAdvisories = new ArrayList<String>();
  }
  
  public String toString() {
    StringBuffer sbuf = new StringBuffer();
    
    sbuf.append("[SimpleGrp: " + name);
    sbuf.append(", visible: " + visible);
    sbuf.append(", inclusion: " + inclusion);
    sbuf.append(", matchLocation: " + matchLocation + "]");
    return sbuf.toString();
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof SimpleGroup))
      return false;
    
    SimpleGroup cmp = (SimpleGroup)o;
    
    return name.equals(cmp.name);
  }
  
  public int hashcode() {
    return name.hashCode();
  }
  
  
  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the matchLocation
   */
  public String getMatchLocation() {
    return matchLocation;
  }

  /**
   * @param matchLocation the matchLocation to set
   */
  public void setMatchLocation(String matchLocation) {
    this.matchLocation = matchLocation;
  }

  /**
   * @return the visible
   */
  public boolean isVisible() {
    return visible;
  }

  /**
   * @param visible the visible to set
   */
  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  /**
   * @return the inclusion
   */
  public boolean isInclusion() {
    return inclusion;
  }

  /**
   * @param inclusion the inclusion to set
   */
  public void setInclusion(boolean inclusion) {
    this.inclusion = inclusion;
  }

  /**
   * @return the patternList
   */
  public ArrayList<String> getPatternList() {
    return patternList;
  }

  /**
   * @param patternList the patternList to set
   */
  public void setPatternList(ArrayList<String> patternList) {
    this.patternList = patternList;
  }
  
  public void addToPatternList(String pattern) {
    if (!patternList.contains(pattern))
      patternList.add(pattern);
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
  public void setExcludedAdvisories(ArrayList<String> patternList) {
    this.excludedAdvisories = patternList;
  }
  
  public void addToExcludedAdvisories(String pattern) {
    if (!excludedAdvisories.contains(pattern))
      excludedAdvisories.add(pattern);
  }
}
