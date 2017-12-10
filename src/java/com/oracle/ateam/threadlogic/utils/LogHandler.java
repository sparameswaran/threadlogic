/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.ateam.threadlogic.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


/**
 *
 * @author saparam
 */
public class LogHandler  {
  
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");
  
  public static ConsoleHandler theConsoleHandler = new ConsoleHandler();
  
  static {
    theConsoleHandler.setFormatter(new LogFormatter());
  }
  
  public static Handler getHandler() {
    return theConsoleHandler;
  }
  
  static public class LogFormatter extends Formatter{ 
  
  
    private static Hashtable<String, String> ht = new Hashtable();
    
    private String calcDate(long millis) {
      SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss z");
      Date resultdate = new Date(millis);
      return date_format.format(resultdate);
    }

    public String format(LogRecord rec) {
      StringBuffer buf = new StringBuffer(1000);
      
      buf.append("<")
      .append(calcDate(rec.getMillis()) + "> <")
      .append(rec.getLevel() + "> <")
      .append(rec.getLoggerName() + "> <")            
      .append(formatMessage(rec) + ">")
      .append(LINE_SEPARATOR);

      if (rec.getThrown() != null) {
        try {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          rec.getThrown().printStackTrace(pw);
          pw.close();
          buf.append(sw.toString());
        } catch (Exception ex) {
          // ignore
        }
      }

      return buf.toString();
    }
  }

}
