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
package com.oracle.ateam.threadlogic;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;

public enum HealthLevel {

  IGNORE, NORMAL, UNKNOWN, WATCH, WARNING, FATAL;

  public String getBackgroundRGBCode() {
    switch (this) {
    case UNKNOWN:
      // pale yellow
      return "rgb(217, 206, 0)";  
    case WATCH:
      // pale orange
      return "rgb(247, 181, 49)";
    case WARNING:
      // orange
      return "rgb(248, 116, 49)";
    case FATAL:
      // red
      return "rgb(255, 60, 60)";
    }
    return "rgb(72,138,199)";
  }

  public Color getColor() {
    switch (this) {
    case UNKNOWN:
      // pale yellow
      return new Color(217, 206, 0);
    case WATCH:
      // pale orange
      return new Color(247, 181, 49);
    case WARNING:
      // orange
      return new Color(248, 116, 49);
    case FATAL:
      // red
      return new Color(255, 60, 60);
    }
    // return blue
    return new Color(72, 138, 199);
  }

};
