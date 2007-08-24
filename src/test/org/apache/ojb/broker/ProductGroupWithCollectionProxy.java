package org.apache.ojb.broker;


/** represents a product group containing a set of Articles.
 * @see Article
 */
public class ProductGroupWithCollectionProxy extends AbstractProductGroup
{

    public ProductGroupWithCollectionProxy()
    {
        super();
    }

    public ProductGroupWithCollectionProxy(Integer pGroupId, String pGroupName, String pDescription)
    {
        super (pGroupId, pGroupName, pDescription);
    }

}
