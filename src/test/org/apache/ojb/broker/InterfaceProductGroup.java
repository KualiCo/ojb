package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.List;

/**
 * Interface for ProductGroup objects.
 */
public interface InterfaceProductGroup extends Serializable
{
    /** return List of all Articles in productgroup*/
    public List getAllArticles();

    /** add article to group*/
    public void add(InterfaceArticle article);

    /** return group id*/
    public Integer getId();

    /** return groupname*/
    public String getName();
}
