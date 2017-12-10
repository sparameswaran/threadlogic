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
 * This file is part of TDA - Thread Dump Analysis Tool.
 *
 * Foobar is free software; you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with Foobar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * $Id: BeaJDKParserTest.java,v 1.4 2010-04-01 08:58:58 irockel Exp $
 */

package com.pironet.tda;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author irockel
 */
public class BeaJDKParserTest extends TestCase {
    
    public BeaJDKParserTest(String testName) {
        super(testName);
    }            

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(BeaJDKParserTest.class);
        
        return suite;
    }
    
    /**
     * Test of hasMoreDumps method, of class com.pironet.tda.SunJDKParser.
     */
    public void testDumpLoad() throws FileNotFoundException, IOException {
        System.out.println("dumpLoad");
        FileInputStream fis = null;
        DumpParser instance = null;
        
        try {
            fis = new FileInputStream("test/none/jrockit_15_dump.txt");
            Map dumpMap = new HashMap();
            Vector topNodes = new Vector();
            instance = DumpParserFactory.get().getDumpParserForLogfile(fis, dumpMap, false, 0);
            
            assertTrue(instance instanceof BeaJDKParser);

            while (instance.hasMoreDumps()) {
                topNodes.add(instance.parseNext());
            }

            // check if three dumps are in it.
            //assertEquals(3, topNodes.size());
        } finally {
            if(instance != null) {
                instance.close();
            }
            if(fis != null) {
                fis.close();
            }
        }
    }

}
