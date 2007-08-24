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
import java.awt.Image;
import java.awt.datatransfer.Transferable;

/** Implementers of this interface are used by DragHelper to query the possible
 * actions for a drag gesture and to export the data into a Transferable
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a>
 * @version $Id: DragCopyCutWorkerInterface.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public interface DragCopyCutWorkerInterface
{
    /** Return a Transferable with the data you whish to export. You also get
     * the Component the DnD actions has been started for. If the component
     * supports selection you must first check which items are selected and
     * afterwards put those items in the Transferable.
     * @param c The component that acts as the drag source
     * @return a Transferable containing the exported data
     */
    public Transferable getTransferable(Component c);
    
    /** Return a bitmask of acceptable actions. In most cases you will only support
     * DRAG_COPY, but sometimes you might support DRAG_LINK or DRAG_MOVE as well.
     * @param c The component that acts as the drag source
     * @return A bitmask of possible drag actions for the given Component
     */
    public int getAcceptableActions(Component c);
    
    /** Is called to notify you that the export has started. This is always
     * called after getTransferable, so you should know which items are exported.
     * This method is currently not called by the framework, but may be in future.
     * @param c The component that acts as the drag source
     * @param action The drag action that is going to be performed
     */
    public void exportStarted(Component c, int action);
    
    /** Is called to notify you that the export this Worker has been notified of
     * has finished. action shows you which action has been performed, e.g. if it
     * is DRAG_MOVE you can remove the dragged items from your model.
     * @param c The component that acts as the drag source
     * @param action The drag action that has been performed
     */
    public void exportDone(Component c, int action);
    
    
    /** DnD on some platforms supports displaying a drag image in addition
     * to the drag cursor (Windows is known not to support it, so if you are
     * on Windows you might be doing all right, but still see no image)
     * @return an Image that shall be displayed with the cursor.
     * @param c The component that acts as the drag source
     * @param t The transferable that is used in this DnD process
     * @param action The currently requested action for the ongoing drag process
     */
    public Image getDragImage(Component c, Transferable t, int action);
    
}
