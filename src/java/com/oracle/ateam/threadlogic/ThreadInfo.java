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
package com.oracle.ateam.threadlogic;

import java.util.ArrayList;

import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup;
import com.oracle.ateam.threadlogic.utils.CustomLogger;
import java.util.logging.Logger;

/**
 * Info (name, content tuple) for thread dump display tree. Modified by Sabha to
 * extend TDAElement
 * 
 * @author irockel
 */
public class ThreadInfo extends ThreadLogicElement {
  
  private String content;
  private String info;
  private int stackLines;
  private String[] tokens;
  private boolean aLotOfWaiting;
  private int childCount;

  // Added by Sabha
  protected String filteredName;
  protected String nameId;
  protected boolean isBlockedForLock;
  protected boolean isMainThread = false;
  protected boolean isIBMJVM = false;
  
  protected String tGroup;
  protected LockInfo blockedForLock;
  protected ArrayList<LockInfo> ownedLocks = new ArrayList<LockInfo>();
  protected ThreadDumpInfo tdi;
  protected ThreadGroup tg;
  
  // Add support for ECIDs & composie
  private String ecid;
  private String compositeFlowID;
  
  // Add support for ContextData
  private String ctxData;
  
  public static final String CONTEXT_DATA_SEPARATOR = ";;"; 
  
  private static Logger theLogger = CustomLogger.getLogger(ThreadLogic.class.getSimpleName());
  
  
  public ThreadInfo(String name, String info, String content, int stackLines, String[] tableTokens) {
    super(name);
    setFilteredName();
    this.info = info;
    this.content = content;
    this.stackLines = stackLines;
    tokens = tableTokens;
    
    this.isMainThread = this.content.contains(".main(");
      
    this.state = ThreadState.RUNNING;
    if (tokens != null)
      parseState();
    
    setNameId();
    checkForECID();
  }
  
  public ThreadInfo(ThreadInfo copy) {
    super(copy);
    
    setFilteredName();
    this.info = copy.info;
    this.content = copy.content;
    this.stackLines = copy.stackLines;
    tokens = copy.tokens;
    
    this.isMainThread = copy.isMainThread;
      
    this.state = copy.state;
    
    setNameId();
  }

  private ThreadInfo(String name) {
    super(name);
    setFilteredName();
    setNameId();
  }

  public static ThreadInfo createTempThreadInfo(String name) {
    return new ThreadInfo(name);
  }

  protected void parseState() {

    // name is token[0]
    // tid is tokens[1]
    // nid is tokens[2]
    // State is tokens[3]

    String threadState = tokens[3];
    if (threadState != null) {
      threadState = threadState.toLowerCase();

      
    
      // Check against JRockit, IBM, SUN Thread states....
      if (threadState.equals("b") || threadState.contains(" blocked") 
          || ( content.contains(" waiting for lock ") && !content.contains("Object.wait(") && !content.contains("park(") )
          || content.contains("State: BLOCKED") ) {
        this.state = ThreadState.BLOCKED;
        this.health = HealthLevel.WATCH;                
      } else if (threadState.contains("parked") || threadState.equals("p")
          || content.contains("State: WAITING (parking)") || content.contains("State: TIMED_WAITING (parking)")
          || (threadState.contains("waiting") && content.contains(".park("))) {
        this.state = ThreadState.PARKING;
      } else if (threadState.contains(" waiting")|| threadState.contains("native_waiting") || threadState.equals("cw") 
          || threadState.equals("WAITING")
          || content.contains("State: WAITING (on object monitor)")
          || content.contains("State: TIMED_WAITING (on object monitor)")
          || (threadState.contains("waiting") && content.contains("java.lang.Object.wait")))  {
        this.state = ThreadState.WAITING;
      } else if (threadState.contains("sleeping") || threadState.equals("cw") 
          || content.contains("State: TIMED_WAITING (sleeping))")
          || (threadState.contains("timed_waiting") && content.contains("Thread.sleep"))) {
        this.state = ThreadState.TIMED_WAIT;
      }
    }
  }

