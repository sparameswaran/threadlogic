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

/**
 *
 * @author saparam
 */
public class ADFThreadGroup extends CustomizedThreadGroup {    
  
  public ADFThreadGroup(String grpName) {
    super(grpName);
  }
  
 
  public void runGroupAdvisory() {
    
    this.health = HealthLevel.NORMAL;
    for(ThreadInfo ti: threads) {
      // Downgrade health levels for ADF threads if they are marked STUCK
      resetAdvisoriesBasedOnThread(ti, ti.getAdvisories());
      HealthLevel tiHealth = ti.getHealth();
    
      // If the thread's health is at a higher level, set the thread group
      // health to that higher level
      // Not directly going with the Advisory default health level as we
      // downgrade level for muxer/adapters...
      // So compare against the thread's health instead of advisory.
      if (tiHealth.ordinal() > this.health.ordinal()) {
        this.health = tiHealth;
      }
    }
  }   
  
  public static void resetAdvisoriesBasedOnThread(ThreadInfo threadInfo, ArrayList<ThreadAdvisory> advisoryList) {

    boolean isAtFatalLevel = (threadInfo.getHealth() == HealthLevel.FATAL);
    
    ThreadAdvisory stuckAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.STUCK_PATTERN);        
    
    if (advisoryList.contains(stuckAdvisory) && isAtFatalLevel) {
      threadInfo.setHealth(HealthLevel.WARNING);
      return;
    }
  }
  
}
