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
package com.oracle.ateam.threadlogic.filter;

import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.ThreadLogicElement;

public class HealthLevelFilter extends Filter {
  
  private HealthLevel health = HealthLevel.IGNORE;
  
  public HealthLevelFilter() {
    setName("Minimum Health Level Filter");
    setEnabled(true);
    setGeneralFilter(true);
  }

  public boolean matches(ThreadLogicElement tle) {
    return tle.getHealth().ordinal() >= health.ordinal();
  }

  public HealthLevel getHealth() {
    return health;
  }

  public void setHealth(HealthLevel health) {
    this.health = health;
  }
}
