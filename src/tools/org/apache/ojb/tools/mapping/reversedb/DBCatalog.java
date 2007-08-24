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
 * @version $Id: DBCatalog.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class DBCatalog implements MetadataNodeInterface, TreeNode, org.apache.ojb.tools.mapping.reversedb.gui.PropertySheetModel
{
  private DBMeta parsedMetaData;
  private java.sql.DatabaseMetaData dbMeta;
  
  private boolean enabled = true;
  
  private String strCatalogName;
  private java.util.TreeMap hmSchemas = new java.util.TreeMap();
  /** Creates a new instance of DBCatalog */
  public DBCatalog(java.sql.DatabaseMetaData pdbMeta, DBMeta paMeta,
  String pstrCatalogName)
  {
    this.dbMeta = pdbMeta;
    this.parsedMetaData = paMeta;
    this.strCatalogName = pstrCatalogName;
  }
  
  public boolean isEnabled()
  {
    return this.enabled;
  }
  
  public void setEnabled(boolean b)
  {
    this.enabled = b;
  }
  
  public DBSchema getSchema(String strSchemaName)
  {
    return (DBSchema)this.hmSchemas.get(strSchemaName);
  }
  
  public void putSchema(String key, DBSchema schema)
  {
    this.hmSchemas.put(key, schema);
  }
  
  public String getCatalogName()
  {
    return this.strCatalogName;
  }
  
  public DBMeta getDBMeta()
  {
    return parsedMetaData;
  }
  
  public boolean isTreeEnabled()
  {
    return getDBMeta().isEnabled() && this.isEnabled();
  }
  
  public void generateReferences()
  throws java.sql.SQLException
  {
    // Set the catalog name of the connection before accessing the metadata object
    dbMeta.getConnection().setCatalog(this.getCatalogName());
    Iterator it = this.hmSchemas.values().iterator();
    while (it.hasNext())
    {
      ((DBSchema)it.next()).generateReferences();
    }
  }
  
  public void read()
  throws java.sql.SQLException
  {
    // Set the catalog name of the connection before accessing the metadata object
    java.sql.ResultSet rs = dbMeta.getSchemas();
    int count = 0;
    while (rs.next())
    {
      count++;
      String strSchemaName = rs.getString("TABLE_SCHEM");
      // Fix for IBM Informix JDBC 2.21JC2; Schema is padded with spaces, needs to be trimmed
      strSchemaName = strSchemaName.trim();
      try
      {
        if (new org.apache.regexp.RE(this.getDBMeta().getSchemaPattern()).match(strSchemaName))
        {
          this.hmSchemas.put(strSchemaName, new DBSchema(dbMeta, this, strSchemaName));
        }
      }
      catch (org.apache.regexp.RESyntaxException ex)
      {
        // This expception should be reported, but this does not fit currently.
        ex.printStackTrace();
      }
      
    }
    // Fix for MySQL: Create an empty schema
    if (count == 0)
    {
      this.hmSchemas.put("", new DBSchema(dbMeta, this, ""));
    }
    
    rs.close();
    Iterator it = hmSchemas.values().iterator();
    while (it.hasNext()) ((DBSchema)it.next()).read();
    
  }
  
  public void addTable(String strSchemaName, String strTableName, String strTableType)
  throws java.sql.SQLException
  {
    DBSchema aDBSchema= this.getSchema(strSchemaName);
    if (aDBSchema == null)
    {
      aDBSchema = new DBSchema(dbMeta, this, strSchemaName);
      this.putSchema(strSchemaName, aDBSchema);
      aDBSchema.read();
    }
    aDBSchema.addTable(strTableName, strTableType);
  }
  
  public void addColumn(String strSchemaName, String strTableName, String strColumnName,
  int iDataType, String strTypeName, int iColumnSize, int iNullable)
  {
    DBSchema aDBSchema = this.getSchema(strSchemaName);
    if (aDBSchema != null)
    {
      aDBSchema.addColumn(strTableName, strColumnName,
      iDataType, strTypeName, iColumnSize, iNullable);
    }
  }
  
  public void addPrimaryKeyColumn(String strSchemaName, String strTableName,
  String strColumnName)
  {
    DBSchema aDBSchema = this.getSchema(strSchemaName);
    if (aDBSchema != null)
    {
      aDBSchema.addPrimaryKeyColumn(strTableName, strColumnName);
    }
  }
  
  
  public java.util.Enumeration children()
  {
    return java.util.Collections.enumeration(this.hmSchemas.values());
  }
  
  public boolean getAllowsChildren()
  {
    return true;
  }
  
  public TreeNode getChildAt(int param)
  {
    TreeNode tn = (TreeNode)this.hmSchemas.values().toArray()[param];
    return tn;
  }
  
  public int getChildCount()
  {
    return this.hmSchemas.size();
  }
  
  public int getIndex(TreeNode treeNode)
  {
    int indexOf = new java.util.Vector(this.hmSchemas.values()).indexOf(treeNode);
    return indexOf;
  }
  
  public TreeNode getParent()
  {
    return this.parsedMetaData;
  }
  
  public boolean isLeaf()
  {
    return false;
  }
  
  public String toString()
  {
    if (this.strCatalogName == null || this.strCatalogName.trim().length() == 0)
      return "<empty catalog>";
    else return this.strCatalogName;
  }
  
  public Class getPropertySheetClass()
  {
    return org.apache.ojb.tools.mapping.reversedb.gui.DBCatalogPropertySheet.class;
  }
  
  public String getXML()
  {
      java.io.StringWriter swr = new java.io.StringWriter();
      writeXML(new java.io.PrintWriter(swr));
      return swr.getBuffer().toString();
  }
  
  public void writeXML(java.io.PrintWriter pw) 
  {
    Iterator i = this.hmSchemas.values().iterator();
    while (i.hasNext())
    {
      ((DBSchema)i.next()).writeXML(pw);
    }
  }
  
  
  public void generateJava(java.io.File aFile, String strHeader, String strFooter) throws java.io.IOException, java.io.FileNotFoundException
  {
    Iterator i = this.hmSchemas.values().iterator();
    while (i.hasNext()) ((DBSchema)i.next()).generateJava(aFile, strHeader, strFooter);
  }
  
  public void setPackage(String packageName)
  {
    Iterator i = this.hmSchemas.values().iterator();
    while (i.hasNext()) ((DBSchema)i.next()).setPackage(packageName);
  }
  
  public void disableClassesWithRegex(org.apache.regexp.RE aRegexp)
  {
    Iterator it = this.hmSchemas.values().iterator();
    while (it.hasNext()) ((DBSchema)it.next()).disableClassesWithRegex(aRegexp);
  }
  
  
}


