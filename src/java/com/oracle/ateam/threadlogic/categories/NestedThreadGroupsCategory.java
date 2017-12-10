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

import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.ThreadLogic;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup;
import com.oracle.ateam.threadlogic.advisories.ThreadGroupFactory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup.HotCallPattern;
import com.oracle.ateam.threadlogic.filter.CompositeFilter;
import com.oracle.ateam.threadlogic.filter.Filter;
import com.oracle.ateam.threadlogic.filter.HealthLevelAdvisoryFilter;
import com.oracle.ateam.threadlogic.filter.HotCallPatternFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.tree.DefaultMutableTreeNode;

public class NestedThreadGroupsCategory extends NestedCategory {

  private LinkedList<ThreadInfo> threadLinkedList = new LinkedList<ThreadInfo>();
  private ArrayList<ThreadGroup> threadGroupList = new ArrayList<ThreadGroup>();
  private ArrayList<ThreadGroup> wlsThreadGroupList = new ArrayList<ThreadGroup>();
  private ArrayList<ThreadGroup> nonWlsThreadGroupList = new ArrayList<ThreadGroup>();

  private Category threads;
  private String overview = null;

  private ArrayList<Filter> allNonWLSFilterList = new ArrayList<Filter>();
  private ArrayList<Filter> allWLSFilterList = new ArrayList<Filter>();

  private ThreadGroup unknownThreadGroup;
  private Filter jvmThreadsFilter, ldapThreadsFilter, muxerThreadsFilter, aqAdapterThreadsFilter;

  private Filter allWLSThreadsFilter1 = new Filter(
      "WLS Threads1",
      "(weblogic)|(oracle.integration)|(com.octetstring.vde)|(orabpel)|(dms)|(HTTPClient)|(oracle.integration)|(oracle.mds)|(oracle.ias)|(oracle)",
      2, false, false, true);
  private Filter allWLSThreadsFilter2 = new Filter("WLS Threads2",
      "(Weblogic)|(orabpel)|(weblogic)|(oracle.dfw)|(JPS)|(WsMgmt)|(Fabric)", 0, false, false, true);

  private CompositeFilter wlsCompositeFilter;
  private CompositeFilter nonwlsCompositeFilter;
  private CompositeFilter unknownCompositeFilter;

  private NestedCategory nestedWLSCategory, nestedNonWLSCategory;

