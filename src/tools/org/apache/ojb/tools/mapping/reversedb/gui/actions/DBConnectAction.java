package org.apache.ojb.tools.mapping.reversedb.gui.actions;

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

import javax.swing.JOptionPane;

/**
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DBConnectAction.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class DBConnectAction extends javax.swing.AbstractAction
{
    private org.apache.ojb.tools.mapping.reversedb.gui.JFrmMainFrame mainFrame;
  /** Creates a new instance of DBConnectAction */
    public DBConnectAction(org.apache.ojb.tools.mapping.reversedb.gui.JFrmMainFrame pmainFrame)
  {
    super();
    mainFrame = pmainFrame;
  }
  
  public void actionPerformed (java.awt.event.ActionEvent actionEvent)
  {
    java.sql.Connection conn = connectToDB(mainFrame.getProperty("JDBCDriver", ""), 
                                           mainFrame.getProperty("JDBCURL", ""), 
                                           mainFrame.getProperty("JDBCUsername", ""),
                                           mainFrame.getProperty("JDBCPassword", "")) ;
    if (conn != null)                                           
      mainFrame.setConnection(conn);
    else
      new org.apache.ojb.tools.mapping.reversedb.gui.JDlgDBConnection(mainFrame, false, mainFrame).show();
  }

  private java.sql.Connection connectToDB(String strJDBCDriver, String strJDBCURL, 
    String strUsername, String strPassword)
  {
      try
      {
          Class.forName(strJDBCDriver); // "com.informix.jdbc.IfxDriver"
          java.sql.Connection conn = 
            java.sql.DriverManager.getConnection(strJDBCURL, 
                strUsername, strPassword);  // "jdbc:informix-sqli://moon:1526/bci_test:INFORMIXSERVER=ol_bci", "informix", "informix"
          return conn;
      }
      catch (java.sql.SQLException sqlEx)
      {
          java.sql.SQLException currentSqlEx = sqlEx;
          System.out.println (sqlEx.getErrorCode() + ":" + sqlEx.getMessage());
          while (currentSqlEx.getNextException() != null)
          {
            currentSqlEx = currentSqlEx.getNextException();
            System.out.println (sqlEx.getErrorCode() + ":" + sqlEx.getMessage());
          }
          JOptionPane.showMessageDialog(mainFrame, "Error connecting to database:\n" + sqlEx.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);          
          return null;
      }
      catch (java.lang.ClassNotFoundException clNotFoundEx)
      {
          clNotFoundEx.printStackTrace();
          JOptionPane.showMessageDialog(mainFrame, "Cannot find driver class:\n" + clNotFoundEx.getMessage(), "Class Not Found", JOptionPane.ERROR_MESSAGE);
          return null;
      }
      catch (Throwable t)
      {
          t.printStackTrace();
          JOptionPane.showMessageDialog(mainFrame, "Unknown error:\n" + t.getMessage(), "Unknown Error", JOptionPane.ERROR_MESSAGE);
          return null;
      }              
  }  
}

/***************************** Changelog *****************************
// $Log: not supported by cvs2svn $
// Revision 1.1.2.1  2005/12/21 22:32:06  tomdz
// Updated license
//
// Revision 1.1  2004/05/05 16:38:11  arminw
// fix fault
// wrong package structure used:
// org.apache.ojb.tools.reversdb
// org.apache.ojb.tools.reversdb2
//
// instead of
// org.apache.ojb.tools.mapping.reversdb
// org.apache.ojb.tools.mapping.reversdb2
//
// Revision 1.1  2004/05/04 13:44:59  arminw
// move reverseDB stuff
//
// Revision 1.7  2004/04/05 12:16:24  tomdz
// Fixed/updated license in files leftover from automatic license transition
//
// Revision 1.6  2004/04/04 23:53:42  brianm
// Fixed initial copyright dates to match cvs repository
//
// Revision 1.5  2004/03/11 18:16:23  brianm
// ASL 2.0
//
// Revision 1.4  2003/06/21 10:38:16  florianbruckner
// improve error reporting
//
// Revision 1.3  2002/06/18 12:23:15  florianbruckner
// bugfix: was reading a table "CATEGORIES" after opening a connection.
//
// Revision 1.2  2002/06/17 19:34:34  jvanzyl
// Correcting all the package references.
// PR:
// Obtained from:
// Submitted by:
// Reviewed by:
//
// Revision 1.1.1.1  2002/06/17 18:16:54  jvanzyl
// Initial OJB import
//
// Revision 1.2  2002/05/16 11:47:09  florianbruckner
// fix CR/LF issue, change license to ASL
//
// Revision 1.1  2002/04/18 11:44:16  mpoeschl
//
// move files to new location
//
// Revision 1.2  2002/04/07 09:05:17  thma
// *** empty log message ***
//
// Revision 1.1.1.1  2002/02/20 13:35:25  Administrator
// initial import
//
/***************************** Changelog *****************************/
 
