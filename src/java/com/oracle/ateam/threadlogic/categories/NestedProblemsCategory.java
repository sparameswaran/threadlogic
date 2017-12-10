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

public class NestedProblemsCategory extends NestedCategory {

  private String stuck = ".*\"\\[STUCK\\].*";

  public NestedProblemsCategory() {
    super("Potential Problems");
    addFilters();
  }

  public void addFilters() {
    Filter stuckFilter = new Filter("Stuck Threads", stuck, 0, false, false, true);
    addToFilters(stuckFilter);

    Filter waitingOnRemote = new Filter("Waiting For Data From Remote", "socketRead", 2, false, false, true);
    waitingOnRemote
        .setInfo("This could indicate that the remote server is slow processing the request and/or returning a response");
    addToFilters(waitingOnRemote);

    Filter blockedFinalizer = new Filter("Blocked Finalizer", "Finalizer.*\".*blocked,", 0, false, false, true);
    blockedFinalizer
        .setInfo("Finalizer is blocked.  This could potentially cause a memory leak if the monitor is never unlocked");
    addToFilters(blockedFinalizer);

  }
}
