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
 * Based on: http://www.java-samples.com/showtutorial.php?tutorialid=152
 */
package com.oracle.ateam.threadlogic.xml;

import com.oracle.ateam.threadlogic.utils.CustomLogger;
import java.io.InputStream;
import java.util.ArrayList;


import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 *
 * @author saparam
 */
public class GroupsDefnParser extends DefaultDomParser{
  
  private static Logger theLogger = CustomLogger.getLogger(GroupsDefnParser.class.getSimpleName());
  
	private static final String complexGrp = "ComplexGroup";
  private static final String simpleGrp = "SimpleGroup";
  private static final String[] groupTypes = new String[] { simpleGrp, complexGrp };
  
  private InputStream input;
  private ArrayList<SimpleGroup> simpleGrpList;
  private ArrayList<ComplexGroup> complexGrpList;
  
  public GroupsDefnParser(InputStream input){
    super();
    this.input = input;
    simpleGrpList = new ArrayList<SimpleGroup>();
    complexGrpList = new ArrayList<ComplexGroup>();
	}

	public void run() throws Exception {
		
		//parse the xml file and get the dom object
		parseXmlFile(input);
		
		//get each of the Group elements and create the associated objects
    for (String grp: groupTypes) {
      parseDocument(grp);
    }
	}
	
	
	
	protected void parseDocument(String grpName )  throws Exception{
		
		Element docEle = dom.getDocumentElement();		
		
		NodeList nl = docEle.getElementsByTagName(grpName);
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength();i++) {
				
				//get the group element
				Element el = (Element)nl.item(i);
				
				if (grpName.equals("SimpleGroup")) {
          SimpleGroup e = getSimpleGroup(el);
          theLogger.finest("Parsed: " + e);
          simpleGrpList.add(e);
        } else {
          ComplexGroup e = getComplexGroup(el);
          theLogger.finest("Parsed: " + e);
          complexGrpList.add(e);
        }
			}
		}
	}


	private SimpleGroup getSimpleGroup(Element grpEl) throws Exception {
		String name = null;
    try {
      //for each <SimpleGroup> element get text or int values of 
      //name, visibility, inclusion and location
      name = getTextValue(grpEl,"Name");
      boolean visible = getBooleanValue(grpEl,"Visible");
      boolean inclusionType  = getBooleanValue(grpEl,"Inclusion");
      String matchLocation = getTextValue(grpEl,"MatchLocation");

      //Create a new SimpleGroup with the value read from the xml nodes
      SimpleGroup smpGrp = new SimpleGroup(name, visible, inclusionType, matchLocation);      

      String enclosingTag = "PatternList";
      String repeaterToken = "Pattern";
      
      parseRepeaterNodes(grpEl, enclosingTag, 
              repeaterToken, smpGrp.getPatternList());
      
      enclosingTag = "ExcludedAdvisories";
      repeaterToken = "AdvisoryId";
      
      parseRepeaterNodes(grpEl, enclosingTag, 
              repeaterToken, smpGrp.getExcludedAdvisories());
      
      return smpGrp;
    } catch(Exception e) {
      theLogger.warning("Error parsing SimpleGroup definition with name: " + name 
              + ", associated error: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }    
	}
  
  private ComplexGroup getComplexGroup(Element grpEl)  throws Exception {
		String name = null;
    try {
      
      //for each <ComplexGroup> element get text or int values of 
      //name, visibility
      name = getTextValue(grpEl,"Name");
      boolean visible = getBooleanValue(grpEl,"Visible");
      theLogger.finest("Parsing ComplexElement:" + name);
      ComplexGroup complexGrp = new ComplexGroup(name, visible);

      String enclosingTag = "Inclusions";
      String repeaterToken = "SimpleGroupId";
      
      parseRepeaterNodes(grpEl, enclosingTag, 
              repeaterToken, complexGrp.getInclusionList());
      
      enclosingTag = "Exclusions";      
      
      parseRepeaterNodes(grpEl, enclosingTag, 
              repeaterToken, complexGrp.getExclusionList());
      
      enclosingTag = "ExcludedAdvisories";
      repeaterToken = "AdvisoryId";
      
      parseRepeaterNodes(grpEl, enclosingTag, 
              repeaterToken, complexGrp.getExcludedAdvisories());

      return complexGrp;
    } catch(Exception e) {
      theLogger.warning("Error parsing ComplexGroup definition with name: " + name 
          + ", associated error: " + e.getMessage());
      e.printStackTrace();
      throw e;
    } 
	}

  /**
   * @return the simpleGrpList
   */
  public ArrayList<SimpleGroup> getSimpleGrpList() {
    return simpleGrpList;
  }

  /**
   * @param simpleGrpList the simpleGrpList to set
   */
  public void setSimpleGrpList(ArrayList<SimpleGroup> simpleGrpList) {
    this.simpleGrpList = simpleGrpList;
  }

  /**
   * @return the complexGrpList
   */
  public ArrayList<ComplexGroup> getComplexGrpList() {
    return complexGrpList;
  }

  /**
   * @param complexGrpList the complexGrpList to set
   */
  public void setComplexGrpList(ArrayList<ComplexGroup> complexGrpList) {
    this.complexGrpList = complexGrpList;
  }


	
}

