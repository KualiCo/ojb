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

/**
 * This class allows you to implement the drag of DnD in your GUI by simply creating
 * an instance of this class, supplying your implementation of a DragCopyCutWorkerInterface 
 * and register the Component with the helper using registerCopmponent().
 * If the default implementation of DnD by this class doesn't satisfy your needs
 * you can override all of the functionality by supplying your own DragGestureListener
 * and DragSourceListener. Those interfaces are part of the Java 1.2/1.3 Dnd framework,
 * so more information about these interfaces can be found in the JDK docs.
 *
 * This class is closely related to DropTargetHelper, the class responsible for 
 * the drop in DnD.
 *
 * To implement DnD for any Component, you have to write the following code:
 * <CODE>
 * new DragHelper(new YourDragCopyCutWorkerInterfaceImplementation()).registerComponent(aComponent);
 * </CODE>
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DragHelper.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class DragHelper
{
    private java.awt.dnd.DragGestureListener   dgListener = new DGListener();
    private java.awt.dnd.DragSourceListener    dsListener = new DSListener();
    private java.awt.dnd.DragSource            dragSource;
    private java.util.Map                      hmDragGestureRecognizers = new java.util.HashMap();
    private Class                              recognizerAbstractClass = null;
    private DragCopyCutWorkerInterface         dragWorker;
    
    /** Using this constructor you can completely customize the drag behaviour. You
     * have to supply your own DragGestureListener and DragSourcecListener in addition
     * to the DragSource, the drag gesture recognizer and the worker.
     *
     * The default implementation of DragGestureListener and DragSourceListener are
     * exposed publicly in this class, so you are able to provide your own
     * implementation for DragGestureListener or DragSourceListener and use the default
     * one for the other.
     * @param pDgListener Your implementation of DragGestureListener. In case you want to
     * use the default supplied within this class, instantiate a DGListener and supply
     * it here.
     * @param pDsListener Your implementation of DragSourceListener. In case you want to
     * use the default supplied within this class, instantiate a DSListener and supply
     * it here.
     * @param pDragSource Your DragSource implementation. The default AWT DragSource is exposed by java.awt.dnd.DragSource.getDefaultDragSource()
     * @param pRecognizerAbstractClass The drag gesture recognizer. To use the AWT-built-in default supply a null here.
     * @param pDragWorker Your DragWorker implementation
     */
    public DragHelper(java.awt.dnd.DragGestureListener pDgListener, 
                      java.awt.dnd.DragSourceListener  pDsListener,
                      java.awt.dnd.DragSource          pDragSource, 
                      Class                            pRecognizerAbstractClass,
                      DragCopyCutWorkerInterface       pDragWorker)
    {
        this(pDragSource, pRecognizerAbstractClass, pDragWorker);
        this.dgListener = pDgListener;
        this.dsListener = pDsListener;
    }
    
    /** A more complex way of setting up dragging. In addition to your worker you need
     * to supply the recognizer and the DragSource (usually
     * java.awt.dnd.DragSource.getDefaultDragSource(), but you can supply your own
     * here)
     * @param pDragSource The drag source
     * @param pRecognizerAbstractClass The recognizer, may be null if you want to use the Swing default implementation
     * @param pDragWorker Your DragCopyCutWorkerInterface
     */    
    public DragHelper(java.awt.dnd.DragSource          pDragSource, 
                      Class                            pRecognizerAbstractClass,
                      DragCopyCutWorkerInterface       pDragWorker)
    {    
        this.dragSource = pDragSource;
        this.recognizerAbstractClass = pRecognizerAbstractClass;
        this.dragWorker = pDragWorker;
    }
    
    
    /** Easiest way to setup dragging for your GUI. The default implementations for
     * DragGestureListener, DragSourceListener and the drag gesture recognizer
     * are used. You just need to supply a DragCopyCutWorkerInterface.
     * @param pDragWorker Your implementation of the  DragCopyCutWorkerInterface
     */    
    public DragHelper(DragCopyCutWorkerInterface pDragWorker)
    {
        this(java.awt.dnd.DragSource.getDefaultDragSource(),
             null, pDragWorker);
    }
    
    /** add a Component to this Worker. After the call dragging is enabled for this
     * Component.
     * @param c the Component to register
     */    
    public void registerComponent(java.awt.Component c)
    {
        unregisterComponent(c);
        if (recognizerAbstractClass == null)
        {
            hmDragGestureRecognizers.put(c,  
                dragSource.createDefaultDragGestureRecognizer(c, 
                    dragWorker.getAcceptableActions(c), dgListener)
                                        );
        }
        else
        {
            hmDragGestureRecognizers.put(c, 
                dragSource.createDragGestureRecognizer (recognizerAbstractClass,
                    c, dragWorker.getAcceptableActions(c), dgListener)
                                        );
        }
    }
    
    /** remove drag support from the given Component.
     * @param c the Component to remove
     */    
    public void unregisterComponent(java.awt.Component c)
    {
        java.awt.dnd.DragGestureRecognizer recognizer = 
            (java.awt.dnd.DragGestureRecognizer)this.hmDragGestureRecognizers.remove(c);
        if (recognizer != null)
            recognizer.setComponent(null);
    }
    
    /** For more information see the javadocs of java.awt.DragGestureListener
     * @see java.awt.dnd.DragGestureListener
     */    
    public class DGListener implements java.awt.dnd.DragGestureListener
    {
        
        /** For more information see the javadocs of java.awt.DragGestureListener. Basically
         * this method is called by AWT if a drag gesture has been recognized and therefore
         * a drag action should be initiated. This method checks whether it can perform a
         * drag, gets the transferable from the worker and starts the drag on the drag
         * source.
         * @param dragGestureEvent For more information see the javadocs of java.awt.DragGestureListener
         */        
        public void dragGestureRecognized (java.awt.dnd.DragGestureEvent dragGestureEvent)
        {
            System.err.println("DGListener.dragGestureRecognized() dragAction:" + dragGestureEvent.getDragAction());
            if (dragWorker.getAcceptableActions (dragGestureEvent.getComponent()) == DnDWorkerConstants.NONE) return;            
            java.awt.datatransfer.Transferable transferable = 
                dragWorker.getTransferable(dragGestureEvent.getSourceAsDragGestureRecognizer ().getComponent ());
            try
            {
                if (transferable != null)
                {
                    dragSource.startDrag(dragGestureEvent, 
                                         null, 
                                         dragWorker.getDragImage(dragGestureEvent.getComponent(), transferable, dragGestureEvent.getDragAction ()), 
                                         new java.awt.Point(0,0),
                                         transferable,
                                         dsListener);
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }        
    }    
    
    
    /** an implementation of java.awt.dnd.DragSourceListener. The methods of this
     * listener get called when a drag is in process.
     */    
    public class DSListener implements java.awt.dnd.DragSourceListener
    {
        
        /** Informs the listener that the drag process has ended. If the drag was
         * successful, the exportDone method of the worker is called.
         * @param dragSourceDropEvent the event.
         */        
        public void dragDropEnd (java.awt.dnd.DragSourceDropEvent dragSourceDropEvent)
        {
            System.err.println("DSListener.dragDropEnd()");
            if (dragSourceDropEvent.getDropSuccess())
            {
                dragWorker.exportDone(dragSourceDropEvent.getDragSourceContext ().getComponent (),
                                      dragSourceDropEvent.getDropAction());
            }
            else
            {
                // ????
            }
        }
        
        /** For more information see the javadocs of java.awt.dnd.DragSourceListener
         * @param dragSourceDragEvent
         */        
        public void dragEnter (java.awt.dnd.DragSourceDragEvent dragSourceDragEvent)
        {
            System.err.println("DSListener.dragEnter() dropAction:" + dragSourceDragEvent.getDropAction());
/*            if ( (dragSourceDragEvent.getDropAction() 
                 & dragWorker.getAcceptableActions(dragSourceDragEvent.getDragSourceContext ().getComponent())) 
                 != DnDWorkerConstants.NONE)
            {
                dragSourceDragEvent.getDragSourceContext().setCursor(java.awt.dnd.DragSource.DefaultCopyDrop);
            }
            else
            {
                dragSourceDragEvent.getDragSourceContext().setCursor(java.awt.dnd.DragSource.DefaultCopyNoDrop);
            }*/
        }
        
        /** DragSourceListener */        
        public void dragExit (java.awt.dnd.DragSourceEvent dragSourceEvent)
        {
            System.err.println("DSListener.dragExit()");
        }
        
        /** For more information see the javadocs of java.awt.dnd.DragSourceListener
         * @param dragSourceDragEvent
         */        
        public void dragOver (java.awt.dnd.DragSourceDragEvent dragSourceDragEvent)
        {
            // System.err.println("DSListener.dragOver()");
        }
        
        /** For more information see the javadocs of java.awt.dnd.DragSourceListener
         * @param dragSourceDragEvent
         */        
        public void dropActionChanged (java.awt.dnd.DragSourceDragEvent dragSourceDragEvent)
        {
            System.err.println("DSListener.dropActionChanged() dropAction:" + dragSourceDragEvent.getDropAction());
/*            if ( (dragSourceDragEvent.getDropAction() 
                & dragWorker.getAcceptableActions(dragSourceDragEvent.getDragSourceContext ().getComponent())) 
                != DnDWorkerConstants.NONE)
            {
                dragSourceDragEvent.getDragSourceContext().setCursor(java.awt.dnd.DragSource.DefaultCopyDrop);
            }            
            else
            {
                dragSourceDragEvent.getDragSourceContext().setCursor(java.awt.dnd.DragSource.DefaultCopyNoDrop);                
            }*/
        }
        
    }
    
}
