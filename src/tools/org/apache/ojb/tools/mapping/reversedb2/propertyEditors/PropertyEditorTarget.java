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

import java.beans.PropertyChangeListener;


/**
 * This interface specifies that the class has editable properties for a 
 * property editor. The typical application for this is a tree where you
 * can select objects and edit settings in another panel.
 *
 * The properties of the target are exposed by getAttribute() and setAttribute().
 * If you want to monitor a property for changes, you can register a PropertyChangeListener
 * either for all properties or a specific one.
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: PropertyEditorTarget.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public interface PropertyEditorTarget
{
    /**
     * Return the property editor class for this PropertyEditorTarget. Depending
     * on the GUI implementation this could for example be an extension of 
     * JPanel. The Property editor is responsible for the layout, so the property
     * editor has to know which properties this target has.
     */
    public Class getPropertyEditorClass();
    
    /**
     * Returns a property
     */
    public Object getAttribute(String key);
    
    /**
     * Stores a property
     */
    public void setAttribute(String key, Object value);
    
    public void addPropertyChangeListener (PropertyChangeListener listener);
    
    public void addPropertyChangeListener (String propertyName, PropertyChangeListener listener);
    
    public void removePropertyChangeListener (PropertyChangeListener listener);
    
    public void removePropertyChangeListener (String propertyName, PropertyChangeListener listener);    
}
