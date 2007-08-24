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

import javax.swing.tree.TreeNode;
import java.sql.SQLException;
import java.io.File;

/**
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DBTable.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class DBTable implements MetadataNodeInterface, TreeNode, org.apache.ojb.tools.mapping.reversedb.gui.PropertySheetModel
{
  private java.sql.DatabaseMetaData dbMeta;
  private DBSchema aSchema;
  private java.util.HashMap hmReferences = new java.util.HashMap(0);
  private java.util.HashMap hmCollections = new java.util.HashMap(0);
  private java.util.TreeMap tmColumns = new java.util.TreeMap();
  private java.util.Vector  vSubTreeNodes = new java.util.Vector();
  private String strTableName;
  private String strClassName;
  private String strPackageName = ""; // In default package by default ;-)
  private String strConversionStrategyClass = "";
  
  private boolean dynamicProxy = false;

  private boolean enabled = true;

  
  /** Creates a new instance of DBTable */
  public DBTable (java.sql.DatabaseMetaData pdbMeta, DBSchema paSchema,
                  String pstrTableName)
  {
    strTableName = pstrTableName;
    // this.strClassName = Character.toUpperCase (strTableName.charAt(0)) + strTableName.substring(1).toLowerCase();
    this.strClassName = Namer.nameClass(this.strTableName);
    aSchema = paSchema;
    dbMeta = pdbMeta;
  }
  
  public boolean hasDynamicProxy()
  {
    return dynamicProxy;
  }
  
  public void setDynamicProxy(boolean b)
  {
    dynamicProxy = b;
  }
  
  public String getConversionStrategyClass()
  {
    return this.strConversionStrategyClass;
  }
  
  public void setConversionStrategyClass(String s)
  {
    this.strConversionStrategyClass = s;
  }
  
  public boolean isEnabled()
  {
    return this.enabled;
  }
  
  public void setEnabled(boolean b)
  {
    this.enabled = b;
  }
  
  public boolean isTreeEnabled()
  {
    return this.aSchema.isTreeEnabled() && this.isEnabled();
  }
  
  
  public DBColumn getColumn(String colName)
  {
    return (DBColumn)tmColumns.get(colName);
  }
  
  public String getTableName()
  {
    return strTableName;
  }
  
  public String getFQTableName()
  {
    String strReturn = null;
    // Are table names supported in table definitions?
    if (aSchema.getDBCatalog().getDBMeta().getSupportsCatalogsInTableDefinitions ())
    {
      // Yes, include catalog name in fq table name
      // Now check, where we have to specify the catalog
      if (aSchema.getDBCatalog().getDBMeta().getIsCatalogAtStart ())
      {
        // At the beginning
        strReturn = aSchema.getDBCatalog().getCatalogName() 
          + aSchema.getDBCatalog().getDBMeta().getCatalogSeparator ()
          + aSchema.getSchemaName() + "." + this.getTableName();
      }
      else
      {
        // At the end
        strReturn = aSchema.getSchemaName() + "." + this.getTableName()
          + aSchema.getDBCatalog().getDBMeta().getCatalogSeparator ()
          + aSchema.getDBCatalog().getCatalogName();
      }
    }
    else
    {
      strReturn = aSchema.getSchemaName() + "." + this.getTableName();
    }
    return strReturn;
  }
  
  public String getClassName()
  {
    return strClassName;
  }
  
  public void setClassName(String s)
  {
    this.strClassName = s;
  }
  
  public String getPackageName()
  {
    return strPackageName;
  }

  public void setPackageName(String s)
  {
    this.strPackageName = s;
  }
  
  public String getFQClassName()
  {
    if (this.getPackageName() != null && this.getPackageName().trim().length() > 0)
      return this.getPackageName() + "." + this.getClassName();
    else
      return this.getClassName();
  }
  
  public DBSchema getDBSchema()
  {
    return this.aSchema;
  }
  
  
  public void read() throws SQLException
  {

  }
  
  public void addColumn(String strColumnName,
            int iDataType, String strTypeName, int iColumnSize, int iNullable)
  {
    DBColumn aDBColumn = new DBColumn(this.dbMeta, this, strColumnName, iDataType, strTypeName);
    this.tmColumns.put(strColumnName, aDBColumn);
  }
  
  public void addPrimaryKeyColumn(String strColumnName)
  {
    DBColumn aDBColumn = this.getColumn(strColumnName);
    if (aDBColumn != null)
    {
      aDBColumn.setPrimaryKeyPart(true);
    }
  }  
  
  public void generateReferences() throws SQLException
  {
    // Check if primary keys for this table have already been set. If not, do it now...
    // This is necessary because Oracle doesn't return the Primary Keys for all tables
    // and for Informix it speeds up things significantly if we do it onve for all tables.
    java.util.Iterator it = this.tmColumns.values().iterator();
    boolean hasNoPrimaryKey = true;
    while (hasNoPrimaryKey && it.hasNext()) if ( ((DBColumn)it.next()).isPrimaryKeyPart()) hasNoPrimaryKey = false;
    if (hasNoPrimaryKey) readPrimaryKeys();        
    
    // Now generate References and Collections
    generateFKReferences();
    generateFKCollections();
    vSubTreeNodes.addAll(this.tmColumns.values());
    vSubTreeNodes.addAll(this.hmCollections.values());
    vSubTreeNodes.addAll(this.hmReferences.values());
  }    
  
  public java.util.Enumeration children ()
  {
    return vSubTreeNodes.elements();
  }
  
  public boolean getAllowsChildren ()
  {
    return true;
  }
  
  public TreeNode getChildAt(int param)
  {
    TreeNode tn = 
      (TreeNode)vSubTreeNodes.elementAt(param);
    return tn;
  }
  
  public int getChildCount ()
  {
    return this.vSubTreeNodes.size();
  }
  
  public int getIndex(TreeNode treeNode)
  {
    return this.vSubTreeNodes.indexOf(treeNode);
  }
  
  public TreeNode getParent()
  {
    return this.aSchema;
  }
  
  public boolean isLeaf ()
  {
    if (this.vSubTreeNodes.size() == 0) return true;
    else return false;
  }  
  
  public String toString()
  {
    return this.strTableName;
  }
  
  
  private void readPrimaryKeys() throws SQLException
  {
    // Now get the primary keys for this table. Ignore any exceptions thrown here as
    // primary keys are not absolutely necessary for reverse engineering
    java.sql.ResultSet rs = null;
    try
    {
      rs = dbMeta.getPrimaryKeys(null, 
                                  this.getDBSchema().getSchemaName(), this.strTableName);
      while (rs.next())
      {
        String strCatalogName = rs.getString("TABLE_CAT");
        String strSchemaName = rs.getString("TABLE_SCHEM");
        if ( (strSchemaName == null && this.aSchema.getSchemaName() == null || strSchemaName.equals(this.aSchema.getSchemaName())) 
             &&
             (strCatalogName == null && this.aSchema.getDBCatalog().getCatalogName() == null 
              || strCatalogName.equals(this.aSchema.getDBCatalog().getCatalogName())))
        {
          String strColumnName = rs.getString("COLUMN_NAME");
          String pkName = rs.getString("PK_NAME");
          DBColumn dbcol = (DBColumn)tmColumns.get(strColumnName);
          if (dbcol != null) dbcol.setPrimaryKeyPart(true);
        }
      }
    }
    catch (SQLException sqlEx)
    {
      // Ignore excpetions here.
    }
    finally
    {
      try
      {
        rs.close();    
      }
      catch (Throwable t)
      {} // Ignore this exception
    }
  }

  private void generateFKReferences() throws SQLException
  {
    // References, points from this class to another class using attributes
    // of this class
    // Ignore any exceptions thrown here.
    java.sql.ResultSet rs = null;
    try
    {
      rs = dbMeta.getImportedKeys(this.getDBSchema().getDBCatalog().getCatalogName(), 
                                                     this.getDBSchema().getSchemaName(), strTableName);
      while (rs.next())
      {
        String strFKSchemaName = rs.getString("FKTABLE_SCHEM");
        String strFKCatalogName = rs.getString("FKTABLE_CAT");

        if (   (strFKCatalogName == null && this.aSchema.getDBCatalog().getCatalogName() == null || strFKCatalogName.equals(this.aSchema.getDBCatalog().getCatalogName()))
            && (strFKSchemaName  == null && this.aSchema.getSchemaName() == null || strFKSchemaName.equals(this.aSchema.getSchemaName())))
        {
          String strPKCatalogName = rs.getString("PKTABLE_CAT");
          String strPKSchemaName = rs.getString("PKTABLE_SCHEM");
          String strPKTableName  = rs.getString("PKTABLE_NAME");
          String strPKColumnName = rs.getString("PKCOLUMN_NAME");
          String strFKTableName  = rs.getString("FKTABLE_NAME");
          String strFKColumnName = rs.getString("FKCOLUMN_NAME");

          // Resolove the primaryKey column
          DBCatalog dbPKCatalog = this.aSchema.getDBCatalog().getDBMeta().getCatalog(strPKCatalogName);
          if (dbPKCatalog != null)
          {
            DBSchema dbPKSchem = dbPKCatalog.getSchema(strPKSchemaName);
            if (dbPKSchem != null)
            {
              DBTable dbPKTable = dbPKSchem.getTable(strPKTableName);
              if (dbPKTable != null)
              {
                DBColumn dbPKColumn = dbPKTable.getColumn(strPKColumnName);
                // The FK column is always from this table.
                DBColumn dbFKColumn = getColumn(strFKColumnName);

                // Now retrieve the FKReference to this table from the collection
                DBFKRelation aFKRef = 
                  (DBFKRelation)this.hmReferences.get(dbPKSchem.getSchemaName() 
                    + "." + dbPKTable.getTableName());
                if (aFKRef == null)
                {
                  aFKRef = new DBFKRelation(dbPKTable, this, false);
                  this.hmReferences.put(dbPKSchem.getSchemaName() 
                    + "." + dbPKTable.getTableName(), aFKRef);
                }
                aFKRef.addColumnPair(dbPKColumn, dbFKColumn);
              }
            }
          }
        }
      }
    }
    catch (SQLException sqlEx)
    {
      // sqlEx.printStackTrace();
    }
    try
    {
      rs.close();    
    }
    catch (Throwable t) {}
  }
  
  private void generateFKCollections() throws SQLException
  {
    // Collections, points from this class to a collection of objects using
    // attributes from the referenced class
    // Ignore any exceptions thrown here
    java.sql.ResultSet rs = null;
    try
    {
      rs = dbMeta.getExportedKeys(this.getDBSchema().getDBCatalog().getCatalogName(), 
                                                       this.getDBSchema().getSchemaName(), strTableName);
      while (rs.next())
      {
        String strPKSchemaName = rs.getString("PKTABLE_SCHEM");
        String strPKCatalogName = rs.getString("PKTABLE_CAT");

        if (    (strPKSchemaName == null && this.aSchema.getSchemaName()==null || strPKSchemaName.equals(this.aSchema.getSchemaName()))
             && (strPKCatalogName == null && this.aSchema.getDBCatalog().getCatalogName() == null 
                 || strPKCatalogName.equals(this.aSchema.getDBCatalog().getCatalogName())))
        {
          String strPKTableName  = rs.getString("PKTABLE_NAME");
          String strPKColumnName = rs.getString("PKCOLUMN_NAME");
          String strFKCatalogName= rs.getString("FKTABLE_CAT");
          String strFKTableName  = rs.getString("FKTABLE_NAME");
          String strFKColumnName = rs.getString("FKCOLUMN_NAME");
          String strFKSchemaName = rs.getString("FKTABLE_SCHEM");        

          // Resolve the FK column. If catalog is supported in the index
          // definition, resolve the catalog of the FK column, otherwise
          // assume the current catalog (Note: This is needed for Informix,
          // because the driver reports null for the catalog in this case.
          DBCatalog dbFKCatalog = null;
          if (this.aSchema.getDBCatalog().getDBMeta().getSupportsCatalogsInIndexDefinitions())
          {
            dbFKCatalog = this.aSchema.getDBCatalog().getDBMeta().getCatalog(strFKCatalogName);
          }
          else
          {
            dbFKCatalog = this.aSchema.getDBCatalog();
          }
          if (dbFKCatalog != null)
          {
            DBSchema dbFKSchema = dbFKCatalog.getSchema(strFKSchemaName);
            if (dbFKSchema != null)
            {
              DBTable dbFKTable = dbFKSchema.getTable(strFKTableName);
              if (dbFKTable != null)
              {
                DBColumn dbFKColumn = dbFKTable.getColumn(strFKColumnName);
                // The PK column is always from this table
                DBColumn dbPKColumn = getColumn(strPKColumnName);
                //Retrieve the FK Reference for the FK Table
                DBFKRelation aFKRef = 
                  (DBFKRelation)this.hmCollections.get(dbFKSchema.getSchemaName() 
                    + "." + dbFKTable.getTableName());
                if (aFKRef == null)
                {
                  aFKRef = new DBFKRelation(this, dbFKTable, true);
                  this.hmCollections.put(dbFKSchema.getSchemaName() 
                    + "." + dbFKTable.getTableName(), aFKRef);
                }
                aFKRef.addColumnPair(dbPKColumn, dbFKColumn);
              }
            }
          }
        }
      }
    }
    catch (SQLException sqlEx)
    {
      // sqlEx.printStackTrace();
    }
    try
    {
      rs.close();    
    }
    catch (Throwable t)
    {
    }
  }
  
  
  public Class getPropertySheetClass ()
  {
    return org.apache.ojb.tools.mapping.reversedb.gui.DBTablePropertySheet.class;
  }
  
  public String getXML()
  {
        java.io.StringWriter sw = new java.io.StringWriter();
        writeXML(new java.io.PrintWriter(sw));
        return sw.getBuffer().toString();
  }  
  
  public void writeXML(java.io.PrintWriter pw)
  {
    // Only generate a Classdescriptor if this table is enabled 
    if (this.isTreeEnabled())
    {
      java.util.Iterator it = tmColumns.values().iterator();
      if (it.hasNext())     
      {
        // Generate the class descriptor
          pw.println("<class-descriptor ");
          pw.println("  class=\"" + this.getFQClassName() + "\"");
          pw.println("  table=\"" + this.getFQTableName() + "\">");
          if (this.hasDynamicProxy())
            pw.println("  <class.proxy>dynamic</class.proxy>");
          if (this.getConversionStrategyClass () != null 
            && this.getConversionStrategyClass ().trim().length() > 0)
            pw.println("  <conversionStrategy>" + this.getConversionStrategyClass () + "</conversionStrategy>");

          it = this.tmColumns.values().iterator();
        
        while (it.hasNext())
        {
            ((DBColumn)it.next()).writeXML(pw);
        }
        
        // Generate references
        it = this.hmReferences.values().iterator();
        while (it.hasNext())
        {
          ((DBFKRelation)it.next()).writeXML(pw);
        }
        // Generate collections
        it = this.hmCollections.values().iterator();
        while (it.hasNext())
        {
          ((DBFKRelation)it.next()).writeXML(pw);
        }        
        pw.println("</class-descriptor>");
      }
    }
  }
  
  public void generateJava(File aFile, String strHeader, String strFooter) throws java.io.IOException, java.io.FileNotFoundException
  {
    if (this.isTreeEnabled())
    {
      // 1. Generate the package directories
      String dirName = this.getPackageName().replace('.', File.separatorChar);
      File packageDir;
      if (this.getPackageName() != null && this.getPackageName().trim().length() > 0)
      {
        packageDir = new File(aFile, dirName);
      }
      else
      {
        packageDir = aFile;
      }

      if (!packageDir.exists()) packageDir.mkdirs();
      File javaFile = new File(packageDir, this.getClassName()+ ".java");
      if (!javaFile.exists()) javaFile.createNewFile();
      java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileOutputStream(javaFile));
      pw.println(strHeader);
      pw.println("// Generated by OJB SchemeGenerator");
      pw.println();
      if (this.getPackageName().trim().length() > 0)
      {
        pw.println("package " + this.getPackageName() + ";");
        pw.println();
      }
      pw.println("public class " + this.getClassName());
      pw.println("{");

      // Generate Fields
      java.util.Iterator it = this.tmColumns.values().iterator();
      while (it.hasNext())
      {
        pw.println(((DBColumn)it.next()).getJavaFieldDefinition());
        pw.println();
      }

      it = this.hmReferences.values().iterator();
      while (it.hasNext())
      {
        pw.println(((DBFKRelation)it.next()).getJavaFieldDefinition());
        pw.println();
      }

      it = this.hmCollections.values().iterator();
      while (it.hasNext())
      {
        pw.println(((DBFKRelation)it.next()).getJavaFieldDefinition());
        pw.println();
      }

      // Generate Getter/Setter
      it = this.tmColumns.values().iterator();
      while (it.hasNext())
      {
        pw.println(((DBColumn)it.next()).getJavaGetterSetterDefinition());
        pw.println();
      }

      it = this.hmReferences.values().iterator();
      while (it.hasNext())
      {
        pw.println(((DBFKRelation)it.next()).getJavaGetterSetterDefinition());
        pw.println();
      }

      it = this.hmCollections.values().iterator();
      while (it.hasNext())
      {
        pw.println(((DBFKRelation)it.next()).getJavaGetterSetterDefinition());
        pw.println();
      }

      pw.println("}");
      pw.println(strFooter);
      pw.close();
    }
  }
  
  public void setPackage (String packageName)
  {
    this.setPackageName(packageName);
  }
  
  public void disableClassesWithRegex(org.apache.regexp.RE aRegexp)
  {
    if (aRegexp.match(this.getClassName())) this.setEnabled(false);
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
// Revision 1.9  2004/04/04 23:53:42  brianm
// Fixed initial copyright dates to match cvs repository
//
// Revision 1.8  2004/03/11 18:16:22  brianm
// ASL 2.0
//
// Revision 1.7  2003/12/12 16:37:16  brj
// removed unnecessary casts, semicolons etc.
//
// Revision 1.6  2003/07/22 11:05:13  florianbruckner
// add a name beautifier (courtesy G.Wayne Kidd)
//
// Revision 1.5  2003/06/21 10:35:03  florianbruckner
// implement XML generation with PrintWriter; getXML() still works and uses writeXML(java.io.PrintWriter)
// does not generate an Id anymore.
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
// Revision 1.2  2002/02/20 13:55:11  Administrator
// add semicolon after package name
//
// Revision 1.1.1.1  2002/02/20 13:35:25  Administrator
// initial import
//
/***************************** Changelog *****************************/
