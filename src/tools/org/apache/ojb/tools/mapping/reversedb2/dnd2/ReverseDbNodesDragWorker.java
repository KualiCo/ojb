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

import java.awt.Component;
import java.awt.datatransfer.Transferable;
import java.awt.Image;
import org.apache.ojb.tools.mapping.reversedb2.dbmetatreemodel.*;
import org.apache.ojb.tools.mapping.reversedb2.datatransfer.*;

/**
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: ReverseDbNodesDragWorker.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class ReverseDbNodesDragWorker implements DragCopyCutWorkerInterface
{
    
    /** Creates a new instance of ReverseDbNodesDragWorker */
    public ReverseDbNodesDragWorker ()
    {
    }
    
    public void exportDone (Component c, int action)
    {
        System.err.println("exportDone");
    }
    
    public void exportStarted (Component c, int action)
    {
        System.err.println("exportStarted");
    }
    
    public int getAcceptableActions (Component c)
    {
        return DnDWorkerConstants.DRAG_COPY | DnDWorkerConstants.DRAG_LINK;
    }
    
    public Image getDragImage (Component c, Transferable t, int action)
    {
        return null;
    }
    
    public Transferable getTransferable (Component c)
    {
        System.err.println("getTransferable()");
        try
        {
            if (c instanceof javax.swing.JTree)
            {
                System.err.println("   e is a JTree");
                javax.swing.JTree jtree = (javax.swing.JTree)c;
                if (jtree.getModel() instanceof DatabaseMetaDataTreeModel)
                {
                    System.err.println("    and has a DatabaseMetaDataTreeModel");
                    ReverseDbTreeNode[] selectedNodes = 
                        new ReverseDbTreeNode[jtree.getSelectionCount()];
                    for (int i = 0; jtree.getSelectionPaths() != null && i < jtree.getSelectionPaths().length; i++)
                    {
                        System.err.println("   adding Node" + jtree.getSelectionPaths()[i].getLastPathComponent());
                        selectedNodes[i] = (ReverseDbTreeNode) jtree.getSelectionPaths()[i].getLastPathComponent();
                    }
                    return new TransferableDBMetaTreeNodes(selectedNodes);
                }
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        System.err.println("   returning null");
        return null;        
    }
    
}
