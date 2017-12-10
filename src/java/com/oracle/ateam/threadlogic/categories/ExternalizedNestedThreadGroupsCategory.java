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
package com.oracle.ateam.threadlogic.categories;


import com.oracle.ateam.threadlogic.filter.*;
import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.ThreadLogic;
import com.oracle.ateam.threadlogic.advisories.ThreadLogicConstants;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.advisories.RestOfWLSThreadGroup;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup;
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroupFactory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup.HotCallPattern;
import com.oracle.ateam.threadlogic.utils.CustomLogger;
import com.oracle.ateam.threadlogic.xml.ComplexGroup;
import com.oracle.ateam.threadlogic.xml.GroupsDefnParser;
import com.oracle.ateam.threadlogic.xml.SimpleGroup;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;

public class ExternalizedNestedThreadGroupsCategory extends NestedCategory {

  private Category threads;
  private String overview = null;
  private ThreadGroup unknownThreadGroup;
  
  private CompositeFilter wlsCompositeFilter;
  private CompositeFilter nonwlsCompositeFilter;
  private CompositeFilter unknownCompositeFilter;
  private NestedCategory nestedWLSCategory, nestedNonWLSCategory;
  
  private LinkedList<ThreadInfo> threadLinkedList = new LinkedList<ThreadInfo>();
  private ArrayList<ThreadGroup> threadGroupList = new ArrayList<ThreadGroup>();
  private ArrayList<ThreadGroup> wlsThreadGroupList = new ArrayList<ThreadGroup>();
  private ArrayList<ThreadGroup> nonWlsThreadGroupList = new ArrayList<ThreadGroup>();
   
  
  private Filter ldapThreadsFilter, muxerThreadsFilter, aqAdapterThreadsFilter;
  
  private ArrayList<Filter> allWLSFilterList, allNonWLSFilterList;
  private static ArrayList<Filter> allNonWLSStaticFilterList, allWLSStaticFilterList;
  
  private Filter wlsJMSFilter1 = new Filter("WLS JMS", "(weblogic.jms)|(weblogic.messaging)", 2, false, false, true);
  private Filter wlsJMSFilter2 = new Filter("WLS JMS", "JmsDispatcher", 0, false, false, true);
  
  private static Filter allWLSThreadStackFilter, allWLSThreadNameFilter;
  
  private static Logger theLogger = CustomLogger.getLogger("ThreadGroupsCategory");
  
  public static String DICTIONARY_KEYS;
  public static String THREADTYPEMAPPER_KEYS;
  public static String PATH_SEPARATOR = "|";
  public static String GROUPDEFS_EXT_DIRECTORY = "threadlogic.groups";
  public static final Hashtable<String, Filter> allKnownFilterMap = new Hashtable<String, Filter>();
  public static String wlsThreadStackPattern, wlsThreadNamePattern;
  
  private int totalWlsDefaultExecuteThreads, maxWlsDefaultExecuteThreadId = -1;
  private int[] wlsDefaultExecuteThreadIds = new int[800];
  private static final String REST_OF_WLS = "Rest of WLS";
  
  static {
    init();
  }

  // Cache the Group Definitions and clone the saved filters...
  // instead of reading each time... for each TD 
  
  private static void init() {
    createExternalFilterList();
    if (allWLSStaticFilterList == null || allNonWLSStaticFilterList == null) {
      allWLSStaticFilterList = createInternalFilterList(ThreadLogicConstants.WLS_THREADGROUP_DEFN_XML);
      allNonWLSStaticFilterList = createInternalFilterList(ThreadLogicConstants.NONWLS_THREADGROUP_DEFN_XML);
    }
    
    allWLSThreadStackFilter = new Filter("WLS Stack", wlsThreadStackPattern, 2, false, false, true);  
    allWLSThreadNameFilter = new Filter("WLS Name", wlsThreadNamePattern, 0, false, false, true);
    
    theLogger.finest("WLS Thread Stack Pattern: " + wlsThreadStackPattern);
    theLogger.finest("WLS Thread Name Pattern: " + wlsThreadNamePattern);
  }
  
  /**
   * This method is expected to find two externally defined Group Defns. 
   * One should be for WLS and other for Non-WLS
   */
 
