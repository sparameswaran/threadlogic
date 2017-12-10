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
package com.oracle.ateam.threadlogic.categories;

import com.oracle.ateam.threadlogic.filter.Filter;

public class NestedWebLogicCategory extends NestedCategory {

  private String active = ".*\"\\[ACTIVE\\].*";
  private String standby = ".*\"\\[STANDBY\\].*";
  private String hogging = ".*\"\\[HOGGING\\].*";
  private String stuck = ".*\"\\[STUCK\\].*";

  public NestedWebLogicCategory() {
    super("WebLogic");
    addFilters();
  }

  private void addFilters() {
    // Add default muxer and self-tuning filters
    Filter muxerThreads = new Filter("Muxer Threads", "for queue: 'weblogic.socket.Muxer'", 0, false, false, true);
    Filter defaultThreads = new Filter("Self Tuning Threads", "for queue: 'weblogic.kernel.Default \\(self-tuning\\)'",
        0, false, false, true);
    Filter commonjThreads = new Filter("Commonj WorkManager Threads", "weblogic.work.j2ee.J2EEWorkManager", 0, false,
        false, true);
    Filter waitingOnRemote = new Filter("Waiting For Data From Remote", "socketRead", 2, false, false, true);
    waitingOnRemote
        .setInfo("This could indicate that the remote server is slow processing the request and/or returning a response");

    Filter waitingForRequests = new Filter("Idle Threads Waiting To Process Requests",
        "weblogic.work.ExecuteThread.waitForRequest", 2, false, false, true);
    waitingForRequests
        .setInfo("These are idle Self-Tuning threads that can be ignored.  These threads are waiting for a request to be dispatched to it.");

    addToFilters(muxerThreads);
    addToFilters(defaultThreads);
    addToFilters(commonjThreads);

    // Add filters for self-tuning
    NestedCategory selfTuning = getSubCategory("Self Tuning Threads");
    selfTuning.addToFilters(new Filter("ACTIVE", active, 0, false, false, true));
    NestedCategory active = selfTuning.getSubCategory("ACTIVE");
    active.addToFilters(waitingForRequests);
    active.addToFilters(waitingOnRemote);

    selfTuning.addToFilters(new Filter("STANDBY", standby, 0, false, false, true));
    selfTuning.addToFilters(new Filter("HOGGING", hogging, 0, false, false, true));
    selfTuning.addToFilters(new Filter("STUCK", stuck, 0, false, false, true));

    NestedCategory commonj = getSubCategory("Commonj WorkManager Threads");
    commonj.addToFilters(waitingOnRemote);

  }

}
