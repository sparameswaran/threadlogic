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
package com.oracle.ateam.threadlogic;

import java.util.ArrayList;
import java.util.Collections;

import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.advisories.ThreadLogicConstants;
import com.oracle.ateam.threadlogic.utils.CustomLogger;
import java.util.logging.Logger;

/**
 * Contains all the relevant structure associated with any thread dump element
 * Advisory, state, name, health etc...
 * 
 * @author saparam
 */
public class ThreadLogicElement extends AbstractInfo implements Comparable {

  protected HealthLevel health = HealthLevel.IGNORE;
  protected ThreadState state;
  protected ArrayList<ThreadAdvisory> advisories = new ArrayList<ThreadAdvisory>();
  
  private static Logger theLogger = CustomLogger.getLogger(ThreadLogic.class.getSimpleName());  
  
  public ThreadLogicElement(String id) {
    super.setName(id);
  }
  
  public ThreadLogicElement(ThreadLogicElement copy) {
    super.setName(copy.name);
    setState(copy.state);
    setHealth(copy.health);
    
    for(ThreadAdvisory tadv: copy.advisories)
      advisories.add(tadv);
  }

  public boolean equals(Object o) {
    if (o == this)
      return true;

    if (!(o instanceof ThreadLogicElement))
      return false;

    if (o != null) {
      ThreadLogicElement cmp = (ThreadLogicElement) o;
      return (cmp.getName() != null && cmp.getName().equals(this.getName()));
    }
    return false;
  }

  public int hashcode() {
    return this.getName().hashCode();
  }

  public int compareTo(Object o) {
    return this.health.compareTo(((ThreadLogicElement) o).getHealth());
  }

  public ArrayList<ThreadAdvisory> getAdvisories() {
    return ThreadAdvisory.sortByHealth(advisories);
  }

  public void setAdvisories(ArrayList<ThreadAdvisory> advisories) {
    this.advisories = advisories;
  }

  public HealthLevel getHealth() {
    return health;
  }

  public void setHealth(HealthLevel health) {    
    this.health = health;
  }

  public ThreadState getState() {
    return state;
  }

  public void setState(ThreadState state) {
    this.state = state;
  }

  public void runAdvisory() {
  }

  public synchronized void removeAdvisory(String keyword) {    
    this.advisories.remove(ThreadAdvisory.lookupThreadAdvisory((keyword)));
  }
  
  public synchronized void removeAdvisory(ThreadAdvisory advisory) {
    this.advisories.remove(advisory);
  }
  
  public synchronized void addAdvisory(ThreadAdvisory advisory) {
    if ((advisories != null) && !advisories.contains(advisory)) {
      this.advisories.add(advisory);
    }
  }

  public synchronized void addAdvisories(ArrayList<ThreadAdvisory> advisories) {
    if (advisories != null) {
      for (ThreadAdvisory advisory : advisories) {
        
        if (!this.advisories.contains(advisory))
          this.advisories.add(advisory);
      }
    }
  }

  public synchronized void insertAdvisory(ThreadAdvisory advisory) {
    if (!advisories.contains(advisory)) {
      this.advisories.add(0, advisory);
    } else {
      this.advisories.remove(advisory);
      this.advisories.add(0, advisory);
    }

  }

    
  // Reset the Health level for the advisory list (due to exclusions)
  public void recalibrateHealthForExcludedAdvisories( HealthLevel downgradedLevel, ArrayList<ThreadAdvisory> exclusionList) {
    
    theLogger.finest(this.getName()+">> HealthLevel before recalibration:" + this.health);
    
    HealthLevel highestLevel = HealthLevel.IGNORE;
    ThreadState state = this.getState();
    
    for (ThreadAdvisory curAdvisory : this.advisories) {
      if (exclusionList.contains(curAdvisory)) {
        curAdvisory.setHealth(downgradedLevel);
      } else if (curAdvisory.getHealth().ordinal() > highestLevel.ordinal()) {
          highestLevel = curAdvisory.getHealth();
      }
    }

    this.setHealth(highestLevel);    
    theLogger.finest(this.getName()+">> HealthLevel after recalibration:" + this.health);
  }
  
  public synchronized ArrayList<ThreadAdvisory> getCritAdvisories() {
    ArrayList<ThreadAdvisory> critList = new ArrayList<ThreadAdvisory>();

    for (ThreadAdvisory entry : this.getAdvisories()) {
      switch (entry.getHealth()) {
      case FATAL:
      case WARNING:
      case WATCH:
        critList.add(entry);
        break;
      }
    }
    return critList;
  }

  public static <T extends ThreadLogicElement> ArrayList<T> sortByHealth(T[] arr) {

    ArrayList<T> list = new ArrayList<T>();
    for (Object o : arr) {
      list.add((T) o);
    }

    return sortByHealth(list);
  }

  public static <T extends ThreadLogicElement> ArrayList<T> sortByHealth(ArrayList<T> list) {

    // Check if there is nothing to sort...
    if ((list == null) || (list.size() <= 1))
      return list;

    // Sort using the underlying Health level
    Collections.sort(list);

    // Reverse for descending order of severity
    Collections.reverse(list);
    return list;
  }

  public boolean markedAsStuck() {
    if ((this.advisories == null) || (this.advisories.size() == 0)) {
      return (getName().contains("STUCK"));
    }
    
    return this.advisories.contains(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.STUCK_PATTERN));
  }
}
