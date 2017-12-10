/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.ateam.threadlogic.utils;

import java.util.logging.Logger;

/**
 *
 * @author saparam
 */
public class CustomLogger extends Logger{
          
  CustomLogger(String name, String resourceBundleName) {
    super(name.substring(name.lastIndexOf(".") + 1), resourceBundleName);
    setUseParentHandlers(false);
    addHandler(LogHandler.getHandler());    
  }
  
  public static Logger getLogger(String className) {
    Logger logger = Logger.getLogger(className.substring(className.lastIndexOf(".") + 1));
    logger.setUseParentHandlers(false);
    logger.addHandler(LogHandler.getHandler()); 
    return logger;
  }
}
