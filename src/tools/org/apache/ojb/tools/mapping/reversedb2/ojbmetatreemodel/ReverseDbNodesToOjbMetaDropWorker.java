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

import org.apache.ojb.tools.mapping.reversedb2.dbmetatreemodel.*;
import org.apache.ojb.tools.mapping.reversedb2.datatransfer.*;
import org.apache.ojb.tools.mapping.reversedb2.dnd2.*;
import org.apache.ojb.broker.util.ClassHelper;

/**
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a>
 * @version $Id: ReverseDbNodesToOjbMetaDropWorker.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class ReverseDbNodesToOjbMetaDropWorker implements DropPasteWorkerInterface
{
    /** Creates a new instance of ReverseDbNodesDropWorker */
    public ReverseDbNodesToOjbMetaDropWorker()
    {
    }

    public int getAcceptableActions (java.awt.Component c)
    {
        return DnDWorkerConstants.DRAG_MOVE | DnDWorkerConstants.DRAG_COPY ;
    }

    public int getAcceptableActions (java.awt.Component c, java.awt.datatransfer.DataFlavor[] flavor)
    {
        for (int j = 0; j < flavor.length; j++)
        {
            System.err.println("ReverseDbNodesToOjbMetaDropWorker.getAcceptableActions() flavor " + flavor[j] + " is " + flavor[j].isMimeTypeEqual(TransferableDBMetaTreeNodes.DBMETATABLENODE_FLAVOR_REMOTE));
            if (flavor[j].equals (TransferableDBMetaTreeNodes.DBMETATABLENODE_FLAVOR_REMOTE))
                return DnDWorkerConstants.DRAG_MOVE | DnDWorkerConstants.DRAG_COPY;
        }
        return DnDWorkerConstants.NONE;
    }

    public java.awt.datatransfer.DataFlavor getSupportedDataFlavor ()
    {
        return TransferableDBMetaTreeNodes.DBMETATABLENODE_FLAVOR_REMOTE;
    }

    private java.util.Enumeration getTableNodes(java.util.Enumeration nodes)
    {
        java.util.Vector tableVector = new java.util.Vector();
        while(nodes.hasMoreElements())
        {
            ReverseDbTreeNode aNode = (ReverseDbTreeNode)nodes.nextElement();
            if (aNode instanceof DBMetaRootNode
                // || aNode instanceof DBMetaSchemaNode
                || aNode instanceof DBMetaCatalogNode)
            {
                java.util.Enumeration e = getTableNodes(aNode.children());
                while (e.hasMoreElements()) tableVector.add(e.nextElement());
            }
            else if (aNode instanceof DBMetaSchemaNode)
            {
                java.util.Enumeration e = aNode.children();
                while (e.hasMoreElements()) tableVector.add(e.nextElement());
            }
            else if (aNode instanceof DBMetaTableNode)
            {
                return java.util.Collections.enumeration(java.util.Collections.singletonList(aNode));
            }
        }
        return tableVector.elements();

    }

    public boolean importData (java.awt.Component c, java.awt.datatransfer.Transferable t, int action)
    {
        if (t.isDataFlavorSupported(TransferableDBMetaTreeNodes.DBMETATABLENODE_FLAVOR_REMOTE) &&
            c instanceof javax.swing.JTree &&
            ((javax.swing.JTree)c).getModel() instanceof OjbMetaDataTreeModel)
        {
            OjbMetaDataTreeModel treeModel = (OjbMetaDataTreeModel)((javax.swing.JTree)c).getModel();
            OjbMetaRootNode rootNode = (OjbMetaRootNode)treeModel.getRoot();
            ReverseDbTreeNode[] nodes;
            try
            {
                nodes = (ReverseDbTreeNode[])t.getTransferData(TransferableDBMetaTreeNodes.DBMETATABLENODE_FLAVOR_REMOTE);
            }
            catch (java.awt.datatransfer.UnsupportedFlavorException ufex)
            {
                ufex.printStackTrace();
                return false;
            }
            catch (java.io.IOException ioex)
            {
                ioex.printStackTrace();
                return false;
            }
            java.util.Enumeration e = getTableNodes(java.util.Collections.enumeration(java.util.Arrays.asList(nodes)));
            while (e.hasMoreElements())
            {
                Object o = e.nextElement();
                if (o instanceof DBMetaTableNode)
                {
                    DBMetaTableNode dbTable = (DBMetaTableNode)o;
                    System.err.println("Adding " + dbTable);
                    org.apache.ojb.broker.metadata.ClassDescriptor cld =
                        new org.apache.ojb.broker.metadata.ClassDescriptor(treeModel.getRepository());
                    cld.setTableName(dbTable.getTableName());
                    try
                    {
                        cld.setClassOfObject(ClassHelper.getClass(dbTable.getTableName()));
                    }
                    catch (ClassNotFoundException e1)
                    {
                        e1.printStackTrace();
                    }
                    treeModel.getRepository().put(cld.getClassNameOfObject(), cld);
                    rootNode.addClassDescriptor(cld);
                }
            }
            return true;
        }
        else
        {
            System.err.println("Cannot import data");
            return false;
        }
    }
}
