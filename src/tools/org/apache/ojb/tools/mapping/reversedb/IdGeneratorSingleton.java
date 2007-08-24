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

import java.util.HashMap;

/**
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: IdGeneratorSingleton.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 * @deprecated since 2003-06-21
 */
public class IdGeneratorSingleton
{
  private static IdGeneratorSingleton singleton = new IdGeneratorSingleton();

  /** Creates a new instance of IdGeneratorSingleton */
  
  private HashMap hmIds = new HashMap();
  
  private IdGeneratorSingleton ()
  {
  }
  
  private int _getId(String type, String instance)
  {
    HashMap hmTypeId = (HashMap)hmIds.get(type);
    if (hmTypeId == null)
    {
      hmTypeId = new HashMap();
      hmIds.put(type, hmTypeId);
    }
    Integer storedId  = (Integer)hmTypeId.get(instance);
    int id;
    if (storedId == null)
    {
      id = 0;
    }
    else id = storedId.intValue()+1;
    hmTypeId.put(instance, new Integer(id));
    return id;
  }
  
  public static int getId(String type, String instance)
  {
    return singleton._getId (type, instance);
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
// Revision 1.5  2004/04/04 23:53:42  brianm
// Fixed initial copyright dates to match cvs repository
//
// Revision 1.4  2004/03/11 18:16:22  brianm
// ASL 2.0
//
// Revision 1.3  2003/06/21 10:36:40  florianbruckner
// remove double license header, deprecate class
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
