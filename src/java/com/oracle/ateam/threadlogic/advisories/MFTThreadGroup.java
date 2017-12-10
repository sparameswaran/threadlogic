package com.oracle.ateam.threadlogic.advisories;

import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.ThreadState;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * 
 * @author Derek Kam
 */
public class MFTThreadGroup extends CustomizedThreadGroup {
	protected int mftEngineThreads;

	public MFTThreadGroup(String grpName) {
		super(grpName);
	}

	public void runGroupAdvisory() {

		for (ThreadInfo ti : this.threads) {

			String content = ti.getContent();
			String threadNameLowerCase = ti.getFilteredName().toLowerCase();

			if (content.contains("mft.engine.EngineServiceImpl"))
				++this.mftEngineThreads;

			if (isIdle(ti)) {
				ThreadAdvisory mftIdleThreadAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.MFT_IDLE_THREADS);
				ti.addAdvisory(mftIdleThreadAdvisory);
				ti.setHealth(HealthLevel.IGNORE);
			}
		}

	}

	public boolean isIdle(ThreadInfo ti) {

		String threadStack = ti.getContent();
		Pattern mftThreadStartProgressMonitorUpdater = Pattern.compile("oracle.tip.mft.init.ThreadWorkExecutor.startProgressMonitorUpdater");

		boolean threadStartProgressMonitorUpdater = mftThreadStartProgressMonitorUpdater.matcher(threadStack).find();

		if (threadStartProgressMonitorUpdater && ((ti.getState() == ThreadState.PARKING) || (ti.getState() == ThreadState.TIMED_WAIT || (ti.getState() == ThreadState.RUNNING)))) {
			int stackDepth = threadStack.split("\n").length;
			return (stackDepth <= ThreadLogicConstants.ACTIVETHREAD_STACKDEPTH);
		}

		return false;
	}
	
	  // Downgrade a MFT thread marked STUCK into Normal if it is idle
	  // This needs to happen before the advisories get associated with the thread
	  
	  public static void resetAdvisoriesBasedOnThread(ThreadInfo threadInfo, ArrayList<ThreadAdvisory> advisoryList) {

	    boolean isAnIdleMFTPollerThread = isIdleMFT(threadInfo, advisoryList);
	    boolean isMarkedStuck = threadInfo.markedAsStuck();
	    
	    if (isAnIdleMFTPollerThread && isMarkedStuck) {
	        threadInfo.setHealth(HealthLevel.NORMAL);
	        advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.STUCK_PATTERN));
	    }
	  }	
	  
	  public static boolean isIdleMFT(ThreadInfo ti,  ArrayList<ThreadAdvisory> advisories) {
		    
		    // A MFT thread is considered idle if in sleep or wait state and has a stack depth of less than 15
		    
		    ThreadAdvisory mftThreadAdvisory = ThreadAdvisory.lookupThreadAdvisoryByName(ThreadLogicConstants.MFT_THREAD);

		    
		    if (! (advisories.contains(mftThreadAdvisory)))
		      return false;
		    
		    String threadStack = ti.getContent();    
		    Pattern mftThreadSleepingOrWaitingPattern = Pattern.compile("Thread.sleep|Object.wait|sleeping");    
		    boolean threadInSleepOrWait = mftThreadSleepingOrWaitingPattern.matcher(threadStack).find();
		            
		    if (threadInSleepOrWait) {
		      int stackDepth = threadStack.split("\n").length;
		      return (stackDepth <= ThreadLogicConstants.ACTIVETHREAD_STACKDEPTH);
		    }
		    
		    return false;
		  }	  
	  
	  public String getCustomizedOverview() {
		    StringBuffer statData = new StringBuffer();
		    statData.append("<tr bgcolor=\"#dddddd\"><td><font face=System "
		        + ">Number of MFT Engine Threads </td><td><b><font face=System>");
		    statData.append(this.mftEngineThreads);
		    statData.append("</b></td></tr>\n\n");
		    return statData.toString();
		  }
}
