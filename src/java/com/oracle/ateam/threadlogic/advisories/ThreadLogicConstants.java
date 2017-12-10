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
package com.oracle.ateam.threadlogic.advisories;

public class ThreadLogicConstants {

  public static final int BLOCKED_THREADS_THRESHOLD = 3;

  public static final int THREAD_STACK_OFFSET = 3;
  public static final int MIN_THREADSTACK_LEN_TO_CONSIDER = 10;
  public static final int MAX_THREADSTACK_LEN_SUBSET = 20;

  // Min size of the thread stack depth to be considered active/busy and
  // involved in some application activity
  public static final int ACTIVETHREAD_STACKDEPTH = 15;

  // Min number of occurences of a pattern before it can be tagged as a Hot
  // method call..
  public static final int HOT_CALL_MIN_OCCURENCE = 5;

  public static final String FINALIZER_THREAD = "Finalize";
  public static final String FINALIZER_THREAD_BLOCKED = "Finalizer.doFinalize";
  public static final String PARALLEL_GC_THREADS = "ParallelGCThreads";
  
  public static final int TOO_MANY_THREADS_LIMIT = 100;
  public static final String TOO_MANY_THREADS = "Too Many Threads";
  public static final String WLS_MUXER_THREADS = "WebLogic Muxer Threads";

  public static final String STUCK_PATTERN = "STUCK";
  public static final String DEADLOCK_PATTERN = "DEADLOCK";
  public static final String REENTRANTLOCK_PATTERN = "ReentrantLock";
  public static final String SEMAPHORE_PATTERN = "Semaphore";
  public static final String SOCKET_READ = "SocketInputStream.socketRead";
  public static final String SERVLET_PATTERN1 = "HttpServlet.service";
  public static final String SERVLET_PATTERN2 = "ServletRequestImpl.run";
  public static final String EJB_PATTERN = "weblogic.ejb.container.internal";

  
  public static final String DB_STMT_EXECUTE = "Statement.executeQuery";
  public static final String DB_PSTMT_EXECUTE = "PreparedStatement.execute";
  
  public static final String HOT_CALL_PATTERN = "HotCallPattern";
  public static final String BLOCKED_THREADS = "BlockedThreads";
  public static final String CONTENTION_FOR_UNOWNED_LOCK = "ContentionForUnownedLock";
  public static final String WAITING_WHILE_BLOCKING = "WaitWhileBlockingPattern";
  public static final String WAITING_INSIDE_WEBLAYER = "WebLayerBlocked";
  public static final String EJB_BLOCKED = "EJB Blocked";
  public static final String MUXER_WAITING = "MuxerWaiting";
  public static final String WLS_JMS_QUEUE_BOTTLENECK = "WLSJMSQueueBottleneck";
  public static final String WLSMUXER_PROCESS_SOCKETS = "SocketMuxer.processSockets";
  public static final String WLS_SUBSYSTEM_REQUEST_OVERFLOW = "WLSSubsystemRequestOverflow";
  public static final String WLS_CLUSTER_MESSAGERECEIVER_RUNNING = "MessageReceiverRunning";

  
  public static final String WLS_WEB_REQUEST = "MessageReceiverRunning";
  public static final String WLS_SESSION_REPLICATION = "MessageReceiverRunning";
  
  public static final String LISTENER_MISSING = "Socket Listener Missing";
  public static final String WLS_EXECUTETHREADS_MISSING = "WLS ExecuteThreads Missing";
  public static final String WLS_IDLE_THREADS = "ExecuteThread.waitForRequest";  
  public static final String LISTENER_THREAD = "SocketImpl.socketAccept";  
  public static final String WLS_SERVICES_STARTUP = "ServerServicesManager.startService";
  public static final String WLS_DEFAULT_THREAD_POOL = "weblogic.kernel.default";
  public static final String WLS_DEFAULT_THREAD_POOL_STARVATION = "ThreadStarvation";
  
  public static final String SEMAPHORE_ACQUIRE = "Semaphore.acquire";
  
