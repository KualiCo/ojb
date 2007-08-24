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

import java.awt.dnd.DropTarget;
/**
 * Starting from JDK 1.2 drag and drop was possible for Java applications. Unfortunately
 * the framework to be used is rather complex. As of JDK 1.4 a new, more simple Dnd framework
 * was added, but unfortunately if you want to be backwards compatible, you still have to
 * stick with the old framework.
 *
 * This helper class should make it easier to implement DnD for JDK 1.2 and 1.3. To add drop
 * support for a Component, you only have to write your Implementation of DropPasteWorkerInterface.
 * This interface is responsible to read data from a Transferable and add it to the model of the
 * target component.
 * <CODE>
 *       DropTargetHelper helper = new DropTargetHelper();
 *       helper.registerDropPasteWorker (new ReverseDbNodesDropWorker());
 *       aComponent.setDropTarget(helper.getDropTarget ());
 * </CODE>
 * If you want to supply your own implementation of a DropTargetListener or an extension
 * of the implementation in this class, you have to use the following code:
 * <CODE>
 * helper.setDefaultDropTargetListener(aDTListener);
 * helper.removeDefaultDropTargetListener();
 * helper.registerDefaultDropTargetListener();
 * </CODE>
 * Because the DropTarget is a unicast source, you first have to remove the
 * old listener and the register the new. If you do not remove the listener
 * before adding a new one, a TooManyListenersException is thrown.
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DropTargetHelper.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class DropTargetHelper
{
    private java.awt.dnd.DropTargetListener defaultDTListener 
        = new org.apache.ojb.tools.mapping.reversedb2.dnd2.DropTargetHelper.DTListener();
    
    private DropTarget defaultDropTarget = new DropTarget();
    
    private java.util.Set dropPasteWorkerSet = new java.util.HashSet();
    
    /** Creates a new instance of DropTarget */
    public DropTargetHelper ()
    {
        super();
        try
        {
            defaultDropTarget.addDropTargetListener(defaultDTListener);
            defaultDropTarget.setActive(true);
        }
        catch (java.util.TooManyListenersException tmle)
        {
            throw new RuntimeException("Internal Error: Drop Target already has a listener registered but this mustn't be at this stage");
        }
    }
    
    /**
     * Remove current DropTargetListener from the DropTarget.
     */
    public void removeDefaultDropTargetListener()
    {
        defaultDropTarget.removeDropTargetListener(defaultDTListener);
    }
    
    /** Set the current DropTargetListener as listener of the current DropTarget.
     * @throws TooManyListenersException
     */
    public void registerDefaultDropTargetListener()
        throws java.util.TooManyListenersException
    {
        defaultDropTarget.addDropTargetListener(defaultDTListener);
    }
    
    /** Set a new DropTargetListner this helper is going to use.
     * @param dtl The new DropTargetListener
     */
    public void setDefaultDropTargetListener(java.awt.dnd.DropTargetListener dtl)
    {
        this.defaultDTListener = dtl;
    }
    
    /** Register a new DropPasteWorkerInterface.
     * @param worker The new worker
     */
    public void registerDropPasteWorker(DropPasteWorkerInterface worker)
    {
        this.dropPasteWorkerSet.add(worker);
        defaultDropTarget.setDefaultActions( 
            defaultDropTarget.getDefaultActions() 
            | worker.getAcceptableActions(defaultDropTarget.getComponent())
                                           );
    }
    
    /** Remove a DropPasteWorker from the helper.
     * @param worker the worker that should be removed
     */
    public void removeDropPasteWorker(DropPasteWorkerInterface worker)
    {
        this.dropPasteWorkerSet.remove(worker);
        java.util.Iterator it = this.dropPasteWorkerSet.iterator();
        int newDefaultActions = 0;
        while (it.hasNext())
            newDefaultActions |= ((DropPasteWorkerInterface)it.next()).getAcceptableActions(defaultDropTarget.getComponent());
        defaultDropTarget.setDefaultActions(newDefaultActions);
    }
    
    /** Get the DropTarget (to be used in Component.setDropTarget());
     * @return a DropTarget
     */
    public DropTarget getDropTarget()
    {
        return this.defaultDropTarget;
    }
    
    /**
     * An implementation of a DropTargetListener.
     */
    public class DTListener implements java.awt.dnd.DropTargetListener
    {
        /** see java.awt.dnd.DropTargetListener
         * @param dropTargetDragEvent
         */        
        public void dragEnter (java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
        {
            // 1. Check which actions are supported with the given DataFlavors 
            // and Components
            System.err.println("DTListener.dragEnter()" + dropTargetDragEvent);
            java.util.Iterator it = dropPasteWorkerSet.iterator();
            int dropTargetSupportedActions = DnDWorkerConstants.NONE;
            while (it.hasNext())
                dropTargetSupportedActions |= 
                    ((DropPasteWorkerInterface)it.next()).getAcceptableActions(
                      dropTargetDragEvent.getDropTargetContext().getComponent(), 
                      dropTargetDragEvent.getCurrentDataFlavors());
//            System.err.println(dropTargetDragEvent.getDropTargetContext().getTransferable().getTransferDataFlavors());
            int dragSourceSupportedActions = dropTargetDragEvent.getSourceActions();
            // Check wheter current actions and acceptable actions match.
            if ((dropTargetSupportedActions & dragSourceSupportedActions) != DnDWorkerConstants.NONE)
            {
                System.err.println("   accepting " +  (dropTargetSupportedActions & dragSourceSupportedActions));
                dropTargetDragEvent.acceptDrag(dropTargetSupportedActions & dragSourceSupportedActions);
            }
            // No match, accept the drag with the supported drop target actions
            else if (dropTargetSupportedActions != DnDWorkerConstants.NONE)
            {
                System.err.println("   accepting " + dropTargetSupportedActions);
                dropTargetDragEvent.acceptDrag(dropTargetSupportedActions);
            }
            // No import possible at all, reject the drag
            else
            {
                System.err.println("   rejecting");
                dropTargetDragEvent.rejectDrag();
            }
        }
        
        /** see java.awt.dnd.DropTargetListener
         * @param dropTargetEvent
         */        
        public void dragExit (java.awt.dnd.DropTargetEvent dropTargetEvent)
        {
            System.err.println("DTListener.dragExit()" + dropTargetEvent);
        }
        
        /** see java.awt.dnd.DropTargetListener
         * @param dropTargetDragEvent
         */        
        public void dragOver (java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
        {

        }
        
        /** see java.awt.dnd.DropTargetListener
         * @param dropTargetDropEvent
         */        
        public void drop (java.awt.dnd.DropTargetDropEvent dropTargetDropEvent)
        {
            System.err.println("DTListener.drop()" + dropTargetDropEvent);   
            
            java.util.Iterator it = dropPasteWorkerSet.iterator();
            boolean result = false;
            
            while (!result & it.hasNext())
            {
                DropPasteWorkerInterface worker = (DropPasteWorkerInterface)it.next();
                int acceptableActions = worker.getAcceptableActions(dropTargetDropEvent.getDropTargetContext ().getComponent(),
                                                dropTargetDropEvent.getTransferable().getTransferDataFlavors());
                if ((acceptableActions & dropTargetDropEvent.getDropAction()) 
                    != DnDWorkerConstants.NONE)
                {
                    dropTargetDropEvent.acceptDrop(acceptableActions & dropTargetDropEvent.getDropAction());
                    result = worker.importData(dropTargetDropEvent.getDropTargetContext ().getComponent(),
                                               dropTargetDropEvent.getTransferable(),
                                               dropTargetDropEvent.getDropAction());
                }
            }
            dropTargetDropEvent.dropComplete(result);            
        }
        
        /** see java.awt.dnd.DropTargetListener
         * @param dropTargetDragEvent
         * @see java.awt.dnd.DropTargetListener
         */        
        public void dropActionChanged (java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
        {
            System.err.println("DTListener.dragEnter()" + dropTargetDragEvent);
            dragEnter(dropTargetDragEvent);
        }
        
    }
    
}
