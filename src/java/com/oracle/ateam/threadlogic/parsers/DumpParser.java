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
 * DumpParser.java
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
 * $Id: DumpParser.java,v 1.11 2007-11-27 09:42:20 irockel Exp $
 */

package com.oracle.ateam.threadlogic.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * Dump Parser Interface, defines base methods for all dump parsers.
 * 
 * @author irockel
 */
public interface DumpParser {
  public boolean hasMoreDumps();

  public MutableTreeNode parseNext();

  public void close() throws IOException;

  public void findLongRunningThreads(DefaultMutableTreeNode root, Map dumpStore, TreePath[] paths, int minOccurence,
      String regex);

  public void mergeDumps(DefaultMutableTreeNode root, Map dumpStore, TreePath[] dumps, int minOccurence, String regex);

  public boolean isFoundClassHistograms();

  public void parseLoggcFile(InputStream loggcFileStream, DefaultMutableTreeNode root);

  public void setDumpHistogramCounter(int value);

  public interface lineChecker {
    public String getFullDump(String line);

    public String getStackStart(String line);

    public String getAt(String line);

    public String getThreadState(String line);

    public String getLockedOwnable(String line);

    public String getWaitingOn(String line);

    public String getParkingToWait(String line);

    public String getWaitingTo(String line);

    public String getLocked(String line);

    public String getEndOfDump(String line);
    
    //Adding marker to identify exact end of thread dump
    // for integration with ECID/Context Data
    public String getExactEndOfDump(String line);

    public String getLockReleased(String line);

    public String getGCThread(String line);
    
    public String getEndOfTitlePattern(String line);

    public void setFullDumpPattern(String pattern);

    public void setAtPattern(String pattern);

    public void setThreadStatePattern(String pattern);

    public void setLockedOwnablePattern(String pattern);

    public void setWaitingOnPattern(String pattern);

    public void setParkingToWaitPattern(String pattern);

    public void setWaitingToPattern(String pattern);

    public void setLockedPattern(String pattern);

    public void setEndOfDumpPattern(String pattern);    
    
    public void setExactEndOfDumpPattern(String pattern);

    public void setLockReleasedPattern(String pattern);

    public void setGCThreadPattern(String pattern);
    
    public void setEndOfTitlePattern(String pattern);

  }
}