  private static void createExternalFilterList() {
    
    String externalGroupDefnDirectory = System.getProperty(GROUPDEFS_EXT_DIRECTORY, "groupsdef");
    File folder = new File(externalGroupDefnDirectory);
    if (folder.exists()) {              
      theLogger.info("\n\nAttempting to load Groups Defn files from directory: " + externalGroupDefnDirectory);
      theLogger.warning("Alert!! There can only be two files - WLSGroups.xml and NonWLSGroups.xml files within the above directory");
      
      File[] listOfFiles = folder.listFiles();
      for(File file: listOfFiles) {
        try {        
          theLogger.info("Attempting to load GroupsDefn from external resource: " + file.getAbsolutePath());
          BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
          
          boolean isWLSGroup = !file.getName().toLowerCase().contains("nonwls");
          theLogger.info("Parsing file - " + file.getName() + " as a WLS Group Definition file??:" + isWLSGroup);
          
          ArrayList<Filter> filterList = parseFilterList(bis, isWLSGroup);    
          if (filterList.size() > 0) {
            if (isWLSGroup)
              allWLSStaticFilterList = filterList;
            else 
              allNonWLSStaticFilterList = filterList;
          }
        } catch(Exception ioe) {
          theLogger.warning("ERROR!! Problem in reading Group Defn from external file: " + ioe.getMessage());
          ioe.printStackTrace();
        }
      }        
    }
    
    return;
  }
  
  private static ArrayList<Filter> createInternalFilterList(String groupsDefnXml) { 
    
    ClassLoader cl = ThreadLogicConstants.class.getClassLoader();
    theLogger.finest("\n\nAttempting to load GroupsDefn from packaged threadlogic jar: " + groupsDefnXml);
    boolean isWLSGroup = !(groupsDefnXml.toLowerCase().contains("nonwls"));
    return parseFilterList(cl.getResourceAsStream(groupsDefnXml), isWLSGroup);    
  }
  
      
  private static ArrayList<Filter> parseFilterList(InputStream is, boolean isWLSGroup) { 
    GroupsDefnParser groupsDefnParser = null;
    ArrayList<Filter> filterArr = new ArrayList<Filter>();
    
    try {           
      groupsDefnParser = new GroupsDefnParser(is);
      groupsDefnParser.run();
      ArrayList<SimpleGroup> simpleGrpList = groupsDefnParser.getSimpleGrpList();
      ArrayList<ComplexGroup> complexGrpList = groupsDefnParser.getComplexGrpList();

      boolean empty = true;
      StringBuffer sbufStack = new StringBuffer(100);
      StringBuffer sbufName = new StringBuffer(100);

      sbufStack.append("(weblogic)|(Weblogic)");
      sbufName.append("(weblogic)");

      for (SimpleGroup smpGrp : simpleGrpList) {
        generateSimpleFilter(smpGrp, filterArr);
        boolean againstStack = smpGrp.getMatchLocation().equals("stack");
        ArrayList<String> patternList = smpGrp.getPatternList();
        for(String pattern: patternList) {
         if (againstStack) {
            sbufStack.append("|(" + pattern + ")");
          } else {
            sbufName.append("|(" + pattern + ")");
          }                
        }
      }

      for (ComplexGroup cmplxGrp : complexGrpList) {
        generateCompositeFilter(cmplxGrp, filterArr);            
      }

      if (isWLSGroup) {
        wlsThreadStackPattern = sbufStack.toString();
        wlsThreadNamePattern = sbufName.toString();
      }

    } catch (Exception e) {
      theLogger.warning("ERROR!! Unable to load or parse the Group Definition Resource:" + e.getMessage());
      e.printStackTrace();
    }
    return filterArr;
  }

