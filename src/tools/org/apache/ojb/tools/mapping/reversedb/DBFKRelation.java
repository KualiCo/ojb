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
 * @version $Id: DBFKRelation.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class DBFKRelation implements MetadataNodeInterface, javax.swing.tree.TreeNode, org.apache.ojb.tools.mapping.reversedb.gui.PropertySheetModel
{
  DBTable pkTable;
  DBTable fkTable;
  
  private boolean bAutoRetrieve = true;
  private boolean bAutoUpdate   = false;
  private boolean bAutoDelete   = false;
  
  private String strFieldName = null;
  
  /* The field type is only relevant, if the parent table is the pk table
   * and this is a collection, then it is set to Vector as default. If
   * this is a refenece it is of the type of the target class.
   */
  private String strFieldType = null;
  
  private boolean enabled = true;  
  
  boolean pkTableIsParent = true;
  java.util.ArrayList alColumnPairs = new java.util.ArrayList();
  /** Creates a new instance of DBFKReference */
  public DBFKRelation (DBTable pPkTable, DBTable pFkTable, boolean ppkTableIsParent)
  {
    pkTable = pPkTable;
    fkTable = pFkTable;
    pkTableIsParent = ppkTableIsParent;
    if (pkTableIsParent)
    {
      this.strFieldName = "coll" + fkTable.getClassName ();
      this.strFieldType = "java.util.Vector";
    }
    else 
    {
      this.strFieldName = "a" + pkTable.getClassName ();
    }
  }
  
  public boolean isEnabled()
  {
    return this.enabled;
  }
  
  public void setEnabled(boolean b)
  {
    this.enabled = b;
  }
  
  public String getFieldName()
  {
    return this.strFieldName;
  }
  
  public void setFieldName(String s)
  {
    strFieldName = s;
  }  
  
  public String getFieldType()
  {
    if (this.isPkTableParent ()) return this.strFieldType;
    else return this.pkTable.getFQClassName();
  }
  
  public void setFieldType(String s)
  {
    if (!this.isPkTableParent()) throw new java.lang.UnsupportedOperationException("Cannot set Field type on this type of relation");
    strFieldType = s;
  }
  
  public boolean isPkTableParent()
  {
    return this.pkTableIsParent;
  }
    
  public void setAutoRetrieve(boolean b)
  {
    this.bAutoRetrieve = b;
  }
  
  public boolean getAutoRetrieve()
  {
    return this.bAutoRetrieve;
  }

  public void setAutoUpdate(boolean b)
  {
    this.bAutoUpdate = b;
  }
  
  public boolean getAutoUpdate()
  {
    return this.bAutoUpdate;
  }
  
  public void setAutoDelete(boolean b)
  {
    this.bAutoDelete = b;
  }
  
  public boolean getAutoDelete()
  {
    return this.bAutoDelete;
  }

  public DBTable getFKTable()
  {
    return fkTable;
  }
  
  public DBTable getPKTable()
  {
    return pkTable;
  }
  
  public boolean isTreeEnabled()
  {
    if (this.pkTableIsParent) return this.pkTable.isTreeEnabled() && this.isEnabled();
    else return this.fkTable.isTreeEnabled() && this.isEnabled();
  }
  
  
  public void addColumnPair(DBColumn pPkColumn, DBColumn pFkColumn)
  {
    alColumnPairs.add(new Object[]{pPkColumn, pFkColumn});
  }
  
  public java.util.Iterator getColumnPairIterator()
  {
    return alColumnPairs.iterator();
  }
  
  public java.util.Enumeration children ()
  {
    if (pkTableIsParent) return this.fkTable.children();
    else return this.pkTable.children();
  }
  
  public boolean getAllowsChildren ()
  {
    if (pkTableIsParent) return this.fkTable.getAllowsChildren();
    else return this.pkTable.getAllowsChildren();
  }
  
  public javax.swing.tree.TreeNode getChildAt (int param)
  {
    if (pkTableIsParent) return this.fkTable.getChildAt(param);
    else return this.pkTable.getChildAt(param);
  }
  
  public int getChildCount ()
  {
    if (pkTableIsParent) return this.fkTable.getChildCount();
    else return this.pkTable.getChildCount();
  }
  
  public int getIndex (javax.swing.tree.TreeNode treeNode)
  {
    if (pkTableIsParent) return this.fkTable.getIndex(treeNode);
    else return this.pkTable.getIndex(treeNode);
  }
  
  public javax.swing.tree.TreeNode getParent ()
  {
    if (pkTableIsParent) return this.pkTable;
    else return this.fkTable;
  }
  
  public boolean isLeaf ()
  {
    if (pkTableIsParent) return this.fkTable.isLeaf();
    else return this.pkTable.isLeaf();
  }

  public String toString()
  {
    if (pkTableIsParent) return this.fkTable.toString() + " (Collection)";
    else return this.pkTable.toString() + " (Reference)";
  }
  
  
  public Class getPropertySheetClass ()
  {
    return org.apache.ojb.tools.mapping.reversedb.gui.DBFKRelationPropertySheet.class;
  }
  
  private void writeXMLReference(java.io.PrintWriter pw)
  {
    pw.println("  <reference-descriptor");
    pw.println("    name=\"" + this.getFieldName()+ "\"" );
    pw.println("    class-ref=\"" + this.getPKTable().getFQClassName() + "\"" );
    pw.println("    auto-retrieve=\"" + this.getAutoRetrieve()+ "\"" );
    pw.println("    auto-update=\"" + this.getAutoUpdate() + "\"");
    pw.println("    auto-delete=\"" + this.getAutoDelete() + "\">");

    pw.print("    <foreignkey field-ref=\"");
    java.util.Iterator it = this.getColumnPairIterator();
    // TODO: dtd dictate one refid
    // while (it.hasNext()) strReturn += ((DBColumn)((Object[])it.next())[1]).getId() + " ";
    if (it.hasNext()) pw.print(((DBColumn)((Object[])it.next())[1]).getJavaFieldName()) ;
    pw.println("\" />");

    pw.println("  </reference-descriptor>");
  }
  
  private void writeXMLCollection(java.io.PrintWriter pw)
  {
    pw.println("  <collection-descriptor");
    pw.println("    name=\"" + this.getFieldName() + "\"");
    pw.println("    element-class-ref=\"" + this.getFKTable().getFQClassName() + "\"" );
    pw.println("    auto-retrieve=\"" + this.getAutoRetrieve() + "\"" );
    pw.println("    auto-update=\"" + this.getAutoUpdate() + "\"" );
    pw.println("    auto-delete=\"" + this.getAutoDelete() + "\">");
    pw.print("    <inverse-foreignkey field-ref=\"");
    java.util.Iterator it = this.getColumnPairIterator();
    while (it.hasNext()) pw.print(((DBColumn)((Object[])it.next())[1]).getJavaFieldName() + " ");
    pw.println("\"    />");
    pw.println("  </collection-descriptor>");
  }
  
  public String getXML()
  {
        java.io.StringWriter sw = new java.io.StringWriter();
        writeXML(new java.io.PrintWriter(sw));
        return sw.getBuffer().toString();      
  }
  
  public void writeXML(java.io.PrintWriter pw) 
  {
    if (this.isPkTableParent())
    {
      writeXMLCollection(pw);
    }
    else
    {
      writeXMLReference(pw);
    }          
  }
  
  
  public void generateJava (java.io.File aFile, String strHeader, String strFooter) throws java.io.IOException, java.io.FileNotFoundException
  {
    throw new UnsupportedOperationException("Generate Java on DBFKReference is not allowed");
  }
  
  public void setPackage (String packageName)
  {
    throw new UnsupportedOperationException("Set Package on DBFKReference is not allowed");       
  }

  public String getJavaFieldDefinition()
  {
    // Only return a field definition if this Relation and both
    // participating tables are enabled;
    if (this.isTreeEnabled() 
        && this.getFKTable().isTreeEnabled() 
        && this.getPKTable().isTreeEnabled())
    {
      return "  private " + this.getFieldType() + " " + this.getFieldName() + ";";
    }
    else return "";
  }
  
  public String getJavaGetterSetterDefinition()
  {
    if (this.isTreeEnabled() 
        && this.getFKTable().isTreeEnabled() 
        && this.getPKTable().isTreeEnabled())
    {
      String strReturn = "";
      strReturn = "  public " + this.getFieldType() + " get" 
        + this.getFieldName().substring(0,1).toUpperCase() 
        + this.getFieldName().substring(1) + "()" 
        + System.getProperty("line.separator");

      strReturn += "  {" + System.getProperty("line.separator");    
      strReturn += "     return this." + this.getFieldName() + ";" + System.getProperty("line.separator");    
      strReturn += "  }" + System.getProperty("line.separator"); 
      strReturn += "  public void set" 
        + this.getFieldName().substring(0,1).toUpperCase() 
        + this.getFieldName().substring(1) + "(" + this.getFieldType() + " param)" 
        + System.getProperty("line.separator");
      strReturn += "  {" + System.getProperty("line.separator");    
      strReturn += "    this." + this.getFieldName() + " = param;" + System.getProperty("line.separator"); 
      strReturn += "  }" + System.getProperty("line.separator");    
      return strReturn;
    }
    else return "";
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
// Revision 1.8  2004/04/04 23:53:42  brianm
// Fixed initial copyright dates to match cvs repository
//
// Revision 1.7  2004/03/11 18:16:22  brianm
// ASL 2.0
//
// Revision 1.6  2003/12/12 16:37:16  brj
// removed unnecessary casts, semicolons etc.
//
// Revision 1.5  2003/09/10 06:45:00  mpoeschl
// remove duplicated license
//
// Revision 1.4  2003/06/21 10:28:44  florianbruckner
// implement XML generation with PrintWriter; getXML() still works and uses writeXML(java.io.PrintWriter)
// Ids are not used for XML generation.
//
// Revision 1.3  2003/01/28 21:42:53  florianbruckner
// update XML generation
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
