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
 * SearchDialog.java
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
 * $Id: SearchDialog.java,v 1.10 2008-01-09 09:31:35 irockel Exp $
 */

package com.oracle.ateam.threadlogic.dialogs;

import com.oracle.ateam.threadlogic.ThreadLogic;
import com.oracle.ateam.threadlogic.utils.TableSorter;
import com.oracle.ateam.threadlogic.utils.ThreadsTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.Position;
import javax.swing.tree.TreePath;

/**
 * 
 * @author irockel
 */
public class SearchDialog extends JDialog implements ActionListener, ItemListener  {

  private static String SEARCH = "search";
  private static String NEXT = "find next";
  private static String CANCEL = "cancel";

  private JTextField searchField;
  private JCheckBox checkThreadContent;

  private JComponent searchComp;
  
  private boolean searchAgainstThreadContent = false;

  public SearchDialog(JFrame owner, JComponent comp) {
    super(owner, "Search this category... ");
    setLayout(new FlowLayout(FlowLayout.LEFT));

    // Create everything.
    searchField = new JTextField(10);
    searchField.setActionCommand(SEARCH);
    searchField.addActionListener(this);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);    

    JLabel label = new JLabel("Enter search string: ");
    label.setLabelFor(searchField);
    
    checkThreadContent = new JCheckBox("Search against thread content");
    checkThreadContent.setSelected(searchAgainstThreadContent);
    checkThreadContent.addItemListener(this);    

    searchComp = comp;

    JComponent buttonPane = createButtonPanel();

    // Lay out everything.
    JPanel textPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    textPane.add(label);
    textPane.add(searchField);
    textPane.add(checkThreadContent);

    add(textPane);
    add(buttonPane);
  }

  protected JComponent createButtonPanel() {
    JPanel p = new JPanel(new GridLayout(0, 1));
    JButton searchButton = new JButton("Search");

    searchButton.setActionCommand(SEARCH);
    searchButton.addActionListener(this);

    p.add(searchButton);

    JButton nextButton = new JButton("Find Next");
    nextButton.setActionCommand(NEXT);
    nextButton.addActionListener(this);
    p.add(nextButton);
    
    return p;
  }

  /** Listens to the check boxes. */
  public void itemStateChanged(ItemEvent e) {
      //Check if it was selected or deselected.
      searchAgainstThreadContent = (e.getStateChange() == ItemEvent.SELECTED);
  }
    
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();

    if (SEARCH.equals(cmd) || NEXT.contains((cmd))) {
      if (searchComp instanceof JTree) {
        TreePath searchPath = ((JTree) searchComp).getNextMatch(searchField.getText(), 0, Position.Bias.Forward);

        if (searchPath != null) {
          ((JTree) searchComp).setSelectionPath(searchPath);
          Rectangle view = ((JTree) searchComp).getPathBounds(searchPath);
          ((JViewport) searchComp.getParent()).scrollRectToVisible(view);
          dispose();
          searchComp.requestFocusInWindow();
        } else {
          JOptionPane.showMessageDialog(getOwner(), searchField.getText() + " not found!", "Search Error",
              JOptionPane.ERROR_MESSAGE);
          resetFocus();
        }
      } else if (searchComp instanceof JTable) {
        ThreadsTableModel ttm = (ThreadsTableModel) ((TableSorter) ((JTable) searchComp).getModel()).getTableModel();
        
        int row = -1;
        int rowOffset = (NEXT.contains(cmd))?1:0;
        
        if (!searchAgainstThreadContent)
          row = ttm.searchRowWithName(((JTable) searchComp).getSelectedRow() + rowOffset, searchField.getText());
        else
          row = ttm.searchRowWithContent(((JTable) searchComp).getSelectedRow() + rowOffset, searchField.getText());
        
        ((JTable) searchComp).getSelectionModel().setSelectionInterval(row, row);
      }
    }
  }

  // Must be called from the event-dispatching thread.
  protected void resetFocus() {
    searchField.requestFocusInWindow();
  }

  public void reset() {
  }
  

  public String getSearchText() {
    if (this.searchField != null)
      return this.searchField.getText();
    
    return null;
  }
  
}
