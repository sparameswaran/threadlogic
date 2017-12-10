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

public class IBMMonitorMap extends MonitorMap {

  public IBMMonitorMap() {
    super();
  }

  public void parseAndAddThread(String line, String threadTitle, String currentThread) {
    if (line == null) {
      return;
    }
    if ((line.indexOf('@') > 0)) {
      String monitor = line.substring(line.indexOf('@'));
      if (line.trim().startsWith("-- Blocked trying") || line.trim().startsWith("- Parking to wait")) {
        addWaitToMonitor(monitor, threadTitle, currentThread);
      } else if (line.trim().startsWith("-- Waiting for notification on")) {
        addSleepToMonitor(monitor, threadTitle, currentThread);
      } else {
        addLockToMonitor(monitor, threadTitle, currentThread);
      }
    }
  }
}