  private static void generateSimpleFilter(SimpleGroup smpGrp, ArrayList<Filter> filterList) {

    String filterName = smpGrp.getName();
    ArrayList<String> patternList = smpGrp.getPatternList();

    String pattern = "";
    int count = patternList.size();
    if (count <= 0) {
      return;
    }

    if (count == 1) {

      pattern = patternList.get(0);

    } else if (count > 1) {

      StringBuffer sbuf = new StringBuffer("(" + patternList.get(0) + ")");
      for (int i = 1; i < count; i++) {
        sbuf.append("|(" + patternList.get(i) + ")");
      }
      pattern = sbuf.toString();
    }

    int filterRuleToApply = Filter.HAS_IN_STACK_RULE;
    if (smpGrp.getMatchLocation().equals("name")) {
      filterRuleToApply = Filter.HAS_IN_TITLE_RULE;
    }

    Filter simpleFilter = new Filter(filterName, pattern, filterRuleToApply, false, false, smpGrp.isInclusion());
    simpleFilter.setExcludedAdvisories(smpGrp.getExcludedAdvisories());
    simpleFilter.setInfo(filterName);

    
    if (allKnownFilterMap.containsKey(filterName)) {
       theLogger.warning("Group Definition already exists:" + filterName + ", use different name or update existing Group Defintion");       
    } else {
      allKnownFilterMap.put(filterName, simpleFilter);
    }

    if (smpGrp.isVisible()) {
      filterList.add(simpleFilter);
    }

    return;
  }

  private static void generateCompositeFilter(ComplexGroup cmplxGrp, ArrayList<Filter> filterList) {
    String filterName = cmplxGrp.getName();

    CompositeFilter compositeFilter = new CompositeFilter(filterName);
    compositeFilter.setExcludedAdvisories(cmplxGrp.getExcludedAdvisories());
    compositeFilter.setInfo(filterName);

    for (String simpleGrpKey : cmplxGrp.getInclusionList()) {
      Filter simpleFilter = allKnownFilterMap.get(simpleGrpKey);
      if (simpleFilter == null) {
        theLogger.warning("ERROR: Simple Group referred by name:" + simpleGrpKey + " not declared previously or name mismatch!!, Fix the error");
        Thread.dumpStack();
        continue;
      }

      compositeFilter.addFilter(simpleFilter, true);
    }

    for (String simpleGrpKey : cmplxGrp.getExclusionList()) {
      Filter simpleFilter = allKnownFilterMap.get(simpleGrpKey);
      if (simpleFilter == null) {
        theLogger.warning("ERROR: Simple Group referred by name:" + simpleGrpKey + " not declared previously or name mismatch!!, Fix the error");
        Thread.dumpStack();
        continue;
      }

      compositeFilter.addFilter(simpleFilter, false);
    }

    allKnownFilterMap.put(filterName, compositeFilter);

    if (cmplxGrp.isVisible()) {
      filterList.add(compositeFilter);
    }

    return;
  }

  private void cloneDefinedFilters() {

    allWLSFilterList = new ArrayList<Filter>();
    allNonWLSFilterList = new ArrayList<Filter>();

    for (Filter filter : allNonWLSStaticFilterList) {
      allNonWLSFilterList.add(filter);
    }

    for (Filter filter : allWLSStaticFilterList) {
      allWLSFilterList.add(filter);
    }
  }

  public ExternalizedNestedThreadGroupsCategory() {
    super("Thread Groups");
    cloneDefinedFilters();
  }

  public Category getThreads() {
    return threads;
  }

  public void setThreads(Category threads) {
    this.threads = threads;
    for (int i = 0; i < threads.getNodeCount(); i++) {
      ThreadInfo ti = (ThreadInfo) ((DefaultMutableTreeNode) threads.getNodeAt(i)).getUserObject();
      threadLinkedList.add(ti);
    }

    addFilters();

    // Sort the thread groups and nested threads by health
    this.threadGroupList = ThreadGroup.sortByHealth(this.threadGroupList);
  }

  public Collection<ThreadGroup> getThreadGroups() {
    return this.threadGroupList;
  }

  public Collection<ThreadGroup> getWLSThreadGroups() {
    return this.wlsThreadGroupList;
  }

  public Collection<ThreadGroup> getNonWLSThreadGroups() {
    return this.nonWlsThreadGroupList;
  }

  public NestedCategory getWLSThreadsCategory() {
    return nestedWLSCategory;
  }

  public NestedCategory getNonWLSThreadsCategory() {
    return nestedNonWLSCategory;
  }

