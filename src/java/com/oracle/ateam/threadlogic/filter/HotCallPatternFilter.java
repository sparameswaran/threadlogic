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
package com.oracle.ateam.threadlogic.filter;

import com.oracle.ateam.threadlogic.ThreadInfo;

import java.util.ArrayList;

/**
 * 
 * @author saparam
 */
public class HotCallPatternFilter extends Filter {

  String callPattern;

  public HotCallPatternFilter(String name, String pattern) {
    setName(name);
    this.callPattern = pattern;

  }

  public boolean matches(ThreadInfo ti, boolean forceEnabled) {
    // Replace all Lock data with empty stuff so we can get match irrespective of lock ids...
    boolean result = ti.getContent().replaceAll("<.*>", "").contains(callPattern);
    return result;
  }

}
