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

package org.apache.ojb.tools.mapping.reversedb2;

/**
 *
 * @author  Administrator
 */
public class ExtendedProperties extends java.util.Properties
{
    
    private String strFilename;
    
    /** Creates a new instance of ExtendedProperties */
    public ExtendedProperties(String pstrFilename)
    {
        this.strFilename = pstrFilename;
      try
      {
        load(new java.io.FileInputStream(System.getProperty("user.home") +
            System.getProperty("file.separator") + strFilename));
      }
      catch (java.io.IOException ioex)
      {
      }                        
    }
    
    public synchronized void storeProperties(String comment)
    {
        try
        {
            store(new java.io.FileOutputStream(System.getProperty("user.home") +
                System.getProperty("file.separator") + strFilename), comment);
        }
        catch (Throwable t)
        {
            // Report nothing
        }    
    }
    
    public synchronized void clear()
    {
        super.clear();
    }
    
    public synchronized Object put(Object key, Object value)
    {
        return super.put(key, value);
    }
    
    public synchronized void putAll(java.util.Map t)
    {
        super.putAll(t);
    }
    
    public synchronized Object remove(Object key)
    {
        return super.remove(key);
    }
    
    public synchronized Object setProperty(String key, String value)
    {
        return super.setProperty(key,value);
    }
    
    protected void finalize()
    {
        System.out.println(this.getClass().getName() + " finalized");
    }
}
