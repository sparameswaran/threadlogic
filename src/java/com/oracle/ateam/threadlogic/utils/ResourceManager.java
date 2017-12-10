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

package com.oracle.ateam.threadlogic.utils;

import java.util.ResourceBundle;

/**
 * 
 * @author irockel
 */
public class ResourceManager {
  private static ResourceBundle locale;

  public static String translate(String key) {
    if (locale == null) {
      locale = ResourceBundle.getBundle("com/oracle/ateam/threadlogic/locale");
    }

    return (locale.getString(key));
  }

}
