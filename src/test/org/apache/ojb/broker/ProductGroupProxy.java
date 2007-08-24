package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.List;

import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.VirtualProxy;


/**
 * Proxy class for ProductGroup.
 */
public class ProductGroupProxy extends VirtualProxy implements InterfaceProductGroup, Serializable
{
    public ProductGroupProxy()
    {
    }

    /**
     * ProductGroupProxy constructor comment.
     * @param uniqueId org.apache.ojb.broker.Identity
     */
    public ProductGroupProxy(PBKey key, Identity uniqueId)
    {
        super(key, uniqueId);
    }

    public ProductGroupProxy(IndirectionHandler handler)
    {
        super(handler);
    }


    /** return List of all Articles in productgroup*/
    public List getAllArticles()
    {
        return realSubject().getAllArticles();
    }

    /** return group id*/
    public Integer getId()
    {
        return realSubject().getId();
    }

    /** return groupname*/
    public String getName()
    {
        return realSubject().getName();
    }

    /**
     * Insert the method's description here.
     * Creation date: (08.11.2000 22:38:26)
     * @return org.apache.ojb.examples.broker.Article
     */
    private ProductGroup realSubject()
    {
        try
        {
            ProductGroup result = (ProductGroup) getRealSubject();
            if (result == null) throw new NullPointerException("Real subject was null");
            return result;
        }
        catch (Throwable t)
        {
            System.out.println(t.getMessage());
            t.printStackTrace();
            return null;
        }
    }

    public String toString()
    {
        return realSubject().toString();
    }

    /** add article to group*/
    public void add(InterfaceArticle article)
    {
        realSubject().add(article);
    }
}
