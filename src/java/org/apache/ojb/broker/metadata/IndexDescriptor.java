package org.apache.ojb.broker.metadata;

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

import org.apache.commons.lang.SystemUtils;

import java.util.Vector;
import java.io.Serializable;

/**
 *
 *
 * @version $Id: IndexDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class IndexDescriptor implements XmlCapable, Serializable
{
	private static final long serialVersionUID = -1722513568634970108L;
    private String name;
    private boolean unique;
    private Vector indexColumns = new Vector();

    public boolean isUnique()
    {
        return unique;
    }

    public void setUnique(boolean unique)
    {
        this.unique = unique;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Vector getIndexColumns()
    {
        return this.indexColumns;
    }

    public void setIndexColumns(Vector indexColumns)
    {
        this.indexColumns = indexColumns;
    }

    /*
     * @see XmlCapable#toXML()
     */
    public String toXML()
    {
        RepositoryTags tags = RepositoryTags.getInstance();
        String eol = SystemUtils.LINE_SEPARATOR;

        //opening tag + attributes
        StringBuffer result = new StringBuffer( 1024 );
        result.append( "      <" );
        result.append( tags.getTagById( INDEX_DESCRIPTOR ) );
        result.append( " " );

        // index name
        result.append( tags.getAttribute( NAME, getName() ) );
        result.append( " " );

        // unique attribute
        result.append( tags.getAttribute( UNIQUE, "" + isUnique() ) );
        result.append( ">" );
        result.append( eol );

        // index columns
        for( int i = 0; i < indexColumns.size(); i++ )
        {
            String l_name = ( String ) indexColumns.elementAt( i );
            result.append( "                " );
            result.append( tags.getOpeningTagNonClosingById( INDEX_COLUMN ) );
            result.append( " " );
            result.append( tags.getAttribute( NAME, l_name ) );
            result.append( " />" );
            result.append( eol );
        }

        // closing tag
        result.append( "      " );
        result.append( tags.getClosingTagById( INDEX_DESCRIPTOR ) );
        result.append( " " );
        result.append( eol );

        return result.toString();
    }

}
