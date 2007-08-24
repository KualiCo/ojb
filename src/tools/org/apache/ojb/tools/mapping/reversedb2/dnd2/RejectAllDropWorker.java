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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: RejectAllDropWorker.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
class RejectAllDropWorker implements DropPasteWorkerInterface
{
    
    /** Creates a new instance of RejectAllDropWorker */
    public RejectAllDropWorker ()
    {
    }
    
    public int getAcceptableActions (Component c)
    {
        return DnDWorkerConstants.NONE;
    }
    
    public int getAcceptableActions (Component c, DataFlavor[] flavor)
    {
        return DnDWorkerConstants.NONE;
    }
    
    public DataFlavor getSupportedDataFlavor ()
    {
        return null;
    }
    
    public boolean importData (Component c, Transferable t, int action)
    {
        return false;
    }
    
}
