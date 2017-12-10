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
package com.oracle.ateam.threadlogic.jedit;

import com.oracle.ateam.threadlogic.ThreadLogic;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

/**
 * popup for the jedit text area
 * 
 * @author irockel
 */
public class PopupMenu extends JPopupMenu implements ActionListener {
  private JEditTextArea ref;
  private ThreadLogic parent;
  private JMenuItem againMenuItem;
  private JMenuItem copyMenuItem;
  private JMenuItem selectNoneMenuItem;

  private String searchString;

  public PopupMenu(JEditTextArea ref, ThreadLogic parent, boolean showSave) {
    JMenuItem menuItem;

    menuItem = new JMenuItem("Goto Line...");
    menuItem.addActionListener(this);
    add(menuItem);
    this.addSeparator();
    menuItem = new JMenuItem("Search...");
    menuItem.addActionListener(this);
    add(menuItem);
    againMenuItem = new JMenuItem("Search again");
    againMenuItem.addActionListener(this);
    againMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
    add(againMenuItem);
    this.addSeparator();
    copyMenuItem = new JMenuItem("Copy to Clipboard");
    copyMenuItem.addActionListener(this);
    copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
    add(copyMenuItem);
    menuItem = new JMenuItem("Select All");
    menuItem.addActionListener(this);
    add(menuItem);
    selectNoneMenuItem = new JMenuItem("Select None");
    selectNoneMenuItem.addActionListener(this);
    add(selectNoneMenuItem);
    if (showSave) {
      this.addSeparator();
      menuItem = new JMenuItem("Save Logfile...");
      menuItem.addActionListener(this);
      add(menuItem);
    }

    this.ref = ref;
    this.parent = parent;
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() instanceof JMenuItem) {
      JMenuItem source = (JMenuItem) (e.getSource());
      if (source.getText().equals("Goto Line...")) {
        gotoLine();
      } else if (source.getText().equals("Search...")) {
        search();
      } else if (source.getText().startsWith("Search again")) {
        search(searchString, ref.getCaretPosition() + 1);
      } else if (source.getText().startsWith("Copy to Clipboard")) {
        ref.copy();
      } else if (source.getText().startsWith("Select All")) {
        ref.selectAll();
      } else if (source.getText().startsWith("Select None")) {
        ref.selectNone();
      } else if (source.getText().startsWith("Save Logfile...")) {
        parent.saveLogFile();
      }
    } else if (e.getSource() instanceof JEditTextArea) {
      // only one key binding
      if (e.getModifiers() > 0) {
        if (ref.getSelectionStart() >= 0) {
          ref.copy();
        }
      } else {
        if (searchString != null) {
          search(searchString, ref.getCaretPosition() + 1);
        }
      }
    }
  }

  private void search(String searchString, int offSet) {
    int searchIndex = ref.getText().indexOf(searchString, offSet);
    if (searchIndex < 0) {
      JOptionPane.showMessageDialog(parent, "Search string not found", "Error", JOptionPane.ERROR_MESSAGE);
    } else {
      ref.setCaretPosition(searchIndex);
    }
  }

  private void gotoLine() {
    String result = JOptionPane.showInputDialog(parent, "Type in line number (between 1-" + ref.getLineCount() + ")");

    if (result != null) {
      int lineNumber = 0;
      try {
        lineNumber = Integer.parseInt(result);
      } catch (NumberFormatException ne) {
        JOptionPane.showMessageDialog(parent, "Invalid line number entered", "Error", JOptionPane.ERROR_MESSAGE);
      }
      if ((lineNumber > 0) && (lineNumber <= ref.getLineCount())) {
        ref.setFirstLine(lineNumber);
      }
    }
  }

  private void search() {
    String result = JOptionPane.showInputDialog(parent, "Type in search string");

    if (result != null) {
      search(result, ref.getCaretPosition());
      searchString = result;
    }
  }

  /**
   * overrides default implementation for "Search Again" enable check.
   */
  public void show(Component invoker, int x, int y) {
    super.show(invoker, x, y);
    againMenuItem.setEnabled(searchString != null);

    String selectedText = ref.getSelectedText();
    copyMenuItem.setEnabled(selectedText != null && selectedText.length() > 0);
    selectNoneMenuItem.setEnabled(copyMenuItem.isEnabled());
  }
}
