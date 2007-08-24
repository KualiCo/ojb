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

/**
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DBColumn.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class DBColumn implements MetadataNodeInterface, javax.swing.tree.TreeNode, org.apache.ojb.tools.mapping.reversedb.gui.PropertySheetModel
{
  private DBTable aTable;
  private String strColumnName;
  private String strJavaFieldName;
  private String strJavaFieldType;
  private int iColumnType;
  private String strColumnTypeName;
  private boolean isPrimaryKeyPart = false;
  private boolean bAutoIncrement = false;
  
  
  private boolean enabled = true;  
  
  
  /** Creates a new instance of DBColumn */
  
  public DBColumn (java.sql.DatabaseMetaData pdbMeta, DBTable paTable,
                   String pstrColumnName, int piColumnType, 
                   String pstrColumnTypeName)
  {
    aTable = paTable;
    strColumnName = pstrColumnName;
/*    this.strJavaFieldName = Character.toLowerCase(pstrColumnName.charAt(0)) 
      + pstrColumnName.substring(1);*/
    this.strJavaFieldName = Namer.nameField(this.strColumnName);
    iColumnType = piColumnType;
    this.strJavaFieldType = Utilities.hmJDBCTypeToJavaType.get(new Integer(iColumnType)).toString();    
    strColumnTypeName = pstrColumnTypeName;
  }
  
  public boolean getAutoIncrement()
  {
    return this.bAutoIncrement;
  }
  
  public void setAutoIncrement(boolean b)
  {
    this.bAutoIncrement = b;
  }
  
  public boolean isEnabled()
  {
    return this.enabled;
  }
  
  public void setEnabled(boolean b)
  {
    this.enabled = b;
  }  
  
  public int getColumnType()
  {
    return this.iColumnType;
  }
  
  public String getJavaFieldName()
  {
    return this.strJavaFieldName;
  }
  
  public void setJavaFieldName(String s)
  {
    this.strJavaFieldName = s;
  }
  
  public String getJavaFieldType()
  {
    return this.strJavaFieldType;
  }
  
  public void setJavaFieldType(String s)
  {
    this.strJavaFieldType = s;
  }
  
  public void setColumnType(int i)
  {
    this.iColumnType = i;
  }
  
  public void setColumnType(String s)
  {
    Integer i = (Integer)Utilities.mJDBCNameToType.get(s);
    if (i != null)
    {
      this.iColumnType = i.intValue();
    }
  }
  
  public String getColumnTypeName()
  {
    return this.strColumnTypeName;
  }
  
  public DBTable getDBTable()
  {
    return this.aTable;
  }
  
  public boolean isTreeEnabled()
  {
    return this.aTable.isTreeEnabled() && this.isEnabled();
  }
  

  public void read()
    throws java.sql.SQLException
  {
  }      
  
  public void generateReferences()
    throws java.sql.SQLException
  {
  }      
  
  public void setPrimaryKeyPart(boolean b)
  {
    this.isPrimaryKeyPart = b;
  }
  
  public boolean isPrimaryKeyPart()
  {
    return this.isPrimaryKeyPart;
  }
  
  public String getColumnName()
  {
    return this.strColumnName;
  }
  
  public java.util.Enumeration children ()
  {
    return null;
  }
  
  public boolean getAllowsChildren ()
  {
    return false;
  }
  
  public javax.swing.tree.TreeNode getChildAt (int param)
  {
    return null;
  }
  
  public int getChildCount ()
  {
    return 0;
  }
  
  public int getIndex (javax.swing.tree.TreeNode treeNode)
  {
    return 0;
  }
  
  public javax.swing.tree.TreeNode getParent ()
  {
    return this.aTable;
  }
  
  public boolean isLeaf ()
  {
    return true;
  }

  public String toString()
  {
    if (this.isPrimaryKeyPart) return strColumnName + " (PK)";
    else return this.strColumnName;
  }

  
  public Class getPropertySheetClass ()
  {
    return org.apache.ojb.tools.mapping.reversedb.gui.DBColumnPropertySheet.class;
  }
  
  public String getXML()
  {
    if (this.isTreeEnabled())
    {
        java.io.StringWriter sw = new java.io.StringWriter();
        writeXML(new java.io.PrintWriter(sw));
        return sw.getBuffer().toString();
/*        
      String strReturn =  
          "  <field-descriptor id=\"" + this.id + "\"" + System.getProperty("line.separator")
       +  "    name=\"" + this.strJavaFieldName + "\"" + System.getProperty("line.separator")
       +  "    column=\"" + this.strColumnName + "\"" + System.getProperty("line.separator")
       +  "    jdbc-type=\"" + Utilities.hmJDBCTypeToName.get(new Integer(this.iColumnType)) + "\"" + System.getProperty("line.separator");
      if (this.isPrimaryKeyPart())
          strReturn += "    primarykey=\"true\"" + System.getProperty("line.separator");
      if (this.getAutoIncrement())
          strReturn += "    <autoincrement>true</autoincrement>" + System.getProperty("line.separator");
      strReturn += "  />";
      return strReturn;      
 */
    }
    else return "";
 
  }  
  
  public void writeXML(java.io.PrintWriter pw)
  {
      pw.println("  <field-descriptor ");
      pw.println("     name=\"" + this.strJavaFieldName + "\"");
      pw.println("     column=\"" + this.strColumnName + "\"");
      pw.println("     jdbc-type=\"" + Utilities.hmJDBCTypeToName.get(new Integer(this.iColumnType)) + "\"" );
      if (this.isPrimaryKeyPart())
          pw.println( "     primarykey=\"true\"" );
      if (this.getAutoIncrement())
          pw.println( "     <autoincrement>true</autoincrement>");
      pw.println("  />");
  }
  
  public void generateJava (java.io.File aFile, String strHeader, String strFooter) throws java.io.IOException, java.io.FileNotFoundException
  {
    throw new UnsupportedOperationException("Generate Java on DBColumn is not allowed");    
  }  
  
  public void setPackage (String packageName)
  {
    throw new UnsupportedOperationException("Set Package on DBColumn is not allowed");    
  }
  
  public String getJavaFieldDefinition()
  {
    if (this.isTreeEnabled())
    {
      return "  private " + this.getJavaFieldType() + " " + this.getJavaFieldName() + ";";
    }
    else return "";
  }
  
   public String getJavaGetterSetterDefinition()
  {
    if (this.isTreeEnabled())
    {
      String strReturn = "";
      strReturn = "  public " + this.getJavaFieldType() + " get" 
        + this.getJavaFieldName().substring(0,1).toUpperCase() 
        + this.getJavaFieldName().substring(1) + "()" 
        + System.getProperty("line.separator");

      strReturn += "  {" + System.getProperty("line.separator");    
      strReturn += "     return this." + this.getJavaFieldName() + ";" + System.getProperty("line.separator");    
      strReturn += "  }" + System.getProperty("line.separator"); 
      strReturn += "  public void set" 
        + this.getJavaFieldName().substring(0,1).toUpperCase() 
        + this.getJavaFieldName().substring(1) + "(" + this.getJavaFieldType() + " param)" 
        + System.getProperty("line.separator");
      strReturn += "  {" + System.getProperty("line.separator");    
      strReturn += "    this." + this.getJavaFieldName() + " = param;" + System.getProperty("line.separator"); 
      strReturn += "  }" + System.getProperty("line.separator");    
      return strReturn;
    }
    else return "";
  }

  /**
   *  @deprecated
   */
  public int getId()
  {
    return 0;
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
// Revision 1.5  2003/06/21 10:27:45  florianbruckner
// implement XML generation with PrintWriter; getXML() still works and uses writeXML(java.io.PrintWriter)
// getId() is deprecated and returns 0 now; Ids are not used for XML generation.
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
// Revision 1.1.1.1  2002/06/17 18:16:51  jvanzyl
// Initial OJB import
//
// Revision 1.2  2002/05/16 11:47:09  florianbruckner
// fix CR/LF issue, change license to ASL
//
// Revision 1.1  2002/04/18 11:44:16  mpoeschl
//
// move files to new location
//
// Revision 1.4  2002/04/09 17:08:32  thma
// *** empty log message ***
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
