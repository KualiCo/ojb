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
 * This interface is used by DropTargetHelper to determine whether a drop
 * can be performed and to import the  data from the transferable into the
 * model of the component.
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DropPasteWorkerInterface.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */

public interface DropPasteWorkerInterface
{
 
    /**
     * @return the DataFlavor this Worker is going to accept.
     */
    public DataFlavor getSupportedDataFlavor();
    
    /** Returns a bitmaks of acceptable actions for the supplied Component
     * and DataFlavor. If more than one DataFlavor is queried, the resulting
     * bitmask should include all possible actions for all flavors.
     * @return a bitmask of actions this Worker can process with the supplied
     * flavors.
     * @param c The component that is the possible drop target.
     * @param flavor The DataFlavours that are associate with the drop action
     */
    int getAcceptableActions(Component c, DataFlavor[] flavor);
    
    /** Returns a bitmask of acceptable actions for this component. As this
     * method doesn't provide the DataFlavors in this action, all possible
     * acceptable actions should be returned. All possible actions are defined
     * in DnDWorkerConstants
     * @return a bitmask of actions this Worker capable of.
     * @param c The component where the drop could occur.
     */
    int getAcceptableActions(Component c);
    
    /** This is the method that is doing the real work. You get the Component
     * where the drop has occurred, the Transferable with the data and the
     * requested action.
     * @return true if the transfer was successful, false if not. If there
     * are more than one Workers are registered with the helper and this
     * method returns false, the next helper is asked to do the import. If true
     * is returned, the action is supposed to be complete and no other worker
     * will be asked. So be careful what you return here, if you return false,
     * no modification to the target model should have happened.
     * @param c The component where the drop has occurred
     * @param t The transferable that shall be imported
     * @param action The action that should be performed.
     */
    boolean importData(Component c, Transferable t, int action);
}
