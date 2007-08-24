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
 * @version $Id: DBSchema.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */

public class DBSchema implements MetadataNodeInterface, TreeNode, org.apache.ojb.tools.mapping.reversedb.gui.PropertySheetModel
{
  private java.sql.DatabaseMetaData dbMeta;
  private DBCatalog aDBCatalog;
  
  private boolean enabled = true;

  private java.util.TreeMap tmTables = new java.util.TreeMap();
  
  private String m_strSchemaName;  
  
  
  /** Creates a new instance of DBSchema */
  public DBSchema( java.sql.DatabaseMetaData pdbMeta, DBCatalog paDBCatalog,
                   String pstrSchemaName)
  {
    aDBCatalog = paDBCatalog;
    dbMeta = pdbMeta;
    m_strSchemaName = pstrSchemaName;
  }

  public boolean isEnabled()
  {
    return this.enabled;
  }
  
  public void setEnabled(boolean b)
  {
    this.enabled = b;
  }
  
  
  public DBCatalog getDBCatalog()
  {
    return this.aDBCatalog;
  }
  
  public String getSchemaName()
  {
    return this.m_strSchemaName;
  }
  
  public boolean isTreeEnabled()
  {
    return this.getDBCatalog().isTreeEnabled() && this.isEnabled();
  }
  
  
  public void read()
    throws java.sql.SQLException
  {
    java.sql.ResultSet rs = dbMeta.getTables(this.getDBCatalog().getCatalogName(), this.getSchemaName(), "%", null);
    while (rs.next())
    {
      String strTableCat = rs.getString("TABLE_CAT");
      String strSchemaName = rs.getString("TABLE_SCHEM");
      String strTableName = rs.getString("TABLE_NAME");
      String strTableType = rs.getString("TABLE_TYPE");
      // Pointbase returns the catalog name in uppercase here and in mixed
      // case in getCatalogs(). Therefore we have to use toUpper().
      if (   
          (strTableCat!=null && strTableCat.equalsIgnoreCase(this.getDBCatalog().getCatalogName()) || strTableCat==this.getDBCatalog().getCatalogName())
          &&
          (strSchemaName!=null && strSchemaName.equals(this.getSchemaName()) || strSchemaName==this.getSchemaName())
         )
        this.addTable(strTableName, strTableType);
      
    }
    rs.close();
    
    rs = dbMeta.getColumns(this.getDBCatalog().getCatalogName(), this.getSchemaName(), "%", "%");
    while (rs.next())
    {
      String strSchemaName = rs.getString("TABLE_SCHEM");
      String strTableName = rs.getString("TABLE_NAME");
      String strColumnName = rs.getString("COLUMN_NAME");
      int iDataType = rs.getInt("DATA_TYPE");
      String strTypeName = rs.getString("TYPE_NAME");
      int iColumnSize = rs.getInt("COLUMN_SIZE");
      int iNullable = rs.getInt("NULLABLE");
      this.addColumn(strTableName, strColumnName,
      iDataType, strTypeName, iColumnSize, iNullable);
    }
    rs.close();    
  }
  
  public void addTable(String strTableName, String strTableType)
    throws java.sql.SQLException
  {
    DBTable aDBTable = new DBTable(dbMeta, this, strTableName);
    this.tmTables.put(strTableName, aDBTable);
    aDBTable.read();
  }

  public void addColumn(String strTableName, String strColumnName,
            int iDataType, String strTypeName, int iColumnSize, int iNullable)
  {
    DBTable aDBTable= this.getTable(strTableName);
    if (aDBTable != null)
    {
      aDBTable.addColumn( strColumnName,
                          iDataType, strTypeName, iColumnSize, iNullable);
    }
  }
  
