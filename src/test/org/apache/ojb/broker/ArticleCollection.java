package org.apache.ojb.broker;

import org.apache.ojb.broker.ManageableCollection;

import java.util.Vector;

/**
 * Insert the type's description here.
 * Creation date: (18.02.2001 16:09:24)
 * @author Thomas Mahler
 */
public class ArticleCollection implements ManageableCollection, java.io.Serializable
{
    private Vector elements;

    /**
     * ArticleCollection constructor comment.
     */
    public ArticleCollection()
    {
        super();
        elements = new Vector();
    }

    public void add(InterfaceArticle article)
    {
        if (elements == null)
            elements = new Vector();
        elements.add(article);
    }

    public InterfaceArticle get(int index)
    {
        return (InterfaceArticle) elements.get(index);
    }

    /**
     * add method comment.
     */
    public void ojbAdd(java.lang.Object anObject)
    {
        elements.add(anObject);
    }

    /**
     * addAll method comment.
     */
    public void ojbAddAll(org.apache.ojb.broker.ManageableCollection otherCollection)
    {
        elements.addAll(((ArticleCollection) otherCollection).elements);
    }

    /**
     * ojbIterator method comment.
     */
    public java.util.Iterator ojbIterator()
    {
        return elements.iterator();
    }

    public void afterStore(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }

    public int size()
    {
        return elements.size();
    }

    public String toString()
    {
        return elements.toString();
    }
}
