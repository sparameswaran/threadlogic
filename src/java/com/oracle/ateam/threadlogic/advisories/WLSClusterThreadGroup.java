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
public class WLSClusterThreadGroup extends CustomizedThreadGroup {    
  
  public WLSClusterThreadGroup(String grpName) {
    super(grpName);
  }
  
  public void runGroupAdvisory() {
    int noOfNonWebApplnThreads = 0;
    ThreadAdvisory clusterUnhealthyAdvisory 
                                  = ThreadAdvisory.lookupThreadAdvisoryByName("WLS Cluster unhealthy");
    ThreadAdvisory webApplnAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName("Web Application Request");
    ThreadAdvisory sessionReplnAdvisory = 
                              ThreadAdvisory.lookupThreadAdvisoryByName("WLS Replicated Session Secondary");
    
    for(ThreadInfo ti: threads) { 
      
      // If the thread is involved in Cluster but not part of web, 
      // then increment count
      if (!ti.hasAdvisory(webApplnAdvisory)) {
        ++noOfNonWebApplnThreads;        
      }
    }
    
    // If there are more than 5 threads involved in cluster related work 
    // that is neither web nor session replication related,
    // Mark the cluster threads as bad
    if (noOfNonWebApplnThreads >= 5) {        

      addAdvisory(clusterUnhealthyAdvisory);
      if (this.getHealth().ordinal() < clusterUnhealthyAdvisory.getHealth().ordinal())
        this.setHealth(clusterUnhealthyAdvisory.getHealth());      

      for(ThreadInfo ti: threads) { 
        if (!ti.hasAdvisory(webApplnAdvisory)) {
          ti.getAdvisories().add(clusterUnhealthyAdvisory);
          ti.setHealth(clusterUnhealthyAdvisory.getHealth());          
        }
      }
    }
  }  
  
  
}
