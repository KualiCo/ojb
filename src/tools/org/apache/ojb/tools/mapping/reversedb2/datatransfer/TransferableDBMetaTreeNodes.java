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

package org.apache.ojb.tools.mapping.reversedb2.datatransfer;
import java.awt.datatransfer.*;
import org.apache.ojb.tools.mapping.reversedb2.dbmetatreemodel.*;

/**
 *
 * @author  Administrator
 */
public class TransferableDBMetaTreeNodes implements java.awt.datatransfer.Transferable, java.io.Serializable
{
    public static DataFlavor DBMETATABLENODE_FLAVOR_REMOTE;
    public static DataFlavor DBMETATABLENODE_FLAVOR_LOCAL;
//    public static DataFlavor DBMETATABLENODE_FLAVOR_SERIALIZED;
//        = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + "; class=org.apache.ojb.tools.mapping.reversedb2.dbmetatreemodel.ReverseDbTreeNode[]", "Reverse Engineered Database Objects");
//          = new DataFlavor(org.apache.ojb.tools.mapping.reversedb2.dbmetatreemodel.ReverseDbTreeNode[].class, "DB");
//          = new DataFlavor(DataFlavor.javaRemoteObjectMimeType + ";class=org.apache.ojb.tools.mapping.reversedb2.dbmetatreemodel.ReverseDbTreeNode[]");
    
    // private static final DataFlavor[] _flavors = {DBMETABLENODE_FLAVOR};
    private static DataFlavor[] _flavors;    
    
    static
    {
        try
        {
            // System.err.println(org.apache.ojb.tools.mapping.reversedb2.dbmetatreemodel.ReverseDbTreeNode[].class.getName());
            // DBMETATABLENODE_FLAVOR_REMOTE     = new DataFlavor(org.apache.ojb.tools.mapping.reversedb2.datatransfer.TransferableDBMetaTreeNodes.ReverseDbTreeNodesContainer.class, "OJB Reversedb Database objects");
            DBMETATABLENODE_FLAVOR_REMOTE     = new DataFlavor(DataFlavor.javaRemoteObjectMimeType + ";class=org.apache.ojb.tools.mapping.reversedb2.datatransfer.TransferableDBMetaTreeNodes$ReverseDbTreeNodesContainer", "OJB Reversedb Database objects");
            DBMETATABLENODE_FLAVOR_LOCAL      = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType  + ";class=org.apache.ojb.tools.mapping.reversedb2.datatransfer.TransferableDBMetaTreeNodes$ReverseDbTreeNodesContainer");
            // DBMETATABLENODE_FLAVOR_SERIALIZED = new DataFlavor(DataFlavor.javaSerializedObjectMimeType + ";class=org.apache.ojb.tools.mapping.reversedb2.datatransfer.TransferableDBMetaTreeNodes$ReverseDbTreeNodesContainer");
            // DBMETATABLENODE_FLAVOR_SERIALIZED = DBMETATABLENODE_FLAVOR_REMOTE;
            _flavors = new DataFlavor[] {DBMETATABLENODE_FLAVOR_LOCAL, DBMETATABLENODE_FLAVOR_REMOTE}; //, DBMETATABLENODE_FLAVOR_SERIALIZED};
        }
        catch (ClassNotFoundException cce)
        {
            DBMETATABLENODE_FLAVOR_REMOTE     = null;
            DBMETATABLENODE_FLAVOR_LOCAL      = null;
//            DBMETATABLENODE_FLAVOR_SERIALIZED = null;
            _flavors = null;
            cce.printStackTrace();
        }
    }
    
// org.apache.ojb.tools.mapping.reversedb2.dbmetatreemodel.ReverseDbTreeNode[]


    /** Creates a new instance of TransferableDBMetaTable */
    
    // private ReverseDbTreeNode[] selectedNodes;
    private ReverseDbTreeNodesContainer nodesContainer = new ReverseDbTreeNodesContainer();
    
    public TransferableDBMetaTreeNodes (ReverseDbTreeNode[] pselectedNodes)
    {
        nodesContainer.selectedNodes = pselectedNodes;
    }
    
    public Object getTransferData(java.awt.datatransfer.DataFlavor dataFlavor)
        throws java.awt.datatransfer.UnsupportedFlavorException, java.io.IOException
    {
        if (   dataFlavor.equals(DBMETATABLENODE_FLAVOR_REMOTE) 
            || dataFlavor.equals(DBMETATABLENODE_FLAVOR_LOCAL)
           )
//            || dataFlaCvor.isMimeTypeEqual(DBMETATABLENODE_FLAVOR_SERIALIZED))
            return nodesContainer.selectedNodes;
        else
            throw new UnsupportedFlavorException(dataFlavor);
    }
    
    public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors()
    {
        return _flavors;
    }
    
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        boolean b = java.util.Arrays.asList(_flavors).contains(flavor);
        System.err.println("TransferableDBMetaTreeNodes.isDataFlavorSupported " + flavor + " returns " + b);
        return java.util.Arrays.asList(_flavors).contains(flavor);
    }        
    
    public static class ReverseDbTreeNodesContainer
        implements java.io.Serializable
    {
        private ReverseDbTreeNode[] selectedNodes;
    }
}
