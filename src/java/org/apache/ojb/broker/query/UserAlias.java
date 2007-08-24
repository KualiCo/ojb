package org.apache.ojb.broker.query;

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * This class is used to specify the path segments of a Criteria
 * that should have associated table aliases.  Previously, the default
 * behaviour was that all path segments participated in the alias
 * 
 * @author <a href="mailto:philip.warrick@mcgill.ca">Phil Warrick</a> 
 */
public class UserAlias implements Serializable
{
    private static final long serialVersionUID = 3257282552220627249L;
    
    private Map m_mapping = new HashMap();
    private String m_name = null;
    private String m_attributePath = null;
    private boolean m_allPathsAliased = false;
    private Logger m_logger = LoggerFactory.getLogger(UserAlias.class);

    /**
     * Constructor declaration
     *
     * @param name the name of the alias
     */
    public UserAlias(String name)
    {
        m_name = name;
    }

    /**
     * Constructor declaration
     *
     * @param name the name of the alias
     * @param attributePath the full path of the SelectionCriteria attribute
     * @param aliasPath the portion of the attributePath which should be aliased.
     * This should be unambiguous.  If ambiguous portions need aliasing (e.g.
     * B.C in allAs.B.C.B.C), use add() instead
     */
    public UserAlias(String name, String attributePath, String aliasPath)
    {
        m_name = name;
        m_attributePath = attributePath;
        if (attributePath.lastIndexOf(aliasPath) == -1)
        {
            m_logger.warn("aliasPath should be a substring of attributePath");
        }
        initMapping(attributePath, aliasPath);
    }

    /**
     * Constructor declaration
     *
     * @param name the name of the alias
     * @param attributePath the full path of the SelectionCriteria attribute
     * @param allPathsAliased indicates that all path portions of attributePath
     * should be aliased (previously was the default)
     */
    public UserAlias(String name, String attributePath, boolean allPathsAliased)
    {
        m_name = name;
        m_attributePath = attributePath;
        m_allPathsAliased = allPathsAliased;
    }

    /**
     * generates the mapping from the aliasPath
     * @param aliasPath the portion of attributePath which should be aliased
     *
     */
    private void initMapping(String attributePath, String aliasPath)
    {
        Iterator aliasSegmentItr = pathToSegments(aliasPath).iterator();
        String currPath = "";
        String separator = "";
        while (aliasSegmentItr.hasNext())
        {
            currPath = currPath + separator + (String) aliasSegmentItr.next();
            int beginIndex = attributePath.indexOf(currPath);
            if (beginIndex == -1)
            {
                break;
            }
            int endIndex = beginIndex + currPath.length();
            m_mapping.put(attributePath.substring(0, endIndex), m_name);
            separator = ".";
        }
    }

    private ArrayList pathToSegments(String path)
    {
        ArrayList segments = new ArrayList();
        int sp = path.indexOf('.');
        while (sp != -1)
        {
            segments.add(path.substring(0, sp));
            path = path.substring(sp + 1);
            sp = path.indexOf('.');
        }
        segments.add(path);
        return segments;
    }

    /**
     * Returns the name of this alias
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Returns the name of this alias if path has been added
     * to the aliased portions of attributePath
     *
     * @param path the path to test for inclusion in the alias
     */
    public String getAlias(String path)
    {
        if (m_allPathsAliased && m_attributePath.lastIndexOf(path) != -1)
        {
            return m_name;
        }
        Object retObj = m_mapping.get(path);
        if (retObj != null)
        {
            return (String) retObj;
        }
        return null;
    }

    /**
     * Adds a path to the aliased paths
     *
     * @param path the path to add to the aliased paths
     */
    public void add(String path)
    {
        m_mapping.put(path, m_name);
    }
}