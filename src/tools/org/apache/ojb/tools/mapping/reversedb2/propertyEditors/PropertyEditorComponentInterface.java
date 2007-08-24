package org.apache.ojb.tools.mapping.reversedb2.propertyEditors;

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
 * This is a generic interface to be used together with PropertyEditor. All components 
 * implementing this interface have to handle setting and getting of values from
 * the PropertyEditorTarget themselves. If they want to get notified about
 * changes in certain properties, they have to register a PropertyChangeListener 
 * with the target.
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: PropertyEditorComponentInterface.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public interface PropertyEditorComponentInterface
{

    public String getEditorKey();
    
    public void setValue(String key, Object value);
    
    public void setValue(Object value);
    
    public void setEditorTarget(PropertyEditorTarget aTarget);
    
}
