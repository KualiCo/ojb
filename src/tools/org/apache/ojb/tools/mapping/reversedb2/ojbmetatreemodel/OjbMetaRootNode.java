
package org.apache.ojb.tools.mapping.reversedb2.ojbmetatreemodel;

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
 * This represents the root of the repository.xml tree model. It contains
 * the ClassDescriptor objects for this repository and the default JDBC
 * connection.
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a>
 * @version $Id: OjbMetaRootNode.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class OjbMetaRootNode extends OjbMetaTreeNode
{
    private java.util.ArrayList supportedActions = new java.util.ArrayList();

    java.util.HashMap cldToNodes = new java.util.HashMap();

    /** Creates a new instance of OjbMetaRootNode */
    public OjbMetaRootNode(org.apache.ojb.broker.metadata.DescriptorRepository pRepository, OjbMetaDataTreeModel model)
    {
        super(pRepository, model, null);
        supportedActions.add(new org.apache.ojb.tools.mapping.reversedb2.ojbmetatreemodel.actions.ActionAddClassDescriptor(this));
    }

    public boolean getAllowsChildren ()
    {
        return true;
    }

    public Class getPropertyEditorClass ()
    {
        return null;
    }

    public boolean isLeaf ()
    {
        return false;
    }

    /** Get an attribute of this node as Object.
     */
    public Object getAttribute (String strKey)
    {
        return null;
    }

    /** Set an attribute of this node as Object.
     */
    public void setAttribute (String strKey, Object value)
    {
    }

    public OjbMetaClassDescriptorNode getClassDescriptorNodeForClassDescriptor(org.apache.ojb.broker.metadata.ClassDescriptor cld)
    {
    	return (OjbMetaClassDescriptorNode)this.cldToNodes.get(cld);
    }

    /** Purpose of this method is to fill the children of the node. It should
     * replace all children in alChildren (the arraylist containing the children)
     * of this node and notify the TreeModel that a change has occurred.
     */
    protected boolean _load ()
    {
        java.util.Iterator it =
            this.getOjbMetaTreeModel ().getRepository().iterator();
        java.util.ArrayList newChildren = new java.util.ArrayList();

        /* @todo make this work */

//        newChildren.add(new OjbMetaJdbcConnectionDescriptorNode(
//            this.getOjbMetaTreeModel ().getRepository(),
//            this.getOjbMetaTreeModel (),
//            this,
//            this.getOjbMetaTreeModel ().getRepository().getDefaultJdbcConnection()));

        while (it.hasNext())
        {
            org.apache.ojb.broker.metadata.ClassDescriptor cld = (org.apache.ojb.broker.metadata.ClassDescriptor)it.next();
            OjbMetaClassDescriptorNode cldNode =
            	new OjbMetaClassDescriptorNode(this.getOjbMetaTreeModel ().getRepository(),
					                           this.getOjbMetaTreeModel (),
     						                   this, cld);
            cldToNodes.put(cld, cldNode);
            newChildren.add(cldNode);
        }
        java.util.Collections.sort(newChildren);
        this.alChildren = newChildren;
        this.getOjbMetaTreeModel ().nodeStructureChanged(this);
        return true;
    }

    public void addClassDescriptor(org.apache.ojb.broker.metadata.ClassDescriptor cld)
    {
        OjbMetaClassDescriptorNode cldNode =
            new OjbMetaClassDescriptorNode(this.getOjbMetaTreeModel ().getRepository(),
                                           this.getOjbMetaTreeModel (),
                                           this, cld);
        cldToNodes.put(cld, cldNode);
        this.alChildren.add(cldNode);
        this.getOjbMetaTreeModel().nodesWereInserted(this, new int[]{this.alChildren.size()-1});
    }

    /**
     * @see ActionTarget#getActions()
     */
    public java.util.Iterator getActions()
    {
        return supportedActions.iterator();
    }

    /**
     * @see ActionTarget#actionListCacheable()
     */
    public boolean actionListCachable()
    {
        return true;
    }

    /**
     * @see ActionTarget
     */
    public boolean actionListStatic()
    {
        return false;
    }

    /**
     * Return the descriptor object this node is associated with. E.g. if the
     * node displays a class descriptor, the ClassDescriptor describing the class
     * should be returned. Used for creating a Transferable. Null in this case
     * because the root doesn't have any associated objects.
     */
    public Object getAssociatedDescriptor()
    {
        return null;
    }

}
