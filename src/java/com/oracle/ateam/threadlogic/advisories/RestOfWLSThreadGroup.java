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
public class RestOfWLSThreadGroup extends CustomizedThreadGroup {

  // Try to make sure there are atleast 5 Idle threads in Default Thread pool
  public static final int MIN_IDLE_THREADS = 3;
  
  private boolean isMissingThreads = false;
  private String missingThreadIds = null;

  public RestOfWLSThreadGroup(String grpName) {
    super(grpName);
  }

  public void runGroupAdvisory() {
    int idleThreadCount = 0;

    ThreadAdvisory idleThreadAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLS_IDLE_THREADS);
    ThreadAdvisory listenerThreadAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.LISTENER_THREAD);

    Pattern defaultWLSThreadPoolNamePattern = Pattern.compile(ThreadLogicConstants.WLS_DEFAULT_THREAD_POOL, Pattern.CASE_INSENSITIVE);

    // If the server is up and running,
    // find # of Idle threads in the weblogic.kernel.Default or weblogic.kernel.default thread pool

    // Check if the server is still starting up
    // If so, ignore count of idle threads and return
    ThreadAdvisory wlsStartupThreadAdvisory
            = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLS_SERVICES_STARTUP);

    int noOfListeners = 0;
    for(ThreadInfo ti: threads) {

      if (ti.getAdvisories().contains(wlsStartupThreadAdvisory))
        return;

      if (ti.getAdvisories().contains(idleThreadAdvisory) &&
              defaultWLSThreadPoolNamePattern.matcher(ti.getName()).find() ) {
        idleThreadCount++;
      } else if (ti.getAdvisories().contains(listenerThreadAdvisory) ) {
        noOfListeners++;
      }
    }

    if (noOfListeners < 1) {
      ThreadAdvisory missingListenerAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.LISTENER_MISSING);
      this.addAdvisory(missingListenerAdvisory);
      if (this.getHealth().ordinal() < missingListenerAdvisory.getHealth().ordinal());
        this.setHealth(missingListenerAdvisory.getHealth());
    }

    // Add Thread Starvation advisory and bump up health level for those
    // that are non-idle and in the default thread pool
    if (idleThreadCount < MIN_IDLE_THREADS) {
      ThreadAdvisory threadPoolStarvationAdvisory
              = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLS_DEFAULT_THREAD_POOL_STARVATION);

      HealthLevel threadPoolStarvationHealth = threadPoolStarvationAdvisory.getHealth();

      addAdvisory(threadPoolStarvationAdvisory);
      if (this.getHealth().ordinal() < threadPoolStarvationHealth.ordinal());
        this.setHealth(threadPoolStarvationHealth);

      for(ThreadInfo ti: threads) {
        if (!ti.getAdvisories().contains(idleThreadAdvisory) &&
              defaultWLSThreadPoolNamePattern.matcher(ti.getName()).find() ) {
          ti.addAdvisory(threadPoolStarvationAdvisory);
          if (ti.getHealth().ordinal() < threadPoolStarvationHealth.ordinal()) {
            ti.setHealth(threadPoolStarvationHealth);
          }
        }
      }
    }
  }

  public void addMissingThreadsAdvisory(ThreadAdvisory missingThreadsAdv) {

    if (!this.getAdvisories().contains(missingThreadsAdv))
      addAdvisory(missingThreadsAdv);

    this.setHealth(missingThreadsAdv.getHealth());

    for(ThreadInfo ti: threads) {
      ti.addAdvisory(missingThreadsAdv);
      ti.setHealth(HealthLevel.FATAL);
    }
    
    this.createOverview();
  }

}
