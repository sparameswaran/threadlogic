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
 * ColoredTable.java
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
 * $Id: ColoredTable.java,v 1.2 2008-01-10 20:36:11 irockel Exp $
 */
package com.oracle.ateam.threadlogic.utils;

import java.awt.Color;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * GrayWhiteTable renders its rows with a sequential color combination of white
 * and gray. Rows with even indicies are rendered white, odd indicies light
 * grey. Note: Do not use GrayWhiteTable for tables with custom renderers such
 * as check boxes. Use JTable instead and modify DefaultTableCellRenderer. Just
 * keep in mind that in order to display a table with more than 1 row colors,
 * you must have 2 separate intances of the renderer, one for each color.
 * 
 * @author irockel
 */
public class ColoredTable extends JTable {

  private DefaultTableCellRenderer whiteRenderer;
  private DefaultTableCellRenderer grayRenderer;
  private DefaultTableCellRenderer greenRenderer;
  private DefaultTableCellRenderer paleGreenRenderer;
  private DefaultTableCellRenderer yellowRenderer;
  private DefaultTableCellRenderer paleYellowRenderer;
  private DefaultTableCellRenderer redRenderer;
  private DefaultTableCellRenderer orangeRenderer;
  private DefaultTableCellRenderer paleOrangeRenderer;
  private DefaultTableCellRenderer siennaRenderer;

  private DefaultTableCellRenderer blueRenderer;

  public ColoredTable() {
    super();
    initRenderers();
  }

  public ColoredTable(TableModel tm) {
    super(tm);
    initRenderers();
  }

  public ColoredTable(Object[][] data, Object[] columns) {
    super(data, columns);
    initRenderers();
  }

  public ColoredTable(int rows, int columns) {
    super(rows, columns);
    initRenderers();
  }

  public void initRenderers() {
    if (whiteRenderer == null) {
      whiteRenderer = new DefaultTableCellRenderer();
      whiteRenderer.setBackground(Color.WHITE);
    }

    if (grayRenderer == null) {
      grayRenderer = new DefaultTableCellRenderer();
      grayRenderer.setBackground(new Color(240, 240, 240));
    }

    if (greenRenderer == null) {
      greenRenderer = new DefaultTableCellRenderer();
      greenRenderer.setBackground(Color.GREEN);
    }

    if (paleGreenRenderer == null) {
      paleGreenRenderer = new DefaultTableCellRenderer();
      paleGreenRenderer.setBackground(new Color(148, 247, 49));
    }
    
    if (siennaRenderer == null) {
      siennaRenderer = new DefaultTableCellRenderer();
      siennaRenderer.setBackground(new Color(248, 116, 49));
    }

    if (paleYellowRenderer == null) {
      paleYellowRenderer = new DefaultTableCellRenderer();
      paleYellowRenderer.setBackground(new Color(217, 206, 0));
    }
    
    if (yellowRenderer == null) {
      yellowRenderer = new DefaultTableCellRenderer();
      yellowRenderer.setBackground(Color.YELLOW);
    }

    if (redRenderer == null) {
      redRenderer = new DefaultTableCellRenderer();
      redRenderer.setBackground(Color.RED);
    }

    if (orangeRenderer == null) {
      orangeRenderer = new DefaultTableCellRenderer();
      orangeRenderer.setBackground(new Color(248, 116, 49));
    }

    if (paleOrangeRenderer == null) {
      paleOrangeRenderer = new DefaultTableCellRenderer();
      paleOrangeRenderer.setBackground(new Color(247, 181, 49));
    }

    if (blueRenderer == null) {
      blueRenderer = new DefaultTableCellRenderer();
      blueRenderer.setBackground(new Color(72, 138, 199));
    }
  }

  /**
   * If row is an even number, getCellRenderer() returns a
   * DefaultTableCellRenderer with white background. For odd rows, this method
   * returns a DefaultTableCellRenderer with a light gray background.
   */
  public TableCellRenderer getCellRenderer(int row, int column) {
    TableModel model = getModel();
    if (model != null) {
      Object o = model.getValueAt(row, column);
      if (o != null) {
        if (o instanceof ThreadDiffsTableModel.STATE_CHANGE) {
          return ((ThreadDiffsTableModel.STATE_CHANGE)o).getRenderer();
        }
        
        String s = o.toString();
        if (s.length() < 10) {

          if (s.equals("WARNING")) {
            return orangeRenderer;
          }

          if (s.equals("FATAL")) {
            return redRenderer;
          }

          if (s.equals("UNKNOWN")) {
            return paleYellowRenderer;
          }
          
          if (s.equals("NORMAL") || s.equals("IGNORE")) {
            return blueRenderer;
          }

          if (s.equals("WATCH")) {
            return paleOrangeRenderer;
          }

          if (s.equals("Progress")) {
            return paleGreenRenderer;
          }

          if (s.equals("No Change")) {
            return yellowRenderer;
          }
        }
      }
    }

    if ((row % 2) == 0) {
      return whiteRenderer;
    } else {
      return grayRenderer;
    }
  }
}
