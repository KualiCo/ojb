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

import java.awt.dnd.DnDConstants;

/** This interface just contains the constants used within the DnD framework
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a>
 * @version $Id: DnDWorkerConstants.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public interface DnDWorkerConstants
{
    /** No action is acceptable or has been performed (depending on the
     * context this constant is used in)
     */    
    public static final int NONE = DnDConstants.ACTION_NONE;
    
    /** A "copy" action is acceptable or has been performed (depending on the
     * context this constant is used in)
     */    
    public static final int DRAG_COPY = DnDConstants.ACTION_COPY;
    
    /** A "move" action is acceptable or has been performed (depending on the
     * context this constant is used in)
     */    
    public static final int DRAG_MOVE = DnDConstants.ACTION_MOVE;
    
    /** A "link" action is acceptable or has been performed (depending on the
     * context this constant is used in)
     */    
    public static final int DRAG_LINK = DnDConstants.ACTION_LINK;
    
    /** A clipboard copy action is requested or has been performed, depending on the
     * context this constant is used in.
     */    
    public static final int CLIP_COPY = 0xFE;

    /** A clipboard cut action is requested or has been performed. */    
    public static final int CLIP_CUT  = 0xFF;       
}