  public NestedThreadGroupsCategory() {
    super("Thread Groups");
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

  private void addNonWLSFilters() {

    nonwlsCompositeFilter = new CompositeFilter("Non-WLS Thread Groups");
    nonwlsCompositeFilter.setInfo("Non-WebLogic Thread Groups");

    // Exclude all wls related threads for it
    nonwlsCompositeFilter.addFilter(allWLSThreadsFilter1, false);
    nonwlsCompositeFilter.addFilter(allWLSThreadsFilter2, false);

    addToFilters(nonwlsCompositeFilter);

    nestedNonWLSCategory = getSubCategory(nonwlsCompositeFilter.getName());

    jvmThreadsFilter = new Filter(
        "JVM Threads",
        "(GC task)|(Low Memory Detector)|(CompilerThread)|(Finalizer)|(VM Periodic Task)|(Attach Listener)|(Attach .andler)|(OperatingSystemMXBean)"
            + "|(MemoryPoolMXBean)|(Code Generation Thread)|Code Optimization Thread|(VM Thread)|(GC task thread)|(Sensor Event Thread)|(JMAPI event thread)"
            + "|(GC Worker Thread)|(OC Main Thread)|(Gc Slave Thread)|(RMI TCP Accept)|(Reference Handler)|(JIT Compilation)|(Signal Dispatcher)",
        0, false, false, true);
    allNonWLSFilterList.add(jvmThreadsFilter);

    Filter sapThreadsFilter = new Filter("SAP Connector Threads", "com.sap.conn.jco", 2, false, false, true);
    allNonWLSFilterList.add(sapThreadsFilter);

    Filter iwayThreadsFilter = new Filter("IWay Adapter Threads", "com.ibi.adapters.util", 2, false, false, true);
    allNonWLSFilterList.add(iwayThreadsFilter);

    ldapThreadsFilter = new Filter("LDAP Threads", "netscape.ldap.LDAPConnThread", 2, false, false, true);
    allNonWLSFilterList.add(ldapThreadsFilter);

    Filter coherenceThreadsFilter = new Filter("Coherence Threads", "com.tangosol.coherence", 2, false, false, true);
    allNonWLSFilterList.add(coherenceThreadsFilter);

    Filter timerThreadsFilter = new Filter("Java Timer Threads", "java.util.Timer", 2, false, false, true);
    allNonWLSFilterList.add(timerThreadsFilter);

    addUnknownThreadGroupFilter();

    for (Filter filter : allNonWLSFilterList) {
      nestedNonWLSCategory.addToFilters(filter);
    }
  }

  private void addUnknownThreadGroupFilter() {

    unknownCompositeFilter = new CompositeFilter("Unknown or Custom Threads");
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

  private void addWLSFilters() {

    wlsCompositeFilter = new CompositeFilter("WLS Thread Groups");
    wlsCompositeFilter.setInfo("WebLogic Thread Groups");

    // Include all wls related threads for it
    wlsCompositeFilter.addFilter(allWLSThreadsFilter1, true);
    wlsCompositeFilter.addFilter(allWLSThreadsFilter2, true);

    addToFilters(wlsCompositeFilter);

    nestedWLSCategory = getSubCategory(wlsCompositeFilter.getName());

    muxerThreadsFilter = new Filter("WebLogic Muxer Threads", "SocketMuxer", 2, false, false, true);
    allWLSFilterList.add(muxerThreadsFilter);

    Filter osbThreadsFilter = new Filter("Oracle Service Bus (OSB) Threads",
        "(com.bea.wli.sb.transports)|(com.bea.wli.sb.pipeline)", 2, false, false, true);
    allWLSFilterList.add(osbThreadsFilter);

    Filter wlsTimerThreadsFilter = new Filter("WebLogic Timer Threads", "weblogic.time", 2, false, false, true);
    allWLSFilterList.add(wlsTimerThreadsFilter);

    Filter wlsClusterThreadsFilter = new Filter("WebLogic Cluster Threads", "weblogic.cluster", 2, false, false, true);
    allWLSFilterList.add(wlsClusterThreadsFilter);

    Filter embeddedLdapThreadsFilter = new Filter("WebLogic Embedded LDAP Threads", "com.octetstring.vde", 2, false,
        false, true);
    allWLSFilterList.add(embeddedLdapThreadsFilter);

    Filter jmsThreadsFilter = new Filter("WebLogic JMS Threads", "(weblogic.jms)|(weblogic.messaging)", 2, false,
        false, true);

    Filter wlsThreadsFilter = new Filter("WebLogic Threads",
        "(weblogic.work)|(weblogic.kernel)|(weblogic.store.internal)|(weblogic.Server)|(weblogic.server)", 2, false,
        false, true);

    Filter soaThreadsFilter = new Filter(
        "Oracle SOA Threads",
        "(orabpel.engine.pool)|(orabpel.invoke.pool)|(HTTPClient)|(oracle.mds)|(oracle.integration.platform.blocks)"
            + "|(oracle.j2ee.ws)|(oracle.wsm)|(orabpel.sweeper)|(com.collaxa.cube)|(oracle.tip.mediator)|(oracle.tip.b2b)",
        2, false, false, true);

    Filter soadfwThreadsFilter = new Filter("Oracle SOA DFW Threads",
        "(orabpel.engine.pool)|(orabpel.invoke.pool)|(oracle.dfw)|(DmsThread)", 0, false, false, true);

    aqAdapterThreadsFilter = new Filter("Oracle AQ Adapter Threads", "oracle.integration.platform.blocks.event.saq", 2,
        false, false, true);

    Filter jmsAdapterThreadsFilter = new Filter("Oracle JMS Adapter Threads",
        "oracle.tip.adapter.jms.inbound.JmsConsumer", 2, false, false, true);

    CompositeFilter aqAdapterCompositeFilter = new CompositeFilter("Oracle AQ Adapter Threads");
    aqAdapterCompositeFilter.addFilter(aqAdapterThreadsFilter, true);
    aqAdapterCompositeFilter.addFilter(soadfwThreadsFilter, false);
    allWLSFilterList.add(aqAdapterCompositeFilter);

    CompositeFilter jmsAdapterCompositeFilter = new CompositeFilter("Oracle JMS Adapter Threads");
    jmsAdapterCompositeFilter.addFilter(jmsAdapterThreadsFilter, true);
    jmsAdapterCompositeFilter.addFilter(soadfwThreadsFilter, false);
    allWLSFilterList.add(jmsAdapterCompositeFilter);

    CompositeFilter soaCompositeFilter = new CompositeFilter("Oracle SOA Threads");
    soaCompositeFilter.addFilter(soaThreadsFilter, true);
    soaCompositeFilter.addFilter(soadfwThreadsFilter, true);
    allWLSFilterList.add(soaCompositeFilter);

    Filter orclThreadsFilter = new Filter(
        "Oracle Framework Threads",
        "(oracle.dfw)|(oracle.dms)|(oracle.core.ojdl)|(oracle.ias.cache.WorkerThread)|(oracle.as)|(oracle.core)|(oracle.security.jps)|(OracleTimeoutPollingThread)",
        2, false, false, true);
    CompositeFilter orclCompositeFilter = new CompositeFilter("Oracle Framework Threads");
    orclCompositeFilter.addFilter(orclThreadsFilter, true);
    orclCompositeFilter.addFilter(soaThreadsFilter, false);
    orclCompositeFilter.addFilter(soadfwThreadsFilter, false);
    allWLSFilterList.add(orclCompositeFilter);

    CompositeFilter jmsCompositeFilter = new CompositeFilter("WebLogic JMS Threads");
    jmsCompositeFilter.addFilter(jmsThreadsFilter, true);
    allWLSFilterList.add(jmsCompositeFilter);

    CompositeFilter wlsCompositeFilter = new CompositeFilter("WebLogic Threads");
    wlsCompositeFilter.addFilter(wlsThreadsFilter, true);
    allWLSFilterList.add(wlsCompositeFilter);

    for (Filter filter : allWLSFilterList) {
      String filterName = filter.getName();
      nestedWLSCategory.addToFilters(filter);

      if (!filterName.contains("WebLogic Threads"))
        wlsCompositeFilter.addFilter(filter, false);

      if (filterName.contains("JMS Threads") || filterName.contains("WebLogic Threads"))
        continue;

      jmsCompositeFilter.addFilter(filter, false);
    }

  }

  private void addFilters() {

    /*
     * BlockedAdvisoryFilter blockedFilter = new
     * BlockedAdvisoryFilter("Threads blocked for Lock"); Filter stuckFilter =
     * new Filter("STUCK Threads", "\\[STUCK\\]", 0, false, false, true);
     * stuckFilter.setInfo(
     * "WebLogic Work Manager has tagged the thread as STUCK as it has not completed execution of a request for a very long time. Check if its doing repeat function (like polling in infinite loop) or is stuck on some unavailable resource or deadlock or due to other constraints."
     * );
     */

    addWLSFilters();
    addNonWLSFilters();

    LinkedList<ThreadInfo> pendingThreadList = new LinkedList<ThreadInfo>(threadLinkedList);
    createThreadGroups(pendingThreadList, allWLSFilterList, true, nestedWLSCategory);
    createThreadGroups(pendingThreadList, allNonWLSFilterList, false, nestedNonWLSCategory);    
            
    // For the rest of the unknown type threads, add them to the unknown group
    for (ThreadInfo ti : pendingThreadList) {
      unknownThreadGroup.addThread(ti);
    }
    createThreadGroupNestedCategories(unknownThreadGroup, unknownCompositeFilter, nestedNonWLSCategory);
  }

  private void createThreadGroups(LinkedList<ThreadInfo> pendingThreadList, ArrayList<Filter> filterList,
      boolean isWLSThreadGroup, NestedCategory parentCategory) {
    for (Filter filter : filterList) {
      String name = filter.getName();

      // Special processing for Unknown thread group
      // only the remaining threads have to be added to Unknown thread group
      if (name.contains("Unknown"))
        continue;

      boolean isJvmGroup = name.contains("JVM");
      ThreadGroup tg = ThreadGroupFactory.createThreadGroup(name);

      
      boolean foundAtleastOneThread = false;
      for (Iterator<ThreadInfo> iterator = pendingThreadList.iterator(); iterator.hasNext();) {
        ThreadInfo ti = iterator.next();

        if (filter.matches(ti)) {
          tg.addThread(ti);
          iterator.remove();
          foundAtleastOneThread = true;
        }
      }

      if (foundAtleastOneThread) {
        threadGroupList.add(tg);

        if (isWLSThreadGroup)
          wlsThreadGroupList.add(tg);
        else
          nonWlsThreadGroupList.add(tg);
      }

      createThreadGroupNestedCategories(tg, filter, parentCategory);
    }

  }

  private void createThreadGroupNestedCategories(ThreadGroup tg, Filter associatedFilter, NestedCategory parentCategory) {

    tg.runAdvisory();
    associatedFilter.setInfo(tg.getOverview());

    NestedCategory nestedCategory = parentCategory.getSubCategory(associatedFilter.getName());
    HealthLevelAdvisoryFilter warningFilter = new HealthLevelAdvisoryFilter("Threads at Warning Or Above",
        HealthLevel.WARNING);
    nestedCategory.addToFilters(warningFilter);
    // nestedCategory.addToFilters(blockedFilter);
    // nestedCategory.addToFilters(stuckFilter);
    // nestedCategory.setAsBlockedIcon();

    CompositeFilter readsCompositeFilter = new CompositeFilter("Reading Data From Remote Endpoint");
    readsCompositeFilter
        .setInfo("The thread is waiting for a remote response or still reading incoming request (via socket or rmi call)");
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
  }

}
