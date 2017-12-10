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
import com.oracle.ateam.threadlogic.ThreadInfo;

/**
 *
 * @author saparam
 */
public class JVMThreadGroup extends CustomizedThreadGroup {    
  
  protected int gcThreads;
  protected boolean isFinalizerBlocked = false;
  
  public JVMThreadGroup(String grpName) {
    super(grpName);
  }
  
  public void runGroupAdvisory() {
    ArrayList<ThreadAdvisory> advisories = new ArrayList<ThreadAdvisory>();
    ArrayList<ThreadInfo> threads = this.getThreads();
    
    for (ThreadInfo thread : threads) {
      String threadName = thread.getName();
      if (threadName.contains(ThreadLogicConstants.FINALIZER_THREAD)) {
        
        if (thread.isBlockedForLock() && (thread.getBlockedForLock() != null) ) {
          isFinalizerBlocked = true;
          thread.addAdvisory(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.FINALIZER_THREAD_BLOCKED));
          Thread.dumpStack();
          thread.setHealth(HealthLevel.FATAL);
          this.setHealth(HealthLevel.FATAL);
          advisories.addAll(thread.getAdvisories());

          continue;
          
        } else {
          isFinalizerBlocked = false;
          thread.removeAdvisory(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.FINALIZER_THREAD_BLOCKED));
          thread.setHealth(HealthLevel.NORMAL);
        }
      }

      if (threadName.toUpperCase().contains("GC ")) {
        ++gcThreads;
      }
    }

    if (gcThreads > 20) {
      ThreadAdvisory advisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.PARALLEL_GC_THREADS);
      advisories.add(advisory);
      
      if (this.getHealth().ordinal() < advisory.getHealth().ordinal());
        this.setHealth(advisory.getHealth());
        
      for(ThreadInfo ti: threads) {
        if (ti.getName().toUpperCase().contains("GC ")) {
          ti.addAdvisory(advisory);
          
          if (ti.getHealth().ordinal() < advisory.getHealth().ordinal()) {
            ti.setHealth(advisory.getHealth());
          }
        }
      }
    }
    
    this.addAdvisories(advisories);
  }  
  
  public String getCustomizedOverview() {
    StringBuffer statData = new StringBuffer();
    statData.append("<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Number of Parallel GC Threads </td><td><b><font face=System>");
    statData.append(this.gcThreads);
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Is Finalizer blocked </td><td><b><font face=System>");
    statData.append(this.isFinalizerBlocked);
    statData.append("</b></td></tr>\n\n");            

    statData.append("</b></td></tr>\n\n");    
    return statData.toString();
  }  
  
  
}
