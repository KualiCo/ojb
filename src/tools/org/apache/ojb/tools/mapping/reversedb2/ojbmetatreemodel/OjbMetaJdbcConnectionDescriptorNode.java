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

package org.apache.ojb.tools.mapping.reversedb2.ojbmetatreemodel;

import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;

/**
 *
 * @author  Administrator
 */
public class OjbMetaJdbcConnectionDescriptorNode extends OjbMetaTreeNode
{

    /** Key for accessing the schema name in the attributes Map */
    public static final String ATT_DBALIAS = "Database Alias";
    public static final String ATT_DBMS    = "DBMS";
    public static final String ATT_DATASOURCE_NAME = "Datasource Name";
    public static final String ATT_DRIVER = "Driver";
    public static final String ATT_DESCRIPTOR_PBKEY = "PBKey";
    public static final String ATT_JDBC_LEVEL = "JDBC Level";
    public static final String ATT_PASSWORD = "Password";
    public static final String ATT_PROTOCOL = "Protocol";
    public static final String ATT_SUBPROTOCOL = "Sub Protocol";
    public static final String ATT_USERNAME = "Username";

    private static java.util.ArrayList supportedActions = new java.util.ArrayList();

    private JdbcConnectionDescriptor connDescriptor;
    /** Creates a new instance of OjbMetaJdbcConnectionDescriptorNode */
    public OjbMetaJdbcConnectionDescriptorNode(DescriptorRepository pRepository,
                                       OjbMetaDataTreeModel pTreeModel,
                                       OjbMetaTreeNode pparent,
                                       JdbcConnectionDescriptor pConnDescriptor)
    {
        super(pRepository, pTreeModel, pparent);
        this.connDescriptor = pConnDescriptor;
    }

    /** Purpose of this method is to fill the children of the node. It should
     * replace all children in alChildren (the arraylist containing the children)
     * of this node and notify the TreeModel that a change has occurred.
     */
    protected boolean _load ()
    {
        return true;
    }

    public boolean getAllowsChildren ()
    {
        return false;
    }

    public Object getAttribute (String key)
    {
        if (key.equals(ATT_DBALIAS))
        {
            return connDescriptor.getDbAlias();
        }
        else if (key.equals(ATT_DATASOURCE_NAME))
        {
            return connDescriptor.getDatasourceName();
        }
        else if (key.equals(ATT_DRIVER))
        {
            return connDescriptor.getDriver();
        }
        else if (key.equals(ATT_JDBC_LEVEL ))
        {
            return new Double(connDescriptor.getJdbcLevel());
        }
        else if (key.equals(ATT_DESCRIPTOR_PBKEY))
        {
            return connDescriptor.getPBKey();
        }
        else if (key.equals(ATT_PASSWORD))
        {
            return connDescriptor.getPassWord();
        }
        else if (key.equals(ATT_PROTOCOL))
        {
            return connDescriptor.getProtocol();
        }
        else if (key.equals(ATT_SUBPROTOCOL))
        {
            return connDescriptor.getSubProtocol();
        }
        else if (key.equals(ATT_USERNAME))
        {
            return connDescriptor.getUserName();
        }
        else if (key.equals(ATT_DBMS))
        {
            return connDescriptor.getDbms();
        }
        else
        {
            return super.getAttribute(key);
        }
    }

    public Class getPropertyEditorClass ()
    {
        return org.apache.ojb.tools.mapping.reversedb2.propertyEditors.JPnlPropertyEditorOJBMetaJdbcConnectionDescriptor.class;
    }

    public boolean isLeaf ()
    {
        return true;
    }

    public void setAttribute (String key, Object value)
    {
        if (key.equals(ATT_DBALIAS))
        {
            Object oldValue = connDescriptor.getDbAlias();
            connDescriptor.setDbAlias(value.toString());
            propertyChangeDelegate.firePropertyChange(ATT_DBALIAS, oldValue, value);
            // We need to send this event because the DBAlias is part of the treenode label...
            this.getOjbMetaTreeModel().nodeChanged(this);
        }
        else if (key.equals(ATT_DATASOURCE_NAME))
        {
            Object oldValue = connDescriptor.getDatasourceName();
            connDescriptor.setDatasourceName(value.toString());
            propertyChangeDelegate.firePropertyChange(ATT_DATASOURCE_NAME, oldValue, value);
        }
        else if (key.equals(ATT_DRIVER))
        {
            Object oldValue = connDescriptor.getDatasourceName();
            connDescriptor.setDriver(value.toString());
            propertyChangeDelegate.firePropertyChange(ATT_DRIVER, oldValue, value);
        }
        else if (key.equals(ATT_DESCRIPTOR_PBKEY))
        {
/* Readonly */
            propertyChangeDelegate.firePropertyChange(ATT_DESCRIPTOR_PBKEY, connDescriptor.getPBKey(), connDescriptor.getPBKey());
        }
        else if (key.equals(ATT_JDBC_LEVEL))
        {
            Object oldValue = new Double(connDescriptor.getJdbcLevel());
            connDescriptor.setJdbcLevel(value.toString());
            propertyChangeDelegate.firePropertyChange(ATT_JDBC_LEVEL, oldValue, new Double(connDescriptor.getJdbcLevel()));
        }
        else if (key.equals(ATT_PASSWORD))
        {
            Object oldValue = connDescriptor.getPassWord();
            connDescriptor.setPassWord(value.toString());
            propertyChangeDelegate.firePropertyChange(ATT_PASSWORD, oldValue, value);
        }
        else if (key.equals(ATT_PROTOCOL))
        {
            Object oldValue = connDescriptor.getProtocol();
            connDescriptor.setProtocol(value.toString());
            propertyChangeDelegate.firePropertyChange(ATT_PROTOCOL, oldValue, value);
        }
        else if (key.equals(ATT_SUBPROTOCOL))
        {
            Object oldValue = connDescriptor.getSubProtocol();
            connDescriptor.setSubProtocol(value.toString());
            propertyChangeDelegate.firePropertyChange(ATT_SUBPROTOCOL, oldValue, value);
        }
        else if (key.equals(ATT_USERNAME))
        {
            Object oldValue = connDescriptor.getUserName();
            connDescriptor.setUserName(value.toString());
            propertyChangeDelegate.firePropertyChange(ATT_USERNAME, oldValue, value);
        }
        else if (key.equals(ATT_DBMS))
        {
            Object oldValue = connDescriptor.getDbms();
            connDescriptor.setDbms(value.toString());
            propertyChangeDelegate.firePropertyChange(ATT_DBMS, oldValue, value);
        }
        else
        {
            super.setAttribute(key, value);
        }
    }

    public String toString()
    {
        return "ConnectionDescriptor: " + connDescriptor.getDbAlias();
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

    public boolean actionListStatic()
    {
        return true;
    }

    /**
     * Return the descriptor object this node is associated with. E.g. if the
     * node displays a class descriptor, the ClassDescriptor describing the class
     * should be returned. Used for creating a Transferable.
     */
    public Object getAssociatedDescriptor()
    {
        return connDescriptor;
    }

}
