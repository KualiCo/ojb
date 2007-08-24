package org.apache.ojb.tools.mapping.reversedb;

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

import java.util.Iterator;
import javax.swing.tree.TreeNode;

/**
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DBMeta.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class DBMeta implements MetadataNodeInterface, TreeNode, org.apache.ojb.tools.mapping.reversedb.gui.PropertySheetModel
{
  private boolean enabled = true;
  
  private java.sql.DatabaseMetaData dbMeta;
  
  // private java.util.TreeMap hmCatalogs = new java.util.TreeMap();
  private java.util.HashMap hmCatalogs = new java.util.HashMap();
  private String catalogTerm;
  private String catalogSeparator;
  private boolean isCatalogAtStart;
  private boolean supportsCatalogsInDataManipulation;
  private boolean supportsCatalogsInIndexDefinitions;
  private boolean supportsCatalogsInPrivilegeDefinitions;
  private boolean supportsCatalogsInProcedureCalls;
  private boolean supportsCatalogsInTableDefinitions;
  private String schemaTerm;
  
  private String catalogPattern;
  private String schemaPattern;
  
  private String databaseProductName = null;
  private String databaseProductVersion = null;
  
  
  /** Creates a new instance of DBSchema */
  public DBMeta (String pCatalogPattern, String pSchemaPattern, java.sql.DatabaseMetaData pDbMeta) throws java.sql.SQLException
  {
    super();
    this.dbMeta = pDbMeta;
    this.catalogPattern = pCatalogPattern;
    this.schemaPattern  = pSchemaPattern;
    System.err.println("Using " + dbMeta.getDriverName() + " "  + dbMeta.getDriverVersion());
  }
  
  public DBMeta (java.sql.DatabaseMetaData pDbMeta) throws java.sql.SQLException
  {
      this(null, null, pDbMeta);
  }
  
  public String getSchemaPattern()
  {
    return this.schemaPattern;
  }
  
  public boolean isEnabled()
  {
    return this.enabled;
  }
  
  public void setEnabled(boolean b)
  {
    this.enabled = b;
  }

  public String getDatabaseProductVersion()
  {
    return this.databaseProductVersion;
  }
  
  public String getDatabaseProductName()
  {
    return this.databaseProductName;
  }
  
  public boolean getSupportsCatalogsInIndexDefinitions()
  {
    return this.supportsCatalogsInIndexDefinitions;
  }
  
  public boolean getSupportsCatalogsInDataManipulation()
  {
    return this.supportsCatalogsInDataManipulation;
  }
  
  public boolean getSupportsCatalogsInPrivilegeDefinitions()
  {
    return this.supportsCatalogsInPrivilegeDefinitions;
  }
  
  public boolean getSupportsCatalogsInProcedureCalls()
  {
    return this.supportsCatalogsInProcedureCalls;
  }
  
  public boolean getSupportsCatalogsInTableDefinitions()
  {
    return this.supportsCatalogsInTableDefinitions;
  }
  
  public String getCatalogTerm()
  {
    return this.catalogTerm;
  }
  
  public String getSchemaTerm()
  {
    return this.schemaTerm;
  }
  
  public String getCatalogSeparator()
  {
    return this.catalogSeparator;
  }
  
  public boolean getIsCatalogAtStart()
  {
    return this.isCatalogAtStart;
  }
  
  public DBCatalog getCatalog(String catalogName)
  {
    return (DBCatalog)this.hmCatalogs.get(catalogName);
  }
  
  public void read()
    throws java.sql.SQLException
  {
    this.databaseProductName = dbMeta.getDatabaseProductName ();
    this.databaseProductVersion = dbMeta.getDatabaseProductVersion ();
    catalogTerm = dbMeta.getCatalogTerm();
    catalogSeparator = dbMeta.getCatalogSeparator();
    isCatalogAtStart = dbMeta.isCatalogAtStart();
    supportsCatalogsInDataManipulation = dbMeta.supportsCatalogsInDataManipulation();
    supportsCatalogsInIndexDefinitions = dbMeta.supportsCatalogsInIndexDefinitions();
    supportsCatalogsInPrivilegeDefinitions = dbMeta.supportsCatalogsInPrivilegeDefinitions();
    supportsCatalogsInProcedureCalls = dbMeta.supportsCatalogsInProcedureCalls();
    supportsCatalogsInTableDefinitions = dbMeta.supportsCatalogsInTableDefinitions();
    schemaTerm = dbMeta.getSchemaTerm();
    
    java.sql.ResultSet rs = dbMeta.getCatalogs();
    int count = 0;
    while(rs.next())
    {
        count++;
        String strCatalogName = rs.getString("TABLE_CAT");
        try
        {
          if (new org.apache.regexp.RE(this.catalogPattern).match(strCatalogName))
          {
            DBCatalog aDBCatalog = new DBCatalog(dbMeta, this, strCatalogName);
            this.hmCatalogs.put(strCatalogName, aDBCatalog);
          }
        }
        catch (org.apache.regexp.RESyntaxException ex)
        {
          // This expception should be reported, but this does not fit currently.
          ex.printStackTrace();
        }
    }
    rs.close();
    if (count==0)
    {
        DBCatalog aDBCatalog = new DBCatalog(dbMeta, this, null);
        this.hmCatalogs.put(null, aDBCatalog); 
    }
    
    // Read after closing recordset.
    Iterator it = hmCatalogs.values().iterator();
    while (it.hasNext()) ((DBCatalog)it.next()).read();
  }
  
  public void generateReferences()
    throws java.sql.SQLException
  {
    Iterator it = this.hmCatalogs.values().iterator();
    while (it.hasNext())
    {
      ((DBCatalog)it.next()).generateReferences();
    }    
  }
  
  public void debug()
  {

  }
  
  public java.util.Enumeration children ()
  {
    return java.util.Collections.enumeration(this.hmCatalogs.values());
  }
  
  public boolean getAllowsChildren ()
  {
    return true;
  }
  
  public TreeNode getChildAt(int param)
  {
    return (TreeNode)this.hmCatalogs.values().toArray()[param];
  }
  
  public int getChildCount ()
  {
    return this.hmCatalogs.size();
  }
  
  public int getIndex(TreeNode treeNode)
  {
    return new java.util.Vector(hmCatalogs.values()).indexOf(treeNode);
  }
  
  public TreeNode getParent()
  {
    return null;
  }
  
  public boolean isLeaf ()
  {
    return false;
  }
  
  public String toString()
  {
    return "DBMeta";
  }
    
  public Class getPropertySheetClass ()
  {
    return org.apache.ojb.tools.mapping.reversedb.gui.DBMetaPropertySheet.class;
  }

  public String getXML()
  {
        java.io.StringWriter sw = new java.io.StringWriter();
        writeXML(new java.io.PrintWriter(sw));
        return sw.getBuffer().toString();      
  }
  
  public void writeXML(java.io.PrintWriter pw) 
  {
      
    
    pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    pw.println("<!DOCTYPE descriptor-repository SYSTEM \"repository.dtd\">" );
    pw.println("<descriptor-repository version=\"0.9.9\">" );
    pw.println("  <jdbc-connection-descriptor" );
    pw.println("    jcd-alias=\"default\"" );
    pw.println("    default-connection=\"true\"" );
    pw.println("    platform=\"XXXX\"" );
    pw.println("    jdbc-level=\"1.0\"");
    pw.println("    driver=\"XXX\"" );
    pw.println("    protocol=\"XXX\"" );
    pw.println("    subprotocol=\"XXX\"");
    pw.println("    dbalias=\"XXX\"" );
    pw.println("    username=\"XXX\"");
    pw.println("    password=\"XXX\">");
    pw.println("  </jdbc-connection-descriptor>");
    
    Iterator i = this.hmCatalogs.values().iterator();
    while (i.hasNext())
    {
      ((DBCatalog)i.next()).writeXML(pw);
    }
    pw.println("</descriptor-repository>");      
  }
  
  public void generateJava (java.io.File aFile, String strHeader, String strFooter) throws java.io.IOException, java.io.FileNotFoundException
  {
    Iterator it = this.hmCatalogs.values().iterator();
    while (it.hasNext()) ((DBCatalog)it.next()).generateJava(aFile, strHeader, strFooter);
  }
  
  public void setPackage (String packageName)
  {
    Iterator it = this.hmCatalogs.values().iterator();
    while (it.hasNext()) ((DBCatalog)it.next()).setPackage(packageName);    
  }
  
  public void disableClassesWithRegex(org.apache.regexp.RE aRegexp)
  {
    Iterator it = this.hmCatalogs.values().iterator();
    while (it.hasNext()) ((DBCatalog)it.next()).disableClassesWithRegex(aRegexp);        
  }
  
  
}


