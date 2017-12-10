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

import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.ThreadState;
import java.util.ArrayList;
import java.util.regex.Pattern;


/**
 *
 * @author saparam
 */
public class SOAThreadGroup extends CustomizedThreadGroup {  
  
  protected int bpelInvokeThreads, b2bExecutorThreads, bpelEngineThreads,
      soaJMSConsumerThreads;
  
  public SOAThreadGroup(String grpName) {
    super(grpName);
  }
  
  public void runGroupAdvisory() {
    
    for (ThreadInfo ti : this.threads) {

      String content = ti.getContent();
      String threadNameLowerCase = ti.getFilteredName().toLowerCase();

      if (content.contains("b2b.engine.ThreadWorkExecutor"))
        ++this.b2bExecutorThreads;
      else if (threadNameLowerCase.contains("orabpel.engine"))
        ++this.bpelEngineThreads;
      else if (threadNameLowerCase.contains("orabpel.invoke"))
        ++this.bpelInvokeThreads;
      else if (content.contains("adapter.jms.inbound.JmsConsumer.run"))
        ++this.soaJMSConsumerThreads;
      
      if (isIdle(ti)) {
        ThreadAdvisory soaIdleThreadAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.SOA_IDLE_THREADS);
        ti.addAdvisory(soaIdleThreadAdvisory);
        ti.setHealth(HealthLevel.IGNORE);
      }
    }
  }
  
  public boolean isIdle(ThreadInfo ti) {
    
    String threadStack = ti.getContent();
    Pattern soaThreadWaitingPattern = Pattern.compile("java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject");
    
    boolean threadWaitingForAbstractQueuedSynchronizer = soaThreadWaitingPattern.matcher(threadStack).find();
            
    if (threadWaitingForAbstractQueuedSynchronizer           
          && (  
              (ti.getState() == ThreadState.PARKING) 
              || (ti.getState() == ThreadState.TIMED_WAIT) 
             )
        ) {
      int stackDepth = threadStack.split("\n").length;
      return (stackDepth <= ThreadLogicConstants.ACTIVETHREAD_STACKDEPTH);
    } 
    
    return false;
  }
  
  public String getCustomizedOverview() {
    StringBuffer statData = new StringBuffer();
    statData.append("<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Number of B2B Executor Threads </td><td><b><font face=System>");
    statData.append(this.b2bExecutorThreads);

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Number of BPEL Invoke Threads </td><td><b><font face=System>");
    statData.append(this.bpelInvokeThreads);

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Number of BPEL Engine Threads </td><td><b><font face=System>");
    statData.append(this.bpelEngineThreads);

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Number of SOA JMS Consumer Threads </td><td><b><font face=System>");
    statData.append(this.soaJMSConsumerThreads);

    statData.append("</b></td></tr>\n\n");
    return statData.toString();
  }
  
  public static boolean isIdleSOAAdapterPoller(ThreadInfo ti,  ArrayList<ThreadAdvisory> advisories) {
    
    // A SOA Adapter thread is considered idle if its involved in one of the polling activity 
    // and is either in sleep or wait state and has a stack depth of less than 15
	// Derek Kam:  Added additional Adapters
    
    ThreadAdvisory aqAdapterThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.SOA_AQ_ADAPTER_THREAD);
    ThreadAdvisory dbAdapterThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.SOA_DB_ADAPTER_THREAD);
    ThreadAdvisory jmsAdapterThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.SOA_JMS_ADAPTER_THREAD);
    ThreadAdvisory fileAdapterThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.SOA_FILE_ADAPTER_THREAD);
    ThreadAdvisory coherenceAdapterThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.SOA_Coherence_ADAPTER_THREAD);
    ThreadAdvisory ftpAdapterThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.SOA_FTP_ADAPTER_THREAD);
    ThreadAdvisory ldapAdapterThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.SOA_LDAP_ADAPTER_THREAD);
    ThreadAdvisory mqAdapterThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.SOA_MQ_ADAPTER_THREAD);
    ThreadAdvisory msmqAdapterThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.SOA_MSMQ_ADAPTER_THREAD);
    ThreadAdvisory socketAdapterThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.SOA_SOCKET_ADAPTER_THREAD);
    ThreadAdvisory umsAdapterThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.SOA_UMS_ADAPTER_THREAD);
    
    if (! (advisories.contains(aqAdapterThreadAdvisory)
            || advisories.contains(dbAdapterThreadAdvisory)
            || advisories.contains(jmsAdapterThreadAdvisory)
            || advisories.contains(fileAdapterThreadAdvisory)
            || advisories.contains(coherenceAdapterThreadAdvisory)
            || advisories.contains(ftpAdapterThreadAdvisory)
            || advisories.contains(ldapAdapterThreadAdvisory)
            || advisories.contains(mqAdapterThreadAdvisory)
            || advisories.contains(msmqAdapterThreadAdvisory)
            || advisories.contains(socketAdapterThreadAdvisory)
            || advisories.contains(umsAdapterThreadAdvisory)) )
      return false;
    
    String threadStack = ti.getContent();    
    Pattern soaThreadSleepingOrWaitingPattern = Pattern.compile("Thread.sleep|Object.wait");    
    boolean threadInSleepOrWait = soaThreadSleepingOrWaitingPattern.matcher(threadStack).find();
            
    if (threadInSleepOrWait) {
      int stackDepth = threadStack.split("\n").length;
      return (stackDepth <= ThreadLogicConstants.ACTIVETHREAD_STACKDEPTH);
    }
    
    return false;
  }
 
  // Downgrade a SOA adapter thread marked STUCK into Normal if its involved in polling and is idle
  // This needs to happen before the advisories get associated with the thread
  
  public static void resetAdvisoriesBasedOnThread(ThreadInfo threadInfo, ArrayList<ThreadAdvisory> advisoryList) {

    boolean isAnIdleSOAPollerThread = isIdleSOAAdapterPoller(threadInfo, advisoryList);
    boolean isMarkedStuck = threadInfo.markedAsStuck();
    
    if (isAnIdleSOAPollerThread && isMarkedStuck) {
        threadInfo.setHealth(HealthLevel.NORMAL);
        advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.STUCK_PATTERN));
    }
  }
  
}
