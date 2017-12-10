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
 * DumpParserFactory.java
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
 * $Id: DumpParserFactory.java,v 1.11 2008-02-14 14:36:08 irockel Exp $
 */

package com.oracle.ateam.threadlogic.parsers;

import com.oracle.ateam.threadlogic.utils.DateMatcher;
import com.oracle.ateam.threadlogic.utils.PrefManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Map;

/**
 * Factory for the dump parsers.
 * 
 * @author irockel
 */
public class DumpParserFactory {
  private static DumpParserFactory instance = null;

  /**
   * singleton private constructor
   */
  private DumpParserFactory() {
  }

  /**
   * get the singleton instance of the factory
   * 
   * @return singleton instance
   */
  public static DumpParserFactory get() {
    if (instance == null) {
      instance = new DumpParserFactory();
    }

    return (instance);
  }

  /**
   * parses the given logfile for thread dumps and return a proper jdk parser
   * (either for Sun VM's or for JRockit/Bea VM's) and initializes the
   * DumpParser with the stream.
   * 
   * @param dumpFileStream
   *          the file stream to use for dump parsing.
   * @param threadStore
   *          the map to store the found thread dumps.
   * @param withCurrentTimeStamp
   *          only used by SunJDKParser for running in JConsole-Plugin-Mode, it
   *          then uses the current time stamp instead of a parsed one.
   * @return a proper dump parser for the given log file, null if no proper
   *         parser was found.
   */
  public DumpParser getDumpParserForLogfile(InputStream dumpFileStream, Map threadStore, boolean withCurrentTimeStamp,
      int startCounter) {
	  LineNumberReader bis = null;
    int readAheadLimit = PrefManager.get().getStreamResetBuffer();
    DumpParser currentDumpParser = null;

    try {
      bis = new LineNumberReader(new InputStreamReader(dumpFileStream));

      // reset current dump parser
      DateMatcher dm = new DateMatcher();
      DateMatcher lastSavedDm = dm;
      boolean foundDate = false;
      String dateEntry = "";
      while (bis.ready() && (currentDumpParser == null)) {
        bis.mark(readAheadLimit);
        String line = bis.readLine();
        dm.checkForDateMatch(line);          
        if (dm.isDefaultMatches()) {
          dateEntry = line;
          foundDate = true;
          
          // Save the very last date entry before we hit the Thread Dump Markers
          lastSavedDm = dm;
        }
        
        if (line.trim().equals(""))
          continue;
        
        if (WrappedSunJDKParser.checkForSupportedThreadDump(line)) {
          currentDumpParser = new WrappedSunJDKParser(bis, threadStore, bis.getLineNumber(), withCurrentTimeStamp,
              startCounter, lastSavedDm);
//        } else if (HotspotParser.checkForSupportedThreadDump(line)) {
//          currentDumpParser = new HotspotParser(bis, threadStore, bis.getLineNumber(), withCurrentTimeStamp, startCounter, lastSavedDm);          
        } else if (JrockitParser.checkForSupportedThreadDump(line) || HotspotParser.checkForSupportedThreadDump(line)) {
        	// Derek Kam: Need to handle thread dump generated using
			// WLST for 12c
        	bis.reset();
			while (bis.ready() && (currentDumpParser == null)) {
				String line2 = bis.readLine();
				if (line2.trim().indexOf("Java HotSpot") >= 0) {
					currentDumpParser = new HotspotParser(bis, threadStore, bis.getLineNumber(), withCurrentTimeStamp, startCounter, lastSavedDm);
				} else if (line2.trim().indexOf("Oracle JRockit") >= 0) {
					currentDumpParser = new JrockitParser(bis, threadStore, bis.getLineNumber(), lastSavedDm);
				}					
			}
        } else if (IBMJDKParser.checkForSupportedThreadDump(line)) {
          currentDumpParser = new IBMJDKParser(bis, threadStore, bis.getLineNumber(), withCurrentTimeStamp, startCounter, lastSavedDm);
        } else {
          int supportedJvmType = FallbackParser.checkForSupportedThreadDump(line);
          if (supportedJvmType < 0)
            continue;
                
          // Found some sort of match against the FallbackParser
            currentDumpParser = new FallbackParser(bis, threadStore, bis.getLineNumber(), withCurrentTimeStamp, startCounter, lastSavedDm, supportedJvmType);

        }
      }
      
      if ((currentDumpParser != null) && (bis != null)) {
        bis.reset();
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return currentDumpParser;
  }
}