  public static final String SOA_IDLE_THREADS = "SOAIdleThread";
  public static final String SOA_AQ_ADAPTER_THREAD = "AQ Dequeue Agent";
  public static final String SOA_JMS_ADAPTER_THREAD = "Oracle SOA JMS Adapter";
  public static final String SOA_DB_ADAPTER_THREAD = "Oracle SOA DB Adapter";
  public static final String SOA_FILE_ADAPTER_THREAD = "Oracle SOA File Adapter";
  public static final String SOA_Coherence_ADAPTER_THREAD = "Oracle SOA Coherence Adapter";
  public static final String SOA_FTP_ADAPTER_THREAD = "Oracle SOA FTP Adapter";
  public static final String SOA_LDAP_ADAPTER_THREAD = "Oracle SOA LDAP Adapter";
  public static final String SOA_MQ_ADAPTER_THREAD = "Oracle SOA MQ Adapter";
  public static final String SOA_MSMQ_ADAPTER_THREAD = "Oracle SOA MSMQ Adapter";
  public static final String SOA_SOCKET_ADAPTER_THREAD = "Oracle SOA Socket Adapter";
  public static final String SOA_UMS_ADAPTER_THREAD = "Oracle SOA UMS Adapte";
  public static final String SOA_ENGINE_PATTERN1 = "com.collaxa.cube.engine.ejb.impl.bpel.BPELEngineBean";
  public static final String SOA_ENGINE_PATTERN2 = "com.collaxa.cube.engine.ejb.impl.bpel.BPELDispatcherBean";
  public static final String SOA_ENGINE_BLOCKED = "BPELEngineBlocked";
  public static final String SOA_ENGINE_NORMAL = "BPELEngine Normal Execution";
  public static final String SOA_HTTPCLIENT_PATTERN = "HTTPClient/StreamDemultiplexor";
  public static final String SOA_HTTPCLIENT_STUCKED= "HTTP Client Contention";
  public static final String SOA_DMS_COLLECTOR_PATTERN = "oracle.dms.collector.Collector";
  public static final String SOA_DMS_COLLECTOR_STUCKED = "DMS Collector Stucked";
  public static final String SOA_BPELXPATHFUNCTIONRESOLVER_PATTERN = "com.collaxa.cube.xml.xpath.BPELXPathFunctionResolver.resolveFunction";
  public static final String SOA_BPELXPATHFUNCTIONRESOLVER_STUCKED = "BPELXPATHFUNCTIONRESOLVER Stucked";
  
  public static final String CLUSTER_DEPLOYMENT_PATTERN = "oracle.integration.platform.blocks.deploy.CoherenceCompositeDeploymentCoordinatorImpl.submitRequestAndWaitForCompletion";
  public static final String CLUSTER_DEPLOYMENT_STUCKED = "Cluster Deployment Stucked";
  
  public static final String MFT_IDLE_THREADS = "Oracle MFT Idle Thread";
  public static final String MFT_THREAD = "Oracle MFT Thread";
  
  public static final String OSB_WAIT_FOR_SERVICE_CALLOUT = "PipelineContextImpl.SynchronousListener.waitForResponse";
  public static final String OSB_EJB_INBOUND = "com.bea.wli.sb.transports.jejb.gen.inb.BaseInboundEJBHelper.callPipeline";
  public static final String OSB_TXMGR_BEGINTX = "com.bea.wli.config.transaction.TransactionManager..beginTransaction";
  public static final String OSB_DERIVED_CACHE = "com.bea.wli.config.derivedcache.DerivedCache.Purger.changesCommitted";
  public static final String OSB_WAIT_FOR_EJB_RESPONSE = "OSB_EJB_RESPONSE_WAIT";
          
  public static final String AQ_ADAPTER_POLLER = "oracle.tip.adapter.db.inbound.InboundWorkWrapper";
  public static final String IWAY_SAP_POLLER = "com.ibi.sap30.inbound.SapInboundAdapter$MasterThread";
  public static final String GENERIC_POLLER = "poll";
  public static final String GENERIC_SELECT = "doSelect";

  public static final String[] POLLERS = { AQ_ADAPTER_POLLER, IWAY_SAP_POLLER, GENERIC_POLLER, GENERIC_SELECT };

  public static final String ADVISORY_MAP_XML = "com/oracle/ateam/threadlogic/resources/AdvisoryMap.xml";
  public static final String WLS_THREADGROUP_DEFN_XML = "com/oracle/ateam/threadlogic/resources/WLSGroups.xml";
  public static final String NONWLS_THREADGROUP_DEFN_XML = "com/oracle/ateam/threadlogic/resources/NonWLSGroups.xml";

  public static final String[] HOTSPOT_TD_LABEL = new String[] { "Full thread dump", "VM Periodic Task Thread" };
  public static final String[] JROCKIT_TD_LABEL = new String[] { "FULL THREAD DUMP", "END OF THREAD DUMP" };
  public static final String[] IBM_TD_LABEL = new String[] { "LOCKS subcomponent dump routine",
      "CLASSES subcomponent dump routine" };

  public static final String[][] TD_LABELS = { HOTSPOT_TD_LABEL, JROCKIT_TD_LABEL, IBM_TD_LABEL };
}