  private void createNonWLSFilterCategories() {

    nonwlsCompositeFilter = new CompositeFilter("Non-WLS Thread Groups");
    nonwlsCompositeFilter.setInfo("Non-WebLogic Thread Groups");

    // Exclude all wls related threads for it
    nonwlsCompositeFilter.addFilter(allWLSThreadStackFilter, false);
    nonwlsCompositeFilter.addFilter(allWLSThreadNameFilter, false);

    addToFilters(nonwlsCompositeFilter);

    nestedNonWLSCategory = getSubCategory(nonwlsCompositeFilter.getName());

    addUnknownThreadGroupFilter();

    for (Filter filter : allNonWLSFilterList) {
      nestedNonWLSCategory.addToFilters(filter);
    }
  }

  private void addUnknownThreadGroupFilter() {

    unknownCompositeFilter = new CompositeFilter("Unknown or Custom");
    unknownThreadGroup = ThreadGroupFactory.createThreadGroup(unknownCompositeFilter.getName());
    threadGroupList.add(unknownThreadGroup);

    for (Filter filter : allNonWLSFilterList) {
      unknownCompositeFilter.addFilter(filter, false);
    }

    for (Filter filter : allWLSFilterList) {
      unknownCompositeFilter.addFilter(filter, false);
    }

    // Add the unknownCompositeFilter to the allNonWLSFilterList
    allNonWLSFilterList.add(unknownCompositeFilter);
  }

  private void createWLSFilterCategories() {

    wlsCompositeFilter = new CompositeFilter("WLS Thread Groups");
    wlsCompositeFilter.setInfo("WebLogic Thread Groups");

    // Include all wls related threads for it
    wlsCompositeFilter.addFilter(allWLSThreadStackFilter, true);
    wlsCompositeFilter.addFilter(allWLSThreadNameFilter, true);

    addToFilters(wlsCompositeFilter);

    nestedWLSCategory = getSubCategory(wlsCompositeFilter.getName());

    // Create a new filter for captuing just the wls & wls jms threads that dont fall under any known wls thread groups
    CompositeFilter wlsJMSThreadsFilter = new CompositeFilter("WLS JMS");
    wlsJMSThreadsFilter.addFilter(wlsJMSFilter1, true);
    wlsJMSThreadsFilter.addFilter(wlsJMSFilter2, true);
    nestedWLSCategory.addToFilters(wlsJMSThreadsFilter);

    CompositeFilter wlsThreadsFilter = new CompositeFilter(REST_OF_WLS);
    wlsThreadsFilter.addFilter(allWLSThreadStackFilter, true);
    wlsThreadsFilter.addFilter(allWLSThreadNameFilter, true);
    
    // Exclude wls jms from pure wls related group
    wlsThreadsFilter.addFilter(wlsJMSFilter1, false);
    wlsThreadsFilter.addFilter(wlsJMSFilter2, false);
    
    nestedWLSCategory.addToFilters(wlsThreadsFilter);

    for (Filter filter : allWLSFilterList) {
      nestedWLSCategory.addToFilters(filter);
      wlsThreadsFilter.addFilter(filter, false);
      wlsJMSThreadsFilter.addFilter(filter, false);
    }

    allWLSFilterList.add(wlsJMSThreadsFilter);
    allWLSFilterList.add(wlsThreadsFilter);
  }

