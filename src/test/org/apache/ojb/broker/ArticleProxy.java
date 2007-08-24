package org.apache.ojb.broker;

import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.VirtualProxy;

/**
 * Proxy class to class Article. Implements interface InterfaceArticle.
 * delegates methods calls to the internal Article-object
 * @author Thomas Mahler
 */
public class ArticleProxy extends VirtualProxy implements InterfaceArticle
{
    /* (non-Javadoc)
     * @see org.apache.ojb.broker.InterfaceArticle#setProductGroup(org.apache.ojb.broker.InterfaceProductGroup)
     */
    public void setProductGroup(InterfaceProductGroup pg)
    {
        realSubject().setProductGroup(pg);
    }

    public ArticleProxy()
    {
    }

    /**
     * ArticleProxy constructor comment.
     * @param uniqueId org.apache.ojb.broker.Identity
     */
    public ArticleProxy(PBKey key, Identity uniqueId)
    {
        super(key, uniqueId);
    }

    public ArticleProxy(IndirectionHandler handler)
    {
        super(handler);
    }


    /**
     * addToStock method comment.
     */
    public void addToStock(int diff)
    {
        realSubject().addToStock(diff);
    }

    /**
     * getArticleId method comment.
     */
    public Integer getArticleId()
    {
        return realSubject().getArticleId();
    }

    /**
     * getArticleName method comment.
     */
    public String getArticleName()
    {
        return realSubject().getArticleName();
    }

    /**
     * getProductGroup method comment.
     */
    public InterfaceProductGroup getProductGroup()
    {
        return realSubject().getProductGroup();
    }

    /**
     * getStockValue method comment.
     */
    public double getStockValue()
    {
        return realSubject().getStockValue();
    }

    public void setStock(int stock)
    {
        realSubject().setStock(stock);
    }

    /**
     * getStock method comment.
     */
    public int getStock()
    {
        return realSubject().getStock();
    }

    /**
     * Insert the method's description here.
     * Creation date: (08.11.2000 22:38:26)
     * @return org.apache.ojb.examples.broker.Article
     */
    private InterfaceArticle realSubject()
    {
        try
        {
            return (InterfaceArticle) getRealSubject();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * setArticleId method comment.
     */
    public void setArticleId(Integer newArticleId)
    {
        realSubject().setArticleId(newArticleId);
    }

    /**
     * setArticleName method comment.
     */
    public void setArticleName(java.lang.String newArticleName)
    {
        realSubject().setArticleName(newArticleName);
    }
}