/***************************** Changelog *****************************
// $Log: not supported by cvs2svn $
// Revision 1.1.2.1  2005/12/21 22:32:04  tomdz
// Updated license
//
// Revision 1.1  2004/05/05 16:39:05  arminw
// fix fault
// wrong package structure used:
// org.apache.ojb.tools.reversdb
// org.apache.ojb.tools.reversdb2
//
// instead of
// org.apache.ojb.tools.mapping.reversdb
// org.apache.ojb.tools.mapping.reversdb2
//
// Revision 1.1  2004/05/04 13:45:00  arminw
// move reverseDB stuff
//
// Revision 1.7  2004/04/04 23:53:42  brianm
// Fixed initial copyright dates to match cvs repository
//
// Revision 1.6  2004/03/11 18:16:22  brianm
// ASL 2.0
//
// Revision 1.5  2003/06/21 10:31:45  florianbruckner
// implement XML generation with PrintWriter; getXML() still works and uses writeXML(java.io.PrintWriter)
//
// Revision 1.4  2003/01/28 21:42:53  florianbruckner
// update XML generation
//
// Revision 1.3  2003/01/28 19:59:14  florianbruckner
// some updates to schema reading to make it a bit more compatible
//
// Revision 1.2  2002/06/17 19:34:33  jvanzyl
// Correcting all the package references.
// PR:
// Obtained from:
// Submitted by:
// Reviewed by:
//
// Revision 1.1.1.1  2002/06/17 18:16:52  jvanzyl
// Initial OJB import
//
// Revision 1.3  2002/05/16 11:47:09  florianbruckner
// fix CR/LF issue, change license to ASL
//
// Revision 1.2  2002/05/16 10:43:59  florianbruckner
// use jakarta-regexp instead of gnu-regexp due to the move to jakarta.
//
// Revision 1.1  2002/04/18 11:44:16  mpoeschl
//
// move files to new location
//
// Revision 1.3  2002/04/07 09:05:16  thma
// *** empty log message ***
//
// Revision 1.2  2002/03/11 17:36:05  florianbruckner
// fix line break issue for these files that were previously checked in with -kb
//
// Revision 1.1  2002/03/04 17:19:32  thma
// initial checking for Florians Reverse engineering tool
//
// Revision 1.1.1.1  2002/02/20 13:35:25  Administrator
// initial import
//
/***************************** Changelog *****************************/
