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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.ateam.threadlogic.advisories;

import java.util.ArrayList;

import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.LockInfo;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.ThreadState;
import java.util.regex.Pattern;

/**
 *
 * @author saparam
 */
public class OSBThreadGroup extends CustomizedThreadGroup {    
  
  // Bump warning levels if more than 15 threads are waiting for Service Callout inside OSB
  public static final int SERVICE_CALLOUT_THRESHOLD = 15;
  
  public OSBThreadGroup(String grpName) {
    super(grpName);
  }
  
  public void runGroupAdvisory() {
    int serviceCalloutBlockedThreads = 0;    
    ThreadAdvisory osbServiceCalloutAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.OSB_WAIT_FOR_SERVICE_CALLOUT);
    ThreadAdvisory semaphoreWaitAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.SEMAPHORE_ACQUIRE);
    ThreadAdvisory osbEjbInboundAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.OSB_EJB_INBOUND);
    ThreadAdvisory osbEjbResponseAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.OSB_WAIT_FOR_EJB_RESPONSE);
    ThreadAdvisory osbBeginTxAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.OSB_TXMGR_BEGINTX);
    ThreadAdvisory osbDerivedCacheBlockedAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.OSB_DERIVED_CACHE);
            
    boolean hasDerivedCacheBlockedAdvisory = false;
    boolean hasTxMgrBlockedAdvisory = false;
            
    for(ThreadInfo ti: threads) { 
      ArrayList<ThreadAdvisory> advisories = ti.getAdvisories();
      if (advisories.contains(osbServiceCalloutAdvisory)) {
        serviceCalloutBlockedThreads++;
      }
      
      if (!hasDerivedCacheBlockedAdvisory) {
        hasDerivedCacheBlockedAdvisory = ti.getAdvisories().contains(osbDerivedCacheBlockedAdvisory);
      }
      
      if (!hasTxMgrBlockedAdvisory) {
        hasTxMgrBlockedAdvisory = ti.getAdvisories().contains(osbBeginTxAdvisory) && ti.isBlockedForLock();
      }
      
      // Change to OSB waiting for Ejb Response advisories if both semaphores and ejb inbound advisories are present
      //Remove the inbound and semaphores blocked advisories
      if (advisories.contains(semaphoreWaitAdvisory) && advisories.contains(osbEjbInboundAdvisory)) {
        advisories.remove(semaphoreWaitAdvisory);
        advisories.remove(osbEjbInboundAdvisory);
        advisories.add(osbEjbResponseAdvisory);
      }
    }
    
    // Bump up the Health Level to WARNING for those threads showing Service Callouts...
    if (serviceCalloutBlockedThreads > SERVICE_CALLOUT_THRESHOLD) {     
      
      //Remove older advisory at lower health level
      this.removeAdvisory(osbServiceCalloutAdvisory);
      
      osbServiceCalloutAdvisory.setHealth(HealthLevel.WARNING);
      this.addAdvisory(osbServiceCalloutAdvisory);
      
      if (this.getHealth().ordinal() < HealthLevel.WARNING.ordinal());
        this.setHealth(HealthLevel.WARNING);
        
      for(ThreadInfo ti: threads) {
        if (ti.getAdvisories().contains(osbServiceCalloutAdvisory)
          && ti.getHealth().ordinal() < HealthLevel.WARNING.ordinal()) {
          
          ti.setHealth(HealthLevel.WARNING);          
        }
      }
    }
    
    // Bump up the Health Level to FATAL 
    // if there are threads that have the derivedCacheBlocked and TxMgrBlocked...
    if (hasDerivedCacheBlockedAdvisory && hasTxMgrBlockedAdvisory) {
       
      osbBeginTxAdvisory.setHealth(HealthLevel.FATAL);
      this.addAdvisory(osbDerivedCacheBlockedAdvisory);
      this.addAdvisory(osbBeginTxAdvisory);
      
      this.setHealth(HealthLevel.FATAL);
        
      for(ThreadInfo ti: threads) {
        if ( (ti.getAdvisories().contains(osbDerivedCacheBlockedAdvisory) 
                || ti.getAdvisories().contains(osbBeginTxAdvisory) )
          && ti.getHealth().ordinal() == HealthLevel.WARNING.ordinal()) {
          
          ti.setHealth(HealthLevel.FATAL);          
        }
      }      
    }
    
  } 
  
  
  
}