  private void addFilters() {

    createWLSFilterCategories();
    createNonWLSFilterCategories();

    // Create references to the Muxer, AQ Adapter and LDAP Filters as they are referred for Exclusion for the nested Filter for Socket Read
    for (Filter filter : allKnownFilterMap.values()) {
      if (filter instanceof CompositeFilter) {
        continue;
      }

      String filterName = filter.getName().toLowerCase();
      if (filterName.contains("muxer")) {
        muxerThreadsFilter = filter;
      } else if (filterName.startsWith("ldap")) {
        ldapThreadsFilter = filter;
      } else if (filterName.contains("aq adapter")) {
        aqAdapterThreadsFilter = filter;
      }
    }

    Arrays.fill(wlsDefaultExecuteThreadIds, -1);
    LinkedList<ThreadInfo> pendingThreadList = new LinkedList<ThreadInfo>(threadLinkedList);
    createThreadGroups(pendingThreadList, allWLSFilterList, true, nestedWLSCategory);
    createThreadGroups(pendingThreadList, allNonWLSFilterList, false, nestedNonWLSCategory);

    // Check for Missing ExecuteThread Ids now that WLS related threads have been filtered.
    StringBuffer missingExecuteThreadIdsBuf = new StringBuffer(100);
    
    boolean firstThread = true;
    for (int i = 0; i <= maxWlsDefaultExecuteThreadId; i++) {
      if (wlsDefaultExecuteThreadIds[i] == -1) {       
        if (!firstThread) 
          missingExecuteThreadIdsBuf.append(", ");
        
        missingExecuteThreadIdsBuf.append("ExecuteThread: '" + i + "'");
        firstThread = false;
      }
    }
    
    if (missingExecuteThreadIdsBuf.length() > 0) {
      theLogger.warning("WLS Default ExecuteThreads Missing : " 
              + missingExecuteThreadIdsBuf.toString());
      
      ThreadGroup restOfWLSTG = null;
      
      for(ThreadGroup tg: wlsThreadGroupList) {
        if (tg.getName().equals(REST_OF_WLS)) {
          restOfWLSTG = tg;
          
          ThreadAdvisory missingThreadAdvisory 
                  = ThreadAdvisory.lookupThreadAdvisoryByName(
                      ThreadLogicConstants.WLS_EXECUTETHREADS_MISSING);
          missingThreadAdvisory.setDescrp(missingThreadAdvisory.getDescrp() 
                  + ". Missing Thread Ids: " + missingExecuteThreadIdsBuf.toString());
          
          ((RestOfWLSThreadGroup)restOfWLSTG).addMissingThreadsAdvisory(missingThreadAdvisory);
          break;
        }
      }
      
      if (restOfWLSTG != null) {        
      
        for(Filter filter: allWLSFilterList) {

          if (filter.getName().equals(REST_OF_WLS)) {
            Filter restOfWLSFilter = filter;          
            restOfWLSFilter.setInfo(restOfWLSTG.getOverview());
            break;
          }
        }
      }
    }
    
    // For the rest of the unknown type threads, add them to the unknown group
    for (ThreadInfo ti : pendingThreadList) {
      unknownThreadGroup.addThread(ti);
      ti.setThreadGroup(unknownThreadGroup);
    }
    createThreadGroupNestedCategories(unknownThreadGroup, unknownCompositeFilter, nestedNonWLSCategory);
  }

  private void createThreadGroups(LinkedList<ThreadInfo> pendingThreadList, ArrayList<Filter> filterList, boolean isWLSThreadGroup, NestedCategory parentCategory) {
    for (Filter filter : filterList) {
      String name = filter.getName();

      // Special processing for Unknown thread group
      // only the remaining threads have to be added to Unknown thread group
      if (name.contains("Unknown")) {
        continue;
      }

      ThreadGroup tg = ThreadGroupFactory.createThreadGroup(name);
      ArrayList<String> excludedAdvisories = filter.getExcludedAdvisories();
      if (excludedAdvisories != null && excludedAdvisories.size() > 0) {
        for(String advisoryId: filter.getExcludedAdvisories()) {

          //theLogger.finest(name + " > Adding exclusion for:" + advisoryId);
          ThreadAdvisory tadv = ThreadAdvisory.lookupThreadAdvisoryByName(advisoryId);
          //theLogger.finest("Found ThreadAdvisory :" + tadv);
          if (tadv != null)
            tg.addToExclusionList(tadv);
        }      
      }

      boolean foundAtleastOneThread = false;
      for (Iterator<ThreadInfo> iterator = pendingThreadList.iterator(); iterator.hasNext();) {
        ThreadInfo ti = iterator.next();
        
        // Check for the thread id and mark it for WLS Default ExecuteThreads
        if (isWLSThreadGroup) {
          int threadId = getWLSDefaultExecuteThreadId(ti);
          if (threadId >= 0) {
            
            if (threadId > maxWlsDefaultExecuteThreadId) {
              maxWlsDefaultExecuteThreadId = threadId;
            }
            
            incrementTotalWLSDefaultExecuteThreads();
            wlsDefaultExecuteThreadIds[threadId] = 1;      
          }
        }
        
        if (filter.matches(ti)) {
          //theLogger.finest("Found Match against filter: " + filter.getName() + ", for Thread:" + ti.getName());
          tg.addThread(ti);
          ti.setThreadGroup(tg);
          iterator.remove();
          foundAtleastOneThread = true;
        }
      }

      if (foundAtleastOneThread) {
        threadGroupList.add(tg);

        if (isWLSThreadGroup) {
          wlsThreadGroupList.add(tg);
        } else {
          nonWlsThreadGroupList.add(tg);
        }
        
        createThreadGroupNestedCategories(tg, filter, parentCategory);
      }
    }
  }

