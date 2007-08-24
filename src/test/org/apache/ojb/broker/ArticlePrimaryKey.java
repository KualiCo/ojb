package org.apache.ojb.broker;

import org.apache.ojb.broker.Identity;

/**
 * This is a Primary Key Class for Article class
 **/
public class ArticlePrimaryKey implements java.io.Serializable
{


    int id;

    /**
     * PlzEntryBmpKey(String key) constructor
     */
    public ArticlePrimaryKey(int key)
    {
        //oid = new PlzEntryIdentity(key);
        id = key;

    }


    /**
     * equals method
     * - user must provide a proper implementation for the equal method. The generated
     *   method assumes the key is a String object.
     */
    public boolean equals(Object o)
    {
        if (o instanceof ArticlePrimaryKey)
            return (id == ((ArticlePrimaryKey) o).id);
        else
            return false;
    }

    /**
     * hashcode method
     * - user must provide a proper implementation for the hashCode method. The generated
     *    method assumes the key is a String object.
     */
    public int hashCode()
    {
        return id;
    }

    /**
     * PlzEntryBmpKey(String key) constructor
     */
    public ArticlePrimaryKey(Identity oid)
    {

        id = ((Integer) oid.getPrimaryKeyValues()[0]).intValue();

    }
}