  public void setParentThreadDump(ThreadDumpInfo tdi) {
    this.tdi = tdi;
  }

  public ThreadDumpInfo getParentThreadDump() {
    return this.tdi;
  }

  public String toString() {
    return getName();
  }

  public String getFilteredName() {
    if (filteredName == null)
      setFilteredName();

    return this.filteredName;
  }

  /*
   * ThreadInfo.name includes everything like state/nid/tid..., so save the key
   * name alone as filteredName that does not have the rest of the labels...
   */
  public void setFilteredName() {
    if (filteredName == null) {
      filteredName = getName().replaceAll("\\[.*\\] ", "").replaceAll("\" .*$", "\"").trim();
    }
  }
  
  public String getNameId() {
    return this.nameId;
  }

  /*
   * Some threads have the same name (as in and only the TID is unique among them
   * As in "EstablishConnection (82.196.48.156:80)" id=5489 idx=0xbb0 tid=14014 prio=5 alive, native_blocked, daemon
   * "EstablishConnection (82.196.48.156:80)" id=5489 idx=0xbb0 tid=14015 prio=5 alive, native_blocked, daemon
   * "EstablishConnection (82.196.48.156:80)" id=5489 idx=0xbb0 tid=14016 prio=5 alive, native_blocked, daemon
   * need to differentiate by thread id also....
   */
  public void setNameId() {

    // name is token[0]
    // tid is tokens[1]
    // nid is tokens[2]
    if ((tokens != null) && tokens.length > 2)
      nameId = getFilteredName() + tokens[1];
    else
      nameId = getFilteredName();    
  }
  
  public String getId() {
    if ( tokens != null && tokens.length > 2)
      return tokens[1];
    return null;
  }
  
