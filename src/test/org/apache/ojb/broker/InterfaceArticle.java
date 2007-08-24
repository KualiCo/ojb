package org.apache.ojb.broker;

import java.io.Serializable;

/**
 * defines the behaviour of Articles. implemented by real 'Articles' and their ArticleProxies
 */
public interface InterfaceArticle extends Serializable
{
    public void addToStock(int diff);

    public Integer getArticleId();

    public String getArticleName();

    public InterfaceProductGroup getProductGroup();

    public void setProductGroup(InterfaceProductGroup pg);

    public double getStockValue();

    public void setArticleId(Integer newArticleId);

    public void setArticleName(String newArticleName);
    
    public void setStock(int stock);

    public int getStock();

    public String toString();
}
