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

import com.oracle.ateam.threadlogic.filter.CompositeFilter;
import com.oracle.ateam.threadlogic.filter.Filter;
import java.util.ArrayList;

/**
 *
 * @author saparam
 */
public class ComplexGroup  {
  private String name;
  private boolean visible;
  
  private ArrayList<String> inclusionList;
  private ArrayList<String> exclusionList;
  private ArrayList<String> excludedAdvisories;

  private ComplexGroup(String name) {
    this.name = name;
    this.inclusionList = new ArrayList<String>();
    this.exclusionList = new ArrayList<String>();
    this.excludedAdvisories = new ArrayList<String>();
  }
  
  public ComplexGroup(String name, boolean visible) {
    this(name);
    this.visible = visible;
  }

  public String toString() {
    StringBuffer sbuf = new StringBuffer();
    
    sbuf.append("[ComplexGrp: " + name);
    sbuf.append(", visible: " + visible + "]");
    return sbuf.toString();
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof ComplexGroup))
      return false;
    
    ComplexGroup cmp = (ComplexGroup)o;
    
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

  public void addToInclusionList(String grp) {
    if (!inclusionList.contains(grp))
      inclusionList.add(grp);
  }

  public void addToExclusionList(String grp) {
    if (!exclusionList.contains(grp))
      exclusionList.add(grp);
  }
  
  
  /**
   * @return the inclusionList
   */
  public ArrayList<String> getInclusionList() {
    return inclusionList;
  }

  /**
   * @param inclusionList the inclusionList to set
   */
  public void setInclusionList(ArrayList<String> inclusionList) {
    this.inclusionList = inclusionList;
  }

  /**
   * @return the exclusionList
   */
  public ArrayList<String> getExclusionList() {
    return exclusionList;
  }

  /**
   * @param exclusionList the exclusionList to set
   */
  public void setExclusionList(ArrayList<String> exclusionList) {
    this.exclusionList = exclusionList;
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
