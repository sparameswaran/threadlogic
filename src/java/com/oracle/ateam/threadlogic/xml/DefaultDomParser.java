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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 *
 * @author saparam
 */
public class DefaultDomParser {
  
	private List list = new ArrayList();	
	protected Document dom;
  
	public DefaultDomParser(){
		
	}

	public void run() throws Exception {
	}
	
	
	protected void parseXmlFile(InputStream input) throws Exception{
		//get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {
			
			//Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			//parse using builder to get DOM representation of the XML file
			dom = db.parse(input);
			

		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
      throw pce;
		}catch(SAXException se) {
			se.printStackTrace();
      throw se;
		}catch(IOException ioe) {
			ioe.printStackTrace();
      throw ioe;
		}
	}

	
	protected void parseDocument(String nodeName)  throws Exception {
		
		Element docEle = dom.getDocumentElement();		
		
		NodeList nl = docEle.getElementsByTagName(nodeName);
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength();i++) {
				
				//get the employee element
				Element el = (Element)nl.item(i);
				
				//get the Employee object
				Object e = getElement(el);
				
				//add it to list
				list.add(e);
			}
		}
	}


	protected Object getElement(Element grpEl) {		
		return new Object();
	}

  protected void parseRepeaterNodes(Element parentElement, String enclosingTag, 
          String repeaterToken, ArrayList<String> addToList) throws Exception {
          
    NodeList nl = parentElement.getElementsByTagName(enclosingTag);
    if(nl != null && nl.getLength() > 0) {

      Element inclusionEl = (Element)nl.item(0); ;
      nl = inclusionEl.getElementsByTagName(repeaterToken);
      for(int i = 0 ; i < nl.getLength();i++) {
        Element patternEl = (Element)nl.item(i); ;   
        String simpleGrpId = getTextValue(patternEl);
        addToList.add(simpleGrpId);
      }
    }
  }

	protected String getTextValue(Element ele, String tagName) throws Exception {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
      
			textVal = el.getFirstChild().getNodeValue().trim();
		}

		return textVal;
	}
  
  protected boolean getBooleanValue(Element ele, String tagName) throws Exception {
		return Boolean.parseBoolean(getTextValue(ele,tagName));
	}

	
	protected int getIntValue(Element ele, String tagName) throws Exception {
		//in production application you would catch the exception
		return Integer.parseInt(getTextValue(ele,tagName));
	}

  protected String getTextValue(Node node) throws Exception {
    if (node != null)
      return node.getFirstChild().getNodeValue().trim();
              
    return null;
  }
  
  protected boolean getBooleanValue(Node node) throws Exception {
    if (node != null)
      return Boolean.parseBoolean(getTextValue(node));
    return false;
	}

	
	protected int getIntValue(Node node)  throws Exception {		
    if (node != null)
      return Integer.parseInt(getTextValue(node));
    return -1;
	}
	

}

