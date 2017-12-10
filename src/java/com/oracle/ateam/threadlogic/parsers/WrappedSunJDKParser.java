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
 * WrappedSunJDKParser.java
 *
 * This file is part of TDA - Thread Dump Analysis Tool.
 *
 * Foobar is free software; you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with Foobar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.oracle.ateam.threadlogic.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Map;

import com.oracle.ateam.threadlogic.utils.DateMatcher;

public class WrappedSunJDKParser extends HotspotParser {

  /**
   * Creates a new instance of WrappedSunJDKParser: A SunJDKParser reading a lot
   * file created by the Tanuki Service Wrapper.
   */
  public WrappedSunJDKParser(LineNumberReader bis, Map threadStore, int lineCounter, boolean withCurrentTimeStamp,
      int startCounter, DateMatcher dm) {
    super(bis, threadStore, lineCounter, withCurrentTimeStamp, startCounter, dm);
  }

  /**
   * check if the passed logline contains the beginning of a sun jdk thread
   * dump.
   * 
   * @param logLine
   *          the line of the logfile to test
   * @return true, if the start of a sun thread dump is detected.
   */
  public static boolean checkForSupportedThreadDump(String logLine) {
    return logLine.startsWith("INFO   | jvm ") && logLine.trim().indexOf(" | Full thread dump") >= 0;
  }

  protected String getNextLine() throws IOException {
    return getBis().readLine().substring(42);
  }

}
