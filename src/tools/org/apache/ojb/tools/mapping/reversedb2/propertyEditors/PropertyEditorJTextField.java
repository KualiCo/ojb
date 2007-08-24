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
 * This is a JTextfield implementing the PropertyEditorComponentInterface. You
 * just have to specify the editor Key (which is the property key you whish
 * to display from the target), everything else is being handeled within this
 * class.
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: PropertyEditorJTextField.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class PropertyEditorJTextField extends javax.swing.JTextField
    implements PropertyEditorComponentInterface,
               java.beans.PropertyChangeListener,
               java.awt.event.KeyListener,
               java.awt.event.FocusListener,
               java.awt.event.ActionListener
               
{
    private String editorKey;
    private PropertyEditorTarget aTarget;
    
    /* Only here to be compliant with JavaBeans spec, which is required for most
     * graphical editors */
    public PropertyEditorJTextField()
    {
        super();
        this.addKeyListener(this);
        this.addActionListener(this);
        this.addFocusListener(this);
    } 
    
    public String getEditorKey()
    {
        return editorKey;
    }
    
    public void setEditorKey(String newKey)
    {
        editorKey = newKey;
        setEditorTarget(this.aTarget);
    }
    
    public void setValue(String key, Object value)
    {
        if (editorKey != null && editorKey.equals(key))
        {
            setValue(value);
        }
    }
    
    public void setValue(Object value)
    {
        if (value == null)
        {
            setText("");
        }
        else
        {
            setText(value.toString());
        }
    }
    
    public void setEditorTarget(PropertyEditorTarget paTarget)
    {
        if (aTarget != null) aTarget.removePropertyChangeListener(this);
        if (paTarget != null)
        {
            aTarget = paTarget;
            aTarget.addPropertyChangeListener(editorKey, this);
            setValue(aTarget.getAttribute(editorKey));
        }
        
    }
    
    /** This method gets called when a bound property is changed.
     * @param evt A PropertyChangeEvent object describing the event source
     *   	and the property that has changed.
     *
     */
    public void propertyChange(java.beans.PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(this.editorKey))
        {
            this.setValue(evt.getNewValue());
        }
    }
    
    public void actionPerformed(java.awt.event.ActionEvent evt)
    {
        if (this.aTarget != null)
            this.aTarget.setAttribute(this.editorKey, getText());
    }

    public void focusLost(java.awt.event.FocusEvent evt)
    {
        if (this.aTarget != null)
            this.aTarget.setAttribute(this.editorKey, getText());
    }
    public void keyPressed(java.awt.event.KeyEvent evt)
    {
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE && aTarget != null)
        {
            setValue(aTarget.getAttribute(this.editorKey));
        }        
    }
    
    /** Invoked when a component gains the keyboard focus.
     *
     */
    public void focusGained(java.awt.event.FocusEvent e)
    {
    }
    
    /** Invoked when a key has been released.
     * See the class description for {@link KeyEvent} for a definition of
     * a key released event.
     *
     */
    public void keyReleased(java.awt.event.KeyEvent e)
    {
    }
    
    /** Invoked when a key has been typed.
     * See the class description for {@link KeyEvent} for a definition of
     * a key typed event.
     *
     */
    public void keyTyped(java.awt.event.KeyEvent e)
    {
    }
    
}
