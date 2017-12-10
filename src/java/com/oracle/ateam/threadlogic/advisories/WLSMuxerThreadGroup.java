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
public class WLSMuxerThreadGroup extends CustomizedThreadGroup {    
  
  public WLSMuxerThreadGroup(String grpName) {
    super(grpName);
  }
  
  public void runGroupAdvisory() {
    if (getThreads().size() > 5) {
      ThreadAdvisory muxerThreadsAdvisory = ThreadAdvisory.lookupThreadAdvisory("WebLogicMuxerThreads");
      HealthLevel muxerThreadsHealth = muxerThreadsAdvisory.getHealth();
      
      addAdvisory(muxerThreadsAdvisory);
      if (this.getHealth().ordinal() < muxerThreadsHealth.ordinal());
        this.setHealth(muxerThreadsHealth);
        
      for(ThreadInfo ti: threads) {
        ti.addAdvisory(muxerThreadsAdvisory);
        if (ti.getHealth().ordinal() < muxerThreadsHealth.ordinal()) {
          ti.setHealth(muxerThreadsHealth);
        }
      }      
    }
  } 
  
  public static void resetAdvisoriesBasedOnThread(ThreadInfo threadInfo, ArrayList<ThreadAdvisory> advisoryList) {

    boolean isAtWatchLevel = (threadInfo.getHealth() == HealthLevel.WATCH);
    
    if (isAtWatchLevel && (threadInfo.getState() == ThreadState.BLOCKED)) {
      
      // Ensure the lock is also held by another muxer thread
      // thats the normal behavior
      LockInfo blockedForLock = threadInfo.getBlockedForLock(); 
      if (blockedForLock == null) {
        threadInfo.setHealth(HealthLevel.NORMAL);
        return;
      }
      
      ThreadInfo ownerOfLock = blockedForLock.getLockOwner();
      if (ownerOfLock != null) {

        String blockingThreadName = ownerOfLock.getName().toLowerCase();

        if (blockingThreadName.contains("muxer")) {
          threadInfo.setHealth(HealthLevel.NORMAL);
          return;
        } else {
          advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLSMUXER_PROCESS_SOCKETS));            
          threadInfo.setHealth(HealthLevel.FATAL);
        }
      }

      
    } else if ( (threadInfo.getState() == ThreadState.WAITING)
                || (threadInfo.getState() == ThreadState.PARKING) 
              ) {
              
      /*
        // Check if this is from a IBM JVM
        // IBM JVM makes the poller marks it as CW/Waiting state as its waiting natively in poll
        // If not IBM, add warning about Muxer blocked in a bad state
        if (!threadInfo.isIBMJVM() && !threadInfo.getContent().contains("FdStruct") ) {

          ThreadAdvisory warningAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.MUXER_WAITING);        
          advisoryList.add(warningAdvisory);
          threadInfo.setHealth(warningAdvisory.getHealth());
          advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLSMUXER_PROCESS_SOCKETS));

      } else {
        // If IBM, dont add warning about Muxer blocked in a bad state
        // Also remove WAITING_WHILE_BLOCKING Advisory that might have got added to the Muxer if it was IBM JVM 
        // and the muxer thread appears in Condition Wait while holding lock and blocking other muxer threads
        advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WAITING_WHILE_BLOCKING));    
      }
       * 
       */
      
      // Remove the waiting_while_blocking pattern for muxers in general... as muxers might be doing small waits for fd events
      advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WAITING_WHILE_BLOCKING));    
    }

    // Make sure the Muxer thread not executing or handling requests itself.
    // it should only be dispatching requests to sub-systems instead of handling job itself
    if (threadInfo.getContent().contains("WorkAdapterImpl.run")) {
      ThreadAdvisory warningAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLS_SUBSYSTEM_REQUEST_OVERFLOW);
      advisoryList.add(warningAdvisory);
      threadInfo.setHealth(warningAdvisory.getHealth());
      advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLSMUXER_PROCESS_SOCKETS));
    }
  }
  
  
}
