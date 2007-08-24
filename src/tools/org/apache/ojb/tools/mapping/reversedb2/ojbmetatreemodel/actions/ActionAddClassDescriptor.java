package org.apache.ojb.tools.mapping.reversedb2.ojbmetatreemodel.actions;

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
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a>
 * @version $Id: ActionAddClassDescriptor.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class ActionAddClassDescriptor extends javax.swing.AbstractAction
{
    private org.apache.ojb.tools.mapping.reversedb2.ojbmetatreemodel.OjbMetaRootNode rootNode;
    /** Creates a new instance of ActionAddClassDescriptor */
    public ActionAddClassDescriptor(org.apache.ojb.tools.mapping.reversedb2.ojbmetatreemodel.OjbMetaRootNode pRootNode)
    {
        super("Add Class");
        rootNode = pRootNode;
    }

    /** Invoked when an action occurs.
     *
     */
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        System.out.println("Action Command: " + e.getActionCommand());
        System.out.println("Action Params : " + e.paramString());
        System.out.println("Action Source : " + e.getSource());
        System.out.println("Action SrcCls : " + e.getSource().getClass().getName());
        org.apache.ojb.broker.metadata.ClassDescriptor cld =
            new org.apache.ojb.broker.metadata.ClassDescriptor(rootNode.getRepository());
        // cld.setClassNameOfObject("New Class");
        cld.setTableName("New Table");
        rootNode.addClassDescriptor(cld);
    }

}
