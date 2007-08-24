package org.apache.ojb.tools.swing;

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
 * @version $Id: SortingComboBoxModel.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class SortingComboBoxModel extends javax.swing.AbstractListModel
  implements javax.swing.MutableComboBoxModel
{

  java.util.List aList;
  Object selectedItem;

  /** Creates a new instance of SortingComboBoxModel */
  public SortingComboBoxModel ()
  {
    super();
  }

  public SortingComboBoxModel(java.util.List l)
  {
    super();
    aList = l;
    this.setSelectedItem(getElementAt(0));
  }

  public Object getElementAt (int param)
  {
//    System.out.println("getElementAt(" + param + ")=" + aList.get(param));
    return aList.get(param);
  }

  public int getSize ()
  {
//    System.out.println("getSize()=" + aList.size());
    return aList.size();
  }


  public void addElement (Object obj)
  {
//    System.out.println("addElement(" + obj + ")");
    aList.add(obj);
    java.util.Collections.sort(aList);
    this.fireContentsChanged(this, 0, aList.size());
  }

  public Object getSelectedItem()
  {
//    System.out.println("getSelectedItem: " + selectedItem);
    return this.selectedItem;
  }

  public void insertElementAt (Object obj, int param)
  {
//    System.out.println("insertElement(" + obj + ", " + param +  ")");
   aList.add(param, obj);
   java.util.Collections.sort(aList);
   this.fireContentsChanged(this, -1, aList.size());
  }

  public void removeElement (Object obj)
  {
    aList.remove(obj);
  }

  public void removeElementAt (int param)
  {
    aList.remove(param);
  }

  public void setSelectedItem (Object obj)
  {
    if ( (selectedItem != null && !selectedItem.equals(obj))
        || (selectedItem == null && obj != null))
    {
      selectedItem = obj;
      fireContentsChanged(this, -1, -1);
    }
// System.out.println("setSelectedItem: " + selectedItem);
  }

  public int getIndexOf(Object o)
  {
//    System.out.println("getIndexOf(" + o + ") = " + aList.indexOf(o));
    return aList.indexOf(o);
  }
}
