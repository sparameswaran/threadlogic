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
package com.oracle.ateam.threadlogic.advisories;

import java.util.ArrayList;

import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.LockInfo;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.ThreadState;
import java.util.regex.Pattern;

/**
 *
 * @author saparam
 */
public class CustomizedThreadGroup extends ThreadGroup {      
   
  public CustomizedThreadGroup(String grpName) {
    super(grpName);
  }
  
  public void runAdvisory() {    
    super.runAdvisory();    
    runGroupAdvisory();
  }
  
  public void runAdvisory(boolean force) {    
    super.runAdvisory();    
    runGroupAdvisory();
  }
  
  
  public void runGroupAdvisory() {
  } 
  
  public String getCustomizedOverview() {
    return null;
  }
  
  
  /**
   * creates the overview information for this thread group.
   */
  protected void createOverview() {  
    setOverview(getBaseOverview() + getCustomizedOverview() 
            + getEndOfBaseOverview() + getCritOverview());
  }
}
