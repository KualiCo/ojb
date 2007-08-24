package org.apache.ojb.tools.mapping.reversedb2;

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
 * This interface is intended to be implemented by objects that are part
 * of a GUI and that whish to present a context menu. 
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: ActionTarget.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public interface ActionTarget 
{
    /**
     * Get a bunch of java.util.Action objects that this object wants to
     * offer in a context-menu.
     * @return an Iterator containing all the actions.
     */
    public java.util.Iterator getActions();
    
    /**
     * Some objects may alter the list of actions they present depending
     * on the state they are in. If this method returns true, the list of
     * actions can be cached (i.e. the resulting GUI object can be cached
     * and doesn't have to be regenerated all the time)
     *
     * @return true if the result of getActions() may be cached.
     */
    public boolean actionListCachable();
    
    /**
     * Some objects may return a list of actions depending on the state they
     * are in or the way they have been created. Others always return the 
     * same list of actions without any dependency on the state. If this
     * is the case, this method should return true. Other objects may
     * cache the list of actions and the depending GUI objects on a per-class
     * basis instead of a per-object basis. If this method returns true, 
     * actionListCacheable has to return true as well, otherwise it would 
     * not make any sense to cache the information here.
     */
    public boolean actionListStatic();
}
