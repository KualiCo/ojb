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


import java.sql.Types;

/**
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: Utilities.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */

public class Utilities
{
  public static final java.util.Map hmJDBCTypeToName 
      = new java.util.HashMap();
  public static final java.util.Map mJDBCNameToType
      = new java.util.TreeMap();

  public static final java.util.Vector vJDBCTypeNames = new java.util.Vector();

  public static final java.util.HashMap hmJDBCTypeToJavaType 
      = new java.util.HashMap();
  
  public static final java.util.Vector vJavaTypes = new java.util.Vector();  
  
  static
  {
      hmJDBCTypeToName.put(new Integer(Types.ARRAY), "ARRAY");
      hmJDBCTypeToName.put(new Integer(Types.BIGINT), "BIGINT");
      hmJDBCTypeToName.put(new Integer(Types.BINARY), "BINARY");
      hmJDBCTypeToName.put(new Integer(Types.BIT), "BIT");
      hmJDBCTypeToName.put(new Integer(Types.BLOB), "BLOB");
      hmJDBCTypeToName.put(new Integer(Types.CHAR), "CHAR");
      hmJDBCTypeToName.put(new Integer(Types.CLOB), "CLOB");
      hmJDBCTypeToName.put(new Integer(Types.DATE), "DATE");
      hmJDBCTypeToName.put(new Integer(Types.DECIMAL), "DECIMAL");
      hmJDBCTypeToName.put(new Integer(Types.DISTINCT), "DISTINCT");
      hmJDBCTypeToName.put(new Integer(Types.DOUBLE), "DOUBLE");
      hmJDBCTypeToName.put(new Integer(Types.FLOAT), "FLOAT");
      hmJDBCTypeToName.put(new Integer(Types.INTEGER), "INTEGER");
      hmJDBCTypeToName.put(new Integer(Types.JAVA_OBJECT), "OBJECT");
      hmJDBCTypeToName.put(new Integer(Types.LONGVARBINARY), "LONGVARBINARY");
      hmJDBCTypeToName.put(new Integer(Types.LONGVARCHAR), "LONGVARCHAR");
      hmJDBCTypeToName.put(new Integer(Types.NULL), "NULL");
      hmJDBCTypeToName.put(new Integer(Types.NUMERIC), "NUMERIC");
      hmJDBCTypeToName.put(new Integer(Types.OTHER), "OTHER");
      hmJDBCTypeToName.put(new Integer(Types.REAL), "REAL");
      hmJDBCTypeToName.put(new Integer(Types.REF), "REF");
      hmJDBCTypeToName.put(new Integer(Types.SMALLINT), "SMALLINT");
      hmJDBCTypeToName.put(new Integer(Types.STRUCT), "STRUCT");
      hmJDBCTypeToName.put(new Integer(Types.TIME), "TIME");
      hmJDBCTypeToName.put(new Integer(Types.TIMESTAMP), "TIMESTAMP");
      hmJDBCTypeToName.put(new Integer(Types.TINYINT), "TINYINT");
      hmJDBCTypeToName.put(new Integer(Types.VARBINARY), "VARBINARY");
      hmJDBCTypeToName.put(new Integer(Types.VARCHAR), "VARCHAR");

      // Build a map containing the name to typecode mapping
      java.util.Iterator it = hmJDBCTypeToName.entrySet().iterator();
      while (it.hasNext()) 
      {
        java.util.Map.Entry aEntry = (java.util.Map.Entry)it.next();
        mJDBCNameToType.put (aEntry.getValue(), aEntry.getKey());
      }
      
      hmJDBCTypeToJavaType.put(new Integer(Types.ARRAY), "Object[]");
      hmJDBCTypeToJavaType.put(new Integer(Types.BIGINT), "Long");
      hmJDBCTypeToJavaType.put(new Integer(Types.BINARY), "byte[]");
      hmJDBCTypeToJavaType.put(new Integer(Types.BIT), "Byte");
      hmJDBCTypeToJavaType.put(new Integer(Types.BLOB), "byte[]");
      hmJDBCTypeToJavaType.put(new Integer(Types.CHAR), "String");
      hmJDBCTypeToJavaType.put(new Integer(Types.CLOB), "String");
      hmJDBCTypeToJavaType.put(new Integer(Types.DATE), "java.sql.Date");
      hmJDBCTypeToJavaType.put(new Integer(Types.DECIMAL), "Long");
      hmJDBCTypeToJavaType.put(new Integer(Types.DISTINCT), "????");
      hmJDBCTypeToJavaType.put(new Integer(Types.DOUBLE), "Double");
      hmJDBCTypeToJavaType.put(new Integer(Types.FLOAT), "Double");
      hmJDBCTypeToJavaType.put(new Integer(Types.INTEGER), "Long");
      hmJDBCTypeToJavaType.put(new Integer(Types.JAVA_OBJECT), "Object");
      hmJDBCTypeToJavaType.put(new Integer(Types.LONGVARBINARY), "byte[]");
      hmJDBCTypeToJavaType.put(new Integer(Types.LONGVARCHAR), "byte[]");
      hmJDBCTypeToJavaType.put(new Integer(Types.NULL), "Object");
      hmJDBCTypeToJavaType.put(new Integer(Types.NUMERIC), "Long");
      hmJDBCTypeToJavaType.put(new Integer(Types.OTHER), "Object");
      hmJDBCTypeToJavaType.put(new Integer(Types.REAL), "Long");
      hmJDBCTypeToJavaType.put(new Integer(Types.REF), "Object");
      hmJDBCTypeToJavaType.put(new Integer(Types.SMALLINT), "Long");
      hmJDBCTypeToJavaType.put(new Integer(Types.STRUCT), "Object");
      hmJDBCTypeToJavaType.put(new Integer(Types.TIME), "java.sql.Time");
      hmJDBCTypeToJavaType.put(new Integer(Types.TIMESTAMP), "java.sql.Timestamp");
      hmJDBCTypeToJavaType.put(new Integer(Types.TINYINT), "Long");
      hmJDBCTypeToJavaType.put(new Integer(Types.VARBINARY), "byte[]");
      hmJDBCTypeToJavaType.put(new Integer(Types.VARCHAR), "String");      
      
      vJavaTypes.addAll(new java.util.TreeSet(hmJDBCTypeToJavaType.values()));
      java.util.Collections.sort(vJavaTypes);
      
      vJDBCTypeNames.addAll(new java.util.TreeSet(hmJDBCTypeToName.values()));
      java.util.Collections.sort(vJDBCTypeNames);
  }
  
  /** Creates a new instance of Utilities */
  private Utilities ()
  {
  }
  
  public static String getTypeNameFromJDBCType(int jdbcType)
  {
    return (String)Utilities.hmJDBCTypeToName.get(new Integer(jdbcType));
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
// Revision 1.6  2004/04/04 23:53:42  brianm
// Fixed initial copyright dates to match cvs repository
//
// Revision 1.5  2004/03/11 18:16:22  brianm
// ASL 2.0
//
// Revision 1.4  2003/12/12 16:37:16  brj
// removed unnecessary casts, semicolons etc.
//
// Revision 1.3  2003/09/10 06:45:00  mpoeschl
// remove duplicated license
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