  public String getNid() {
    
    if ( tokens != null) {
      // For wlst generated dump, there are no nids, only ids
      String nid = tokens[1];
      
      // if Token[2] is not null, return that
      if ((tokens.length > 2) && (tokens[2] != null))
        nid = tokens[2];
      
      return nid;
    }
    
    return null;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getInfo() {
    return info;
  }

  public void setInfo(String info) {
    this.info = info;
  }

  public int getStackLines() {
    return stackLines;
  }

  public void setStackLines(int stackLines) {
    this.stackLines = stackLines;
  }

  public String[] getTokens() {
    return (tokens);
  }

  public void setALotOfWaiting(boolean b) {
    aLotOfWaiting = b;
  }

  public boolean areALotOfWaiting() {
    return (aLotOfWaiting);
  }

  public void setChildCount(int childCount) {
    this.childCount = childCount;
  }

  public int getChildCount() {
    return childCount;
  }

  // Added by Sabha
  public String getTGroup() {
    return tGroup;
  }

  public void setTGroup(String group) {
    tGroup = group;
  }

  public LockInfo getBlockedForLock() {
    return blockedForLock;
  }

  public void setBlockedForLock(LockInfo blockedForLock) {
    this.blockedForLock = blockedForLock;
    blockedForLock.addBlocker(this);
  }

  public void setBlockedForLock(String lockId) {
     theLogger.finest("setBlockedForLock: '" + lockId +"', for thread:"+
      this.getName());
    LockInfo lock = this.getParentThreadDump().findLock(lockId);
    if (lock == null) {
      lock = new LockInfo(lockId);
      this.getParentThreadDump().addLock(lock);
    }
    this.setBlockedForLock(lock);
  }

  public void addOwnedLocks(LockInfo holdingLock) {
    holdingLock.setLockOwner(this);
    this.ownedLocks.add(holdingLock);
  }

  public void addOwnedLocks(ArrayList<String> holdingLocks) {

    for (String lockId : holdingLocks) {
      LockInfo lock = this.getParentThreadDump().findLock(lockId);
      if (lock == null) {
        lock = new LockInfo(lockId);
        this.getParentThreadDump().addLock(lock);
        lock.setParentThreadDump(this.getParentThreadDump());
      }
      lock.setLockOwner(this);
      this.ownedLocks.add(lock);
    }
  }

  public ArrayList<LockInfo> getOwnedLocks() {
    return ownedLocks;
  }

  public void setOwnedLocks(ArrayList<LockInfo> holdingLocks) {
    this.ownedLocks = holdingLocks;
  }

  public void runAdvisory() {
    theLogger.finest("Running advisory against Thread: " + this.name);
    //Thread.dumpStack();
    this.health = HealthLevel.IGNORE;

    if (this.isBlockedForLock)
      this.health = HealthLevel.WATCH;
    
    ThreadAdvisory.runThreadAdvisory(this);

    this.advisories = ThreadAdvisory.sortByHealth(advisories);
    synchronized (advisories) {
      for (ThreadAdvisory advisory : this.getAdvisories()) {

        // If any of the advisory is at a higher level, set the thread health to
        // the higher level
        theLogger.finest("Advisory:"+ advisory);
        theLogger.finest("ThreadHolder health:"+ this.health);
        if (advisory.getHealth().ordinal() > this.health.ordinal()) {
          this.health = advisory.getHealth();
        }
      }
    }
  }

  public boolean isBlockedForLock() {
    return isBlockedForLock;
  }

  public boolean isMainThread() {
    return isMainThread;
  }
  
  public boolean isIBMJVM() {
    return isIBMJVM;
  }
  
  public void setIsIBMJVM(boolean isIBMJVM) {
    this.isIBMJVM = isIBMJVM;
  }
  
  public boolean equals(Object o) {
    if (o == this)
      return true;

    if (!(o instanceof ThreadInfo))
      return false;

    if (o != null) {
      ThreadInfo cmp = (ThreadInfo) o;
      return this.getFilteredName().equals(cmp.getFilteredName());
    }
    return false;
  }

  /**
   * @return the parent threadgroup
   */
  public ThreadGroup getThreadGroup() {
    return tg;
  }

  /**
   * @param tg the parent threadgroup to set
   */
  public void setThreadGroup(ThreadGroup tg) {
    this.tg = tg;
    this.tGroup = tg.getName();
  }

  /**
   * @return the ecid
   */
  public String getEcid() {
    return ecid;
  }

  /**
   * @param ecid the ecid to set
   */
  public void setEcid(String ecid) {
    this.ecid = ecid;
  }

  public void checkForECID() {
    
    // Sample thread stack for ones generated by WLST
    /*
     * "[ACTIVE] ExecuteThread: '62' for queue: 'weblogic.kernel.Default (self-tuning)'" id=1800 BLOCKED on lock=com.bea.alsb.console.reporting.jmsprovider.ReportManagementFlow@114436 ExeuctionContext=[WLSExecutionContext instance: 482b649aa1df79d3:-71abeda7:13a888f91c6:-8000-000000000001ba5c,0
        mThreadId: 1800
      mOrderIndex: -9223372036854775256
       mSuspended: false
   mCtxContentMap: null
   m_ctxGlobalMap: 0
     mCtxLocalMap: 2
     mInheritable: true
       mListeners: 2
 family:WLSContextFamily instance: 
            mECID: 482b649aa1df79d3:-71abeda7:13a888f91c6:-8000-000000000001ba5c
          mCtxMap: 1
       mGlobalMap: 0
   mPropagateKeys: null
         mLogKeys: null
       mLimitKeys: null
]
     owned by [STUCK] ExecuteThread: '201' for queue: 'weblogic.kernel.Default (self-tuning)' id=2035
    at org.apache.beehive.netui.pageflow.FlowController.execute(FlowController.java:322)
    at org.apache.beehive.netui.pageflow.internal.FlowControllerAction.execute(FlowControllerAction.java:52)

     */
	// Derek Kam: check both ecid and mECID;
	int beginIndex = content.indexOf("ecid");
    if (beginIndex < 0) {
    	beginIndex = content.indexOf("mECID");
    	if (beginIndex < 0) 
    		return;    
    }
    int endIndex = content.indexOf("\n", beginIndex+6);
    setEcid(content.substring(beginIndex + 6, endIndex).trim());
    
    // Derek Kam:Search for flow id - Starting from SOA 12.1.3
	String idField = "oracle.soa.tracking.FlowId";
	beginIndex = content.indexOf(idField);
	if (beginIndex < 0) {
		// Derek Kam: Cannot find flowid, so search for composite id
		idField = "composite_name";
		beginIndex = content.indexOf(idField);
		if (beginIndex < 0)
			return;
	}
     
    endIndex = content.indexOf("\n", beginIndex+6);
    setCompositeFlowID(content.substring(beginIndex + idField.length() + 1, endIndex).trim());
  }

  /**
   * @return the ctxData
   */
  public String getCtxData() {
    return ctxData;
  }

  /**
   * @param ctxData the ctxData to set
   */
  public void setCtxData(String ctxData) {
		this.ctxData = ctxData;

		int beginIndex = ctxData.indexOf("ECID");
		int beginIndex2 = 0;
		if (beginIndex < 0)
			return;

		int endIndex = ctxData.indexOf(ThreadInfo.CONTEXT_DATA_SEPARATOR, beginIndex + 6);
		if (endIndex > 0)
			setEcid(ctxData.substring(beginIndex + 5, endIndex).trim());
		else
			setEcid(ctxData.substring(beginIndex + 5).trim());

		// Derek Kam: Search for flow id - Starting from SOA 12.1.3
		String id = "";
		String name = "";

		String idField = "oracle.soa.tracking.FlowId";
		String nameField = "composite_name";
		beginIndex = ctxData.indexOf(idField);
		if (beginIndex < 0) {
			// Derek Kam: Cannot find flowid, so search for composite id
			idField = "composite_instance_id";
			beginIndex = ctxData.indexOf(idField);
		}

		if (beginIndex > 0){
			endIndex = ctxData.indexOf(ThreadInfo.CONTEXT_DATA_SEPARATOR, beginIndex + 2);
			if (endIndex > 0)
				id = ctxData.substring(beginIndex + idField.length() + 1, endIndex).trim();
			else
				id = ctxData.substring(beginIndex + idField.length() + 1).trim();
		}
		
		// Derek Kam: If found composite id or flow id, continue to find the composite name 
		beginIndex = ctxData.indexOf(nameField);

		if (beginIndex < 0) {
			if (id!="")
				setCompositeFlowID(id);
			return;
		}

		endIndex = ctxData.indexOf(ThreadInfo.CONTEXT_DATA_SEPARATOR, beginIndex + 2);
		if (endIndex > 0)
			name = ctxData.substring(beginIndex + nameField.length() + 1, endIndex).trim();
		else
			name = ctxData.substring(beginIndex + nameField.length() + 1).trim();
		
		if (name != "" && id !=""){
			setCompositeFlowID(name + " : " + id);
		}else if (id !=""){
			setCompositeFlowID(id);
		}else if (name!=""){
			setCompositeFlowID(name);
		}
  }

  /**
   * @return the compositeName
   */
  public String getCompositeFlowID() {
    return compositeFlowID;
  }

  /**
   * @param compositeName the compositeName to set
   */
  public void setCompositeFlowID(String compositeFlowID) {
    this.compositeFlowID = compositeFlowID;
  }
  
  /**
   * @param matchAdvisory ThreadAdvisory to check against
   */
  public boolean hasAdvisory(ThreadAdvisory matchAdvisory) {
    
    if ((matchAdvisory == null) || (advisories.size() ==0))
      return false;
        
    for(ThreadAdvisory advisory : advisories) {
      
      if (advisory.equals(matchAdvisory))
        return true;
    }
    return false;
  }  
  
}
