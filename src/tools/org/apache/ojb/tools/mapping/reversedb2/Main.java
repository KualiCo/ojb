package org.apache.ojb.tools.mapping.reversedb2;

import org.apache.ojb.tools.mapping.reversedb2.gui.*;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: Main.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class Main 
{    
    public static final String PROPERTY_JDBCDRIVER = "JDBCDriver";
    public static final String PROPERTY_JDBCURL    = "JDBCUrl";
    public static final String PROPERTY_JDBCUSER   = "JDBCUser";
    public static final String PROPERTY_MAINFRAME_WIDTH = "MainframeWidth";
    public static final String PROPERTY_MAINFRAME_HEIGHT = "MainframeHeight";
    public static final String PROPERTY_MAINFRAME_POSX = "MainframePosX";
    public static final String PROPERTY_MAINFRAME_POSY = "MainframePosY";
    
    private ExtendedProperties reverseDbProperties = new ExtendedProperties("ReverseDb2.properties");
    
    private static Main singleton = new Main();
    
    public static ExtendedProperties getProperties()
    {
        return singleton.reverseDbProperties;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        new JFrmMain().show();
    }
    
    /** Creates a new instance of Main */
    private Main() 
    {
    }
    
}