  public void addPrimaryKeyColumn(String strTableName, String strColumnName)
  {
    DBTable aDBTable = this.getTable(strTableName);
    if (aDBTable != null)
    {
      aDBTable.addPrimaryKeyColumn(strColumnName);
    }
  }
  
  
  public DBTable getTable(String strTableName)
  {
    return (DBTable)tmTables.get(strTableName);
  }
  
  public void generateReferences()
    throws java.sql.SQLException
  {
    Iterator it = tmTables.values().iterator();
    while (it.hasNext())
    {
      ((DBTable)it.next()).generateReferences();
    }    
  }  
  
  public java.util.Enumeration children ()
  {
    return java.util.Collections.enumeration(this.tmTables.values());
  }
  
  public boolean getAllowsChildren ()
  {
    return true;
  }
  
  public TreeNode getChildAt(int param)
  {
    TreeNode tn = (TreeNode)tmTables.values().toArray()[param];
    return tn;
  }
  
  public int getChildCount ()
  {
    return this.tmTables.size();
  }
  
  public int getIndex(TreeNode treeNode)
  {
    int indexOf = new java.util.Vector(tmTables.values()).indexOf(treeNode);
    return indexOf;
  }
  
  public TreeNode getParent()
  {
    return this.aDBCatalog;
  }
  
  public boolean isLeaf ()
  {
    return false;
  }  
  
  public String toString()
  {
    if (m_strSchemaName == null || m_strSchemaName.trim().length() == 0) 
       return "<empty  schema>";
    else return this.m_strSchemaName;
  }
    
  public Class getPropertySheetClass ()
  {
    return org.apache.ojb.tools.mapping.reversedb.gui.DBSchemaPropertySheet.class;
  }
  
  public String getXML()
  {
      java.io.StringWriter swr = new java.io.StringWriter();
      writeXML(new java.io.PrintWriter(swr));
      return swr.getBuffer().toString();
  }

  public void writeXML(java.io.PrintWriter pw) 
  {
      Iterator i = this.tmTables.values().iterator();
      while (i.hasNext())
      {
        ((DBTable)i.next()).writeXML(pw);
      }      
  }
  
  public void generateJava (java.io.File aFile, String strHeader, String strFooter) throws java.io.IOException, java.io.FileNotFoundException
  {
    Iterator i = this.tmTables.values().iterator();
    while (i.hasNext()) ((DBTable)i.next()).generateJava(aFile, strHeader, strFooter);
  }
  
  public void setPackage (String packageName)
  {
    Iterator i = this.tmTables.values().iterator();
    while (i.hasNext()) ((DBTable)i.next()).setPackage(packageName);            
  }

  public void disableClassesWithRegex(org.apache.regexp.RE aRegexp)
  {
    Iterator it = this.tmTables.values().iterator();
    while (it.hasNext()) ((DBTable)it.next()).disableClassesWithRegex(aRegexp);        
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
// Revision 1.1  2004/05/04 13:45:01  arminw
// move reverseDB stuff
//
// Revision 1.11  2004/04/04 23:53:42  brianm
// Fixed initial copyright dates to match cvs repository
//
// Revision 1.10  2004/03/11 18:16:22  brianm
// ASL 2.0
//
// Revision 1.9  2003/09/13 16:59:44  brj
// prevent NPE
// patch by Jason Pyeron
//
// Revision 1.8  2003/06/29 14:28:07  florianbruckner
// fix a bug that could cause problems with all databases that use mixed or lower case catalog names (e.g. Informix, MySQL)
//
// Revision 1.7  2003/06/21 10:34:01  florianbruckner
// implement XML generation with PrintWriter; getXML() still works and uses writeXML(java.io.PrintWriter)
//
// Revision 1.6  2003/06/07 10:10:04  brj
// some style fixes
//
// Revision 1.5  2003/02/21 12:44:10  florianbruckner
// add support for PointBase. The version distributed with Sun ONE AppServer
// returns a mixed case catalog name ("PointBase") in getCatalogs() and an upper
// case catalog name in getTables().
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