/***************************** Changelog *****************************
 * // $Log: not supported by cvs2svn $
 * // Revision 1.1.2.1  2005/12/21 22:32:04  tomdz
 * // Updated license
 * //
 * // Revision 1.1  2004/05/05 16:39:05  arminw
 * // fix fault
 * // wrong package structure used:
 * // org.apache.ojb.tools.reversdb
 * // org.apache.ojb.tools.reversdb2
 * //
 * // instead of
 * // org.apache.ojb.tools.mapping.reversdb
 * // org.apache.ojb.tools.mapping.reversdb2
 * //
 * // Revision 1.1  2004/05/04 13:45:00  arminw
 * // move reverseDB stuff
 * //
 * // Revision 1.7  2004/04/04 23:53:42  brianm
 * // Fixed initial copyright dates to match cvs repository
 * //
 * // Revision 1.6  2004/03/11 18:16:22  brianm
 * // ASL 2.0
 * //
 * // Revision 1.5  2003/06/21 10:23:25  florianbruckner
 * // implement XML generation with PrintWriter; getXML() still works and uses writeXML(java.io.PrintWriter)
 * //
 * // Revision 1.4  2003/01/28 21:42:53  florianbruckner
 * // update XML generation
 * //
 * // Revision 1.3  2003/01/28 19:59:14  florianbruckner
 * // some updates to schema reading to make it a bit more compatible
 * //
 * // Revision 1.2  2002/06/17 19:34:33  jvanzyl
 * // Correcting all the package references.
 * // PR:
 * // Obtained from:
 * // Submitted by:
 * // Reviewed by:
 * //
 * // Revision 1.1.1.1  2002/06/17 18:16:51  jvanzyl
 * // Initial OJB import
 * //
 * // Revision 1.3  2002/05/16 11:47:09  florianbruckner
 * // fix CR/LF issue, change license to ASL
 * //
 * // Revision 1.2  2002/05/16 10:43:59  florianbruckner
 * // use jakarta-regexp instead of gnu-regexp due to the move to jakarta.
 * //
 * // Revision 1.1  2002/04/18 11:44:16  mpoeschl
 * //
 * // move files to new location
 * //
 * // Revision 1.3  2002/04/07 09:05:16  thma
 * // *** empty log message ***
 * //
 * // Revision 1.2  2002/03/11 17:36:04  florianbruckner
 * // fix line break issue for these files that were previously checked in with -kb
 * //
 * // Revision 1.1  2002/03/04 17:19:32  thma
 * // initial checking for Florians Reverse engineering tool
 * //
 * // Revision 1.1.1.1  2002/02/20 13:35:25  Administrator
 * // initial import
 * //
 * /***************************** Changelog *****************************/
