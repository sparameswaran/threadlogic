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
 * Logfile.java
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
 * along with TDA; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * $Id: Logfile.java,v 1.8 2008-02-16 18:00:24 irockel Exp $
 */

package com.oracle.ateam.threadlogic;

import java.util.ArrayList;

import com.oracle.ateam.threadlogic.parsers.DumpParser;

/**
 * root node info object of log file thread dump information
 * 
 * @author irockel
 */
public class Logfile extends AbstractInfo {
  private String content;
  private DumpParser usedParser;
  private String tempFileLocation;  
  private ArrayList<ThreadDumpInfo> tdList = new ArrayList<ThreadDumpInfo>();

  /**
   * Creates a new instance of Logfile
   */
  public Logfile(String value) {
    content = value;
    setName(value);
  }

  /**
   * returns the text content of this node
   */
  public Object getContent() {
    return content;
  }

  public String toString() {
    return ((String) getContent());
  }

  /**
   * return the parser used in this log file
   * 
   * @return used parser, null if no dump was found in log file.
   */
  public DumpParser getUsedParser() {
    return (usedParser);
  }

  /**
   * set the used parser to the passed parser. Method can only be called once
   * during lifetime of object.
   * 
   * @param parser
   *          new parser.
   */
  public void setUsedParser(DumpParser parser) {
    if (usedParser == null) {
      usedParser = parser;
    }
  }
  
  public ArrayList<ThreadDumpInfo> getThreadDumps() {
    return tdList;
  }
  
  public void addThreadDump(ThreadDumpInfo tdi) {
    tdList.add(tdi);
  }
  
  /**
   * @return the tempFileLocation
   */
  public String getTempFileLocation() {
    return tempFileLocation;
  }

  /**
   * @param tempFileLocation the tempFileLocation to set
   */
  public void setTempFileLocation(String tempFileLocation) {
    this.tempFileLocation = tempFileLocation;
  }  
}
