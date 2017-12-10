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
 * TipOfDay.java
 *
 * This file is part of TDA - Thread Dump Analysis Tool.
 *
 * TDA is free software; you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * TDA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with TDA; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * $Id: TipOfDay.java,v 1.1 2008-09-16 20:46:27 irockel Exp $
 */
package com.oracle.ateam.threadlogic.utils;

import com.oracle.ateam.threadlogic.ThreadLogic;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

/**
 * read random tip of day from tips.properties.
 * 
 * @author irockel
 */
public class TipOfDay {
  private static Properties tips;
  private static int tipsCount = 0;
  private static Random rand = new Random();

  public static String getTipOfDay() {
    if (tips == null) {
      loadTips();
    }
    return (tips.getProperty("tip." + (rand.nextInt(tipsCount))));
  }

  private static void loadTips() {
    tips = new Properties();
    try {
      tips.load(ThreadLogic.class.getResourceAsStream("doc/tips.properties"));
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    tipsCount = Integer.parseInt(tips.getProperty("tips.count"));
  }
}
