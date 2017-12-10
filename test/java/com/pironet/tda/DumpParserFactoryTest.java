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
 * DumpParserFactoryTest.java
 *
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
 * $Id: DumpParserFactoryTest.java,v 1.5 2008-02-15 09:05:04 irockel Exp $
 */
package com.pironet.tda;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import junit.framework.*;
import java.io.InputStream;
import java.util.Map;

/**
 *
 * @author irockel
 */
public class DumpParserFactoryTest extends TestCase {
    
    public DumpParserFactoryTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(DumpParserFactoryTest.class);
        
        return suite;
    }

    /**
     * Test of get method, of class com.pironet.tda.DumpParserFactory.
     */
    public void testGet() {
        System.out.println("get");
        
        DumpParserFactory result = DumpParserFactory.get();
        assertNotNull(result);                
    }

    /**
     * Test of getDumpParserForVersion method, of class com.pironet.tda.DumpParserFactory.
     */
    public void testGetDumpParserForSunLogfile() throws FileNotFoundException {
        System.out.println("getDumpParserForVersion");
        
        InputStream dumpFileStream = new FileInputStream("test/none/test.log");
        Map threadStore = null;
        DumpParserFactory instance = DumpParserFactory.get();
        
        DumpParser result = instance.getDumpParserForLogfile(dumpFileStream, threadStore, false, 0);
        assertNotNull(result);
        
        assertTrue(result instanceof com.pironet.tda.SunJDKParser);
    }

    /**
     * Test of getDumpParserForVersion method, of class com.pironet.tda.DumpParserFactory.
     */
    public void testGetDumpParserForBeaLogfile() throws FileNotFoundException {
        System.out.println("getDumpParserForVersion");
        
        InputStream dumpFileStream = new FileInputStream("test/none/jrockit_15_dump.txt");
        Map threadStore = null;
        DumpParserFactory instance = DumpParserFactory.get();
        
        DumpParser result = instance.getDumpParserForLogfile(dumpFileStream, threadStore, false, 0);
        assertNotNull(result);
        
        assertTrue(result instanceof com.pironet.tda.BeaJDKParser);
    }    
}
