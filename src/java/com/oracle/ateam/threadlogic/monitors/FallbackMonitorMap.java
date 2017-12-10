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
package com.oracle.ateam.threadlogic.monitors;

public class FallbackMonitorMap extends MonitorMap {

  public FallbackMonitorMap() {
    super();
  }

  public void parseAndAddThread(String line, String threadTitle, String content) {
    if (line == null) {
      return;
    }
    int index = line.indexOf(" lock=");
    if ((index > 0)) {
      int end = line.indexOf(" ", index+3);
      String monitor = line.substring(index + 6, end != -1 ? end : line.length());

      if (line.contains(" BLOCKED on"))  {
        addWaitToMonitor(monitor, threadTitle, content);
        
        /* Sample thread blocked for a lock held by another thread
         * "[ACTIVE] ExecuteThread: '62' for queue: 'weblogic.kernel.Default (self-tuning)'" id=1800 BLOCKED on lock=com.bea.alsb.console.reporting.jmsprovider.ReportManagementFlow@114436 ExeuctionContext=[WLSExecutionContext instance: 482b649aa1df79d3:-71abeda7:13a888f91c6:-8000-000000000001ba5c,0
        mThreadId: 1800
      mOrderIndex: -9223372036854775256
       mSuspended: false
   mCtxContentMap: null
   m_ctxGlobalMap: 0
     mCtxLocalMap: 2
     mInheritable: true
       mListeners: 2
 family:WLSContextFamily instance: 
            mECID: 482b649aa1df79d3:-71abeda7:13a888f91c6:-8000-000000000001ba5c
          mCtxMap: 1
       mGlobalMap: 0
   mPropagateKeys: null
         mLogKeys: null
       mLimitKeys: null
]
     owned by [STUCK] ExecuteThread: '201' for queue: 'weblogic.kernel.Default (self-tuning)' id=2035
    at org.apache.beehive.netui.pageflow.FlowController.execute(FlowController.java:322)
    at org.apache.beehive.netui.pageflow.internal.FlowControllerAction.execute(FlowControllerAction.java:52)

         */
        index = content.indexOf(" owned by ");
        if (index > 0) {
          end = content.indexOf(" id=", index + 10);
          String owner = content.substring(index + 10, end);
          owner = owner.replaceAll("\\[.*\\] ", "").trim();
          
          // we dont know the stack trace of the owner thread at this point.
          addLockToMonitor(monitor, owner, null);
        }
        
      } else if (line.contains(" WAITING on ")) {
        //this.addLockToMonitor(monitor, threadTitle, content);
        this.addSleepToMonitor(monitor, threadTitle, content);
      } 
      
      
    }
  }
}
