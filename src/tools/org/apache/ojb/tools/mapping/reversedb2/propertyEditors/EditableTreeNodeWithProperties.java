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
 * This class provides a basic implementation of a PropertyEditor and a TreeNode.
 * This is the typical application of the propertyEditor framework, you will usually
 * have a tree or a table with an overview of possibly editable objects and a panel
 * with a detailed view on the object.
 *
 * The properties are maintained in a HashMap, setProperty sets these properties,
 * getProperty retrieves them. You may want to define public final keys for your
 * properties in order to have uniform access to them from all editors.
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: EditableTreeNodeWithProperties.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public abstract class EditableTreeNodeWithProperties 
    implements javax.swing.tree.TreeNode, 
    org.apache.ojb.tools.mapping.reversedb2.propertyEditors.PropertyEditorTarget,
    java.io.Serializable
{
	static final long serialVersionUID = -8720549176372985715L;
    private java.util.HashMap hmAttributes = new java.util.HashMap();
    
    protected java.beans.PropertyChangeSupport propertyChangeDelegate = new
        java.beans.PropertyChangeSupport(this);

    /** Creates a new instance of EditableTreeNodeWithProperties */
    public EditableTreeNodeWithProperties()
    {
    }
    
    /**
     * Add a new PropertyChangeListener to this node. This functionality has
     * been borrowed from the java.beans package, though this class has 
     * nothing to do with a bean
     */
    public void addPropertyChangeListener (java.beans.PropertyChangeListener listener)
    {
        this.propertyChangeDelegate.addPropertyChangeListener(listener);
    }

    /**
     * Add a new PropertyChangeListener to this node for a specific property. 
     * This functionality has
     * been borrowed from the java.beans package, though this class has 
     * nothing to do with a bean
     */
    public void addPropertyChangeListener (String propertyName, java.beans.PropertyChangeListener listener)
    {
        this.propertyChangeDelegate.addPropertyChangeListener(propertyName, listener);
    }
    
    /**
     * Remove a PropertyChangeListener from this node. This functionality has
     * been borrowed from the java.beans package, though this class has 
     * nothing to do with a bean
     */
    public void removePropertyChangeListener (java.beans.PropertyChangeListener listener)
    {
        this.propertyChangeDelegate.removePropertyChangeListener (listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property from this node. 
     * This functionality has. Please note that the listener this does not remove
     * a listener that has been added without specifying the property it is 
     * interested in.
     */    
    public void removePropertyChangeListener (String propertyName, java.beans.PropertyChangeListener listener)
    {
        this.propertyChangeDelegate.removePropertyChangeListener(propertyName, listener);
    }
    
    /**
     * Get an attribute of this node as Object. This method is backed by
     * a HashMap, so all rules of HashMap apply to this method.
     */
    public Object getAttribute(String strKey)
    {
        return hmAttributes.get(strKey);
    }
    
    /**
     * Set an attribute of this node as Object. This method is backed by
     * a HashMap, so all rules of HashMap apply to this method.
     * Fires a PropertyChangeEvent.
     */
    public void setAttribute(String strKey, Object value)
    {
        this.propertyChangeDelegate.firePropertyChange(strKey,
            hmAttributes.put(strKey, value), value);
    }    
    
}
