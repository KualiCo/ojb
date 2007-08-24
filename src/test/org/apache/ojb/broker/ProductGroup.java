package org.apache.ojb.broker;


/** represents a product group containing a set of Articles.
 * @see Article
 */
public class ProductGroup extends AbstractProductGroup
{

    public ProductGroup()
    {
        super();
    }

    public ProductGroup(Integer pGroupId, String pGroupName, String pDescription)
    {
        super (pGroupId, pGroupName, pDescription);
    }
}