  private void createThreadGroupNestedCategories(ThreadGroup tg, Filter associatedFilter, NestedCategory parentCategory) {

    tg.runAdvisory();

    NestedCategory nestedCategory = parentCategory.getSubCategory(associatedFilter.getName());
    HealthLevelAdvisoryFilter warningFilter = new HealthLevelAdvisoryFilter("Threads at Warning Or Above",
            HealthLevel.WARNING);
    nestedCategory.addToFilters(warningFilter);
    // nestedCategory.addToFilters(blockedFilter);
    // nestedCategory.addToFilters(stuckFilter);
    // nestedCategory.setAsBlockedIcon();

    CompositeFilter readsCompositeFilter = new CompositeFilter("Reading Data From Remote Endpoint");
    readsCompositeFilter.setInfo("The thread is waiting for a remote response or still reading incoming request (via socket or rmi call)");
    Filter waitingOnRemote = new Filter("Reading Data From Remote Endpoint", "(socketRead)|(ResponseImpl.waitForData)",
            2, false, false, true);
    readsCompositeFilter.addFilter(waitingOnRemote, true);
    readsCompositeFilter.addFilter(ldapThreadsFilter, false);
    readsCompositeFilter.addFilter(muxerThreadsFilter, false);
    readsCompositeFilter.addFilter(aqAdapterThreadsFilter, false);
    nestedCategory.addToFilters(readsCompositeFilter);

    ArrayList<HotCallPattern> hotPatterns = tg.getHotPatterns();
    if (hotPatterns.size() > 0) {
      int count = 1;
      ThreadAdvisory hotcallPatternAdvsiory = ThreadAdvisory.getHotPatternAdvisory();
      for (HotCallPattern hotcall : hotPatterns) {
        HotCallPatternFilter fil = new HotCallPatternFilter("Hot Call Pattern - " + count, hotcall.geThreadPattern());
        String color = hotcallPatternAdvsiory.getHealth().getBackgroundRGBCode();
        StringBuffer sb = new StringBuffer("<font size=5>Advisories: ");
        ThreadLogic.appendAdvisoryLink(sb, hotcallPatternAdvsiory);
        sb.append("</font><br><br>");


        fil.setInfo(sb.toString() + "<pre> Multiple Threads are exhibiting following call execution pattern:\n"
                + hotcall.geThreadPattern() + "</pre>");
        nestedCategory.addToFilters(fil);
        count++;
      }
    }

    associatedFilter.setInfo(tg.getOverview());
  }
  
    
  public boolean isWLSDefaultExecuteThread(ThreadInfo ti) {
    String threadName = ti.getName();
    if (threadName == null)
      return false;
    
    return threadName.contains("weblogic.kernel.Default") && threadName.contains("ExecuteThread");
  }
  
  public int getWLSDefaultExecuteThreadId(ThreadInfo ti) {
    if (!isWLSDefaultExecuteThread(ti))
      return -1;
    
    try {
      int threadIdBeginIndex = ti.getName().indexOf("ExecuteThread: '") + 16;// "ExecuteThread: 'ID';
      int threadIdEndIndex = ti.getName().indexOf("'", threadIdBeginIndex+1);
      return Integer.parseInt(ti.getName().substring(threadIdBeginIndex, threadIdEndIndex)); 
    } catch(Exception e) {
      return -1;
    }
  }
  
  public int getTotalWLSDefaultExecuteThreads() {
    return totalWlsDefaultExecuteThreads;
  }
  
  public int incrementTotalWLSDefaultExecuteThreads() {
    return ++totalWlsDefaultExecuteThreads;
  }
  
  public int getMaxWLSDefaultExecuteThreadId() {
    return maxWlsDefaultExecuteThreadId;
  }
}
