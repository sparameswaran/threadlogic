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
 * $Id: HelpViewer.java,v 1.2 2008-10-05 10:07:09 irockel Exp $
 */
package com.oracle.ateam.threadlogic;

import com.oracle.ateam.threadlogic.utils.Browser;
import com.oracle.ateam.threadlogic.utils.ResourceManager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Enumeration;
import javax.help.HelpSet;
import javax.help.JHelp;
import javax.help.JHelpContentViewer;
import javax.help.JHelpIndexNavigator;
import javax.help.JHelpNavigator;
import javax.help.SwingHelpUtilities;
import javax.help.plaf.basic.BasicContentViewerUI;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;

/**
 * displays the java help.
 * 
 * @author irockel
 */
public class HelpViewer extends BasicContentViewerUI {

  public HelpViewer(JHelpContentViewer x) {
    super(x);
  }

  /**
   * display the java help dialog.
   * 
   * @param owner
   *          the owner frame, used for centering the dialog.
   */
  public static void show(Frame owner) {
    JHelp helpViewer = null;

    SwingHelpUtilities.setContentViewerUI("com.oracle.ateam.threadlogic.HelpViewer");
    try {
      ClassLoader cl = ThreadLocal.class.getClassLoader();
      URL url = HelpSet.findHelpSet(cl, "javahelp/jhelpset.hs");
      helpViewer = new JHelp(new HelpSet(cl, url));

      helpViewer.setToolbarDisplayed(false);
      helpViewer.setCurrentID("general");
    } catch (Exception e) {
    }
    Enumeration eNavigators = helpViewer.getHelpNavigators();
    while (eNavigators.hasMoreElements()) {
      JHelpNavigator nav = (JHelpNavigator) eNavigators.nextElement();
      if (nav instanceof JHelpIndexNavigator) {
        helpViewer.removeHelpNavigator(nav);
      }
    }

    final JDialog helpFrame = new JDialog(owner, ResourceManager.translate("help.contents"));
    try {
      helpFrame.setIconImage(ThreadLogic.createImageIcon("Help.gif").getImage());
    } catch (NoSuchMethodError nsme) {
      // ignore, for 1.4 backward compatibility
    }

    helpFrame.setLayout(new BorderLayout());
    helpFrame.getContentPane().add(helpViewer, BorderLayout.CENTER);
    JButton closeButton = new JButton("Close");
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(closeButton);
    helpFrame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        helpFrame.dispose();
      }
    });
    helpFrame.getRootPane().setDefaultButton(closeButton);
    helpFrame.setSize(new Dimension(900, 700));
    helpFrame.setLocationRelativeTo(owner);
    helpFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    helpFrame.setVisible(true);
  }

  public static javax.swing.plaf.ComponentUI createUI(JComponent x) {
    return new HelpViewer((JHelpContentViewer) x);
  }

  public void hyperlinkUpdate(HyperlinkEvent he) {
    if (he.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
      try {
        URL u = he.getURL();
        if (u.getProtocol().equalsIgnoreCase("mailto") || u.getProtocol().equalsIgnoreCase("http")
            || u.getProtocol().equalsIgnoreCase("ftp")) {
          Browser.open(u.toString());
          return;
        }
      } catch (Throwable t) {
      }
    }
    super.hyperlinkUpdate(he);
  }
}
