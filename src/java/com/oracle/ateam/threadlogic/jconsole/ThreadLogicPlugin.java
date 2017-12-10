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
 * ThreadLogicPlugin.java
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
 * $Id: ThreadLogicPlugin.java,v 1.1 2007-12-08 09:58:34 irockel Exp $
 */
package com.oracle.ateam.threadlogic.jconsole;

import com.oracle.ateam.threadlogic.ThreadLogic;
import com.oracle.ateam.threadlogic.jconsole.MBeanDumper;
import com.sun.tools.jconsole.JConsoleContext;
import com.sun.tools.jconsole.JConsoleContext.ConnectionState;
import com.sun.tools.jconsole.JConsolePlugin;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.SwingWorker;

/**
 * The ThreadLogicPlugin capsulates ThreadLogic to be displayed in jconsole.
 */
public class ThreadLogicPlugin extends JConsolePlugin implements PropertyChangeListener {

  private MBeanDumper mBeanDumper;
  private ThreadLogic threadlogic = null;
  private Map tabs = null;

  public ThreadLogicPlugin() {
    // register itself as a listener
    addContextPropertyChangeListener(this);
  }

  /*
   * Returns a Thread Dumps tab to be added in JConsole.
   */
  public synchronized Map getTabs() {
    if (tabs == null) {
      try {
        mBeanDumper = new MBeanDumper(getContext().getMBeanServerConnection());
        threadlogic = new ThreadLogic(false, mBeanDumper);

        threadlogic.init(true, false);
        tabs = new LinkedHashMap();
        tabs.put("ThreadLogic", threadlogic);
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
    return tabs;
  }

  /*
   * Returns a SwingWorker which is responsible for updating the TDA tab.
   */
  public SwingWorker newSwingWorker() {
    return (new Worker(threadlogic));
  }

  /** 
   * SwingWorker responsible for updating the GUI
   */
  class Worker extends SwingWorker {

    private ThreadLogic threadLogic;

    Worker(ThreadLogic threadLogic) {
      this.threadLogic = threadLogic;
    }

    protected void done() {
      // nothing to do atm
    }

    protected Object doInBackground() throws Exception {
      // nothing to do atm
      return null;
    }
  }

  /*
   * Property listener to reset the MBeanServerConnection
   * at reconnection time.
   */
  public void propertyChange(PropertyChangeEvent ev) {
    String prop = ev.getPropertyName();
    if (JConsoleContext.CONNECTION_STATE_PROPERTY.equals(prop)) {
      ConnectionState newState = (ConnectionState) ev.getNewValue();

      /* 
      JConsole supports disconnection and reconnection
      The MBeanServerConnection will become invalid when
      disconnected. Need to use the new MBeanServerConnection object
      created at reconnection time. 
       */
      if (newState == ConnectionState.CONNECTED && threadlogic != null) {
        mBeanDumper.setMBeanServerConnection(getContext().getMBeanServerConnection());
      }
    }
  }
}
