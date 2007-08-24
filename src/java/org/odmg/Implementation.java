package org.odmg;


import org.apache.ojb.odmg.oql.EnhancedOQLQuery;


/**
 * The factory interface for a particular ODMG implementation.
 * Each ODMG implementation will have a class that implements this interface.
 * @author	David Jordan (as Java Editor of the Object Data Management Group)
 * @version ODMG 3.0
 */

public interface Implementation
{
    /**
     * Create a <code>Transaction</code> object and associate it with the current thread.
     * @return The newly created <code>Transaction</code> instance.
     * @see org.odmg.Transaction
     */
    public Transaction newTransaction();

    /**
     * Get the current <code>Transaction</code> for the thread.
     * @return The current <code>Transaction</code> object or null if there is none.
     * @see org.odmg.Transaction
     */
    public Transaction currentTransaction();

    /**
     * Create a new <code>Database</code> object.
     * @return The new <code>Database</code> object.
     * @see org.odmg.Database
     */
    public Database newDatabase();

    /**
     * Create a new <code>OQLQuery</code> object.
     * @return The new <code>OQLQuery</code> object.
     * @see org.odmg.OQLQuery
     */
    public EnhancedOQLQuery newOQLQuery();

    /**
     * Create a new <code>DList</code> object.
     * @return The new <code>DList</code> object.
     * @see org.odmg.DList
     */
    public DList newDList();

    /**
     * Create a new <code>DBag</code> object.
     * @return The new <code>DBag</code> object.
     * @see org.odmg.DBag
     */
    public DBag newDBag();

    /**
     * Create a new <code>DSet</code> object.
     * @return The new <code>DSet</code> object.
     * @see org.odmg.DSet
     */
    public DSet newDSet();

    /**
     * Create a new <code>DArray</code> object.
     * @return The new <code>DArray</code> object.
     * @see org.odmg.DArray
     */
    public DArray newDArray();

    /**
     * Create a new <code>DMap</code> object.
     * @return	The new <code>DMap</code> object.
     * @see org.odmg.DMap
     */
    public DMap newDMap();

    /**
     * Get a <code>String</code> representation of the object's identifier.
     * @param obj The object whose identifier is being accessed.
     * @return The object's identifier in the form of a String
     */
    public String getObjectId(Object obj);

    /**
     * Get the <code>Database</code> that contains the object <code>obj</code>.
     * @param obj The object.
     * @return The <code>Database</code> that contains the object.
     */
    public Database getDatabase(Object obj);
}
