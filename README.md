# ThreadLogic

ThreadLogic, an open source Thread Dump Analyzer tool, for the Middleware community.
This tool was created by Sabha Parameswaran, owner of this repo, (in colloboration with Eric Gross) when he was with Oracle Fusion Middleware Architect Team.

## Motivation behind ThreadLogic
 
 The previous set of TDA tools (Samurai/TDA) don't mine the thread dumps or provide a more detailed view of what each thread is doing while just limiting themselves to reporting the state (locked/waiting/running) or the lock information.  They don't mention the type of activity within a thread, should it be treated as normal or deserving a closer look? Can a pattern or anti-pattern be applied against them? Any possible optimizations? Are there any hot spots? Any classification of threads based on their execution cycles? 

 ThreadLogic was created to address these deficiencies. It is based on a fork of the TDA open source tool with new capabilities to analyze and provide advice based on a set of extensible advisories and thread grouping while supporting all JVM thread dumps. Also, a thorough and in-depth analysis of WebLogic Server thread dumps is provided. Both the thread grouping and advisories are extensible where user can add new patterns to match and tag or group threads. Please check the document in the ThreadLogic projects web site for more details of the tool.

 ## Reasons behind this Repo  
 This repo hosts copy of the older bits of ThreadLogic that were on java.net project which has been subsequently retired. The owner of this repo decided to host a copy of the bits and maintain it going forward due to the retirement of the java.net/projects site and also based on requests from users for access to the tool. The author of the repo hopes to add new advisories/patterns based on user submissions of different thread dumps (can be via issues or gists).
