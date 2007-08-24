package org.apache.ojb.tools.mapping.reversedb2.dnd2;

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

/**
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: ReverseDbNodesDropWorker.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class ReverseDbNodesDropWorker implements DropPasteWorkerInterface
{
    /** Creates a new instance of ReverseDbNodesDropWorker */
    public ReverseDbNodesDropWorker ()
    {
    }
   
    public int getAcceptableActions (java.awt.Component c)
    {
        return DnDWorkerConstants.DRAG_MOVE | DnDWorkerConstants.DRAG_COPY ;
    }
    
    public int getAcceptableActions (java.awt.Component c, java.awt.datatransfer.DataFlavor[] flavor)
    {
        for (int j = 0; j < flavor.length; j++)
            if (flavor[j].isMimeTypeEqual (TransferableDBMetaTreeNodes.DBMETATABLENODE_FLAVOR_REMOTE))
                return DnDWorkerConstants.DRAG_MOVE | DnDWorkerConstants.DRAG_COPY;
        return DnDWorkerConstants.NONE;
    }
    
    public java.awt.datatransfer.DataFlavor getSupportedDataFlavor ()
    {
        return TransferableDBMetaTreeNodes.DBMETATABLENODE_FLAVOR_REMOTE;
    }
    
    public boolean importData (java.awt.Component c, java.awt.datatransfer.Transferable t, int action)
    {
        if (t.isDataFlavorSupported(TransferableDBMetaTreeNodes.DBMETATABLENODE_FLAVOR_REMOTE))
        {
            try
            {
                ReverseDbTreeNode[] nodes = 
                    (ReverseDbTreeNode[])t.getTransferData(TransferableDBMetaTreeNodes.DBMETATABLENODE_FLAVOR_REMOTE);
                for (int i = 0; i < nodes.length; i++)
                    System.err.println("Transfered: " + nodes[i]);
                return true;
            }
            catch (Throwable throwable)
            {
                throwable.printStackTrace();
                return false;
            }
        }
        else
        {
            return false;
        }
    }
}
