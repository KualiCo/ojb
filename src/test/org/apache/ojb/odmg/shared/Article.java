package org.apache.ojb.odmg.shared;

import org.apache.ojb.odmg.shared.ProductGroup;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;


/** Simple Article class is not derived from any base class nor does it implement any Interface,
 * but still it can be made persistent by the PersistenceBroker.
 * Has a lot of private members to be mapped to rdbms columns, but only few business methods
 */
public class Article implements org.apache.ojb.odmg.TransactionAware
{
    /** return a string representaion of an article*/
    public String toString()
    {
        return new ToStringBuilder(this)
                .append("articleId", articleId)
                .append("articleName", articleName)
                .append("productGroup", (productGroup != null ? productGroup.getName() : null))
                .append("productGroupId", productGroupId)
                .append("isSelloutArticle", isSelloutArticle)
                .append("minimumStock", minimumStock)
                .append("orderedUnits", orderedUnits)
                .append("price", price)
                .append("orderedUnits", orderedUnits)
                .append("stock", stock)
                .append("supplierId", supplierId)
                .append("unit", unit)
                .toString();
    }

    /** maps to db-column "Artikel-Nr";INT;PrimaryKey*/
    private int articleId;
    /** maps to db-column Artikelname;CHAR*/
    private String articleName;
    /** maps to db-column Auslaufartikel;SMALL INT*/
    private boolean isSelloutArticle;
    /** maps to db-column Mindestbestand;INT*/
    private int minimumStock;
    /** maps to db-column BestellteEinheiten;INT*/
    private int orderedUnits;
    /** maps to db-column Einzelpreis;DECIMAL*/
    private double price;
    /** reference to the articles category*/
    private ProductGroup productGroup;
    /** maps to db-column Kategorie-Nr;INT*/
    private int productGroupId;
    /** maps to db-column Lagerbestand;INT*/
    private int stock;
    /** maps to db-column Lieferanten-Nr;INT*/
    private int supplierId;
    /** maps to db-column Liefereinheit;CHAR*/
    private String unit;

    public Article(int pArticleId, String pArticleName,
                   int pSupplierId, int pProcuctGroupId,
                   String pUnit, double pPrice, int pStock,
                   int pOrderedUnits, int pMinimumStock,
                   boolean pIsSelloutArticle)
    {
        articleId = pArticleId;
        articleName = pArticleName;
        supplierId = pSupplierId;
        productGroupId = pProcuctGroupId;
        unit = pUnit;
        price = pPrice;
        stock = pStock;
        orderedUnits = pOrderedUnits;
        minimumStock = pMinimumStock;
        isSelloutArticle = pIsSelloutArticle;

    }

    public Article()
    {
    }

    public static Article createInstance()
    {
        return new Article();
    }


    /** increase the amount of articles in stock by diff
     * mark the object as modified only if value changes (i.e. diff != 0 )
     */
    public void addToStock(int diff)
    {
        stock += diff;
    }

    /**
     * return an articles unique id.
     * @return int the articles unique id
     */
    public int getArticleId()
    {
        return articleId;
    }

    /**
     * return an articles name.
     * @return java.lang.String
     */
    public String getArticleName()
    {
        return articleName;
    }

    /** return an articles ProductGroup*/
    public ProductGroup getProductGroup()
    {
        return productGroup;
    }

    /**
     * return stock of Article.
     * @return int
     */
    public int getStock()
    {
        return stock;
    }

    /** compute the total value of an articles stock*/
    public double getStockValue()
    {
        return price * stock;
    }

    /**
     * Sets the articleId.
     * @param articleId The articleId to set
     */
    public void setArticleId(int articleId)
    {
        this.articleId = articleId;
    }

    /**
     * Sets the articleName.
     * @param articleName The articleName to set
     */
    public void setArticleName(String articleName)
    {
        this.articleName = articleName;
    }

    /**
     * Sets the stock.
     * @param stock The stock to set
     */
    public void setStock(int stock)
    {
        this.stock = stock;
    }

    /**
     * afterAbort will be called after a transaction has been aborted.
     * The values of fields which get persisted will have changed to
     * what they were at the begining of the transaction.  This method
     * should be overridden to reset any transient or non-persistent
     * fields.
     */
    public void afterAbort()
    {
        //System.out.println("afterAbort: " + new Identity(this));
    }

    /**
     * afterCommit is called only after a successful commit has taken
     * place.
     */
    public void afterCommit()
    {
        //System.out.println("afterCommit: " + new Identity(this));
    }

    /**
     * beforeAbort is called before a transaction is aborted.
     */
    public void beforeAbort()
    {
        //System.out.println("beforeAbort: " + new Identity(this));
    }

    /**
     * beforeCommit will give an object a chance to kill a
     * transaction before it is committed.
     *
     * To kill a transaction, throw a new TransactionAbortedException.
     */
    public void beforeCommit() throws org.odmg.TransactionAbortedException
    {
        //System.out.println("beforeCommit: " + new Identity(this));
    }


    public boolean equals(Object obj)
    {
        if (obj instanceof Article)
        {
            Article other = ((Article) obj);
            return new EqualsBuilder()
                .append(articleId, other.articleId)
                .append(articleName, other.articleName)
                .append(productGroupId, other.productGroupId)
                .append(isSelloutArticle, other.isSelloutArticle)
                .append(minimumStock, other.minimumStock)
                .append(orderedUnits, other.orderedUnits)
                .append(price, other.price)
                .append(orderedUnits, other.orderedUnits)
                .append(stock, other.stock)
                .append(supplierId, other.supplierId)
                .append(unit, other.unit)
                .isEquals();
        }
        else
            return false;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        // Since we redefined equals, we have to redefine hashCode as well
        return articleId;
    }

    /**
     * Gets the isSelloutArticle.
     * @return Returns a boolean
     */
    public boolean getIsSelloutArticle()
    {
        return isSelloutArticle;
    }

    /**
     * Sets the isSelloutArticle.
     * @param isSelloutArticle The isSelloutArticle to set
     */
    public void setIsSelloutArticle(boolean isSelloutArticle)
    {
        this.isSelloutArticle = isSelloutArticle;
    }

    /**
     * Gets the minimumStock.
     * @return Returns a int
     */
    public int getMinimumStock()
    {
        return minimumStock;
    }

    /**
     * Sets the minimumStock.
     * @param minimumStock The minimumStock to set
     */
    public void setMinimumStock(int minimumStock)
    {
        this.minimumStock = minimumStock;
    }

    /**
     * Gets the orderedUnits.
     * @return Returns a int
     */
    public int getOrderedUnits()
    {
        return orderedUnits;
    }

    /**
     * Sets the orderedUnits.
     * @param orderedUnits The orderedUnits to set
     */
    public void setOrderedUnits(int orderedUnits)
    {
        this.orderedUnits = orderedUnits;
    }

    /**
     * Gets the price.
     * @return Returns a double
     */
    public double getPrice()
    {
        return price;
    }

    /**
     * Sets the price.
     * @param price The price to set
     */
    public void setPrice(double price)
    {
        this.price = price;
    }

    /**
     * Sets the productGroup.
     * @param productGroup The productGroup to set
     */
    public void setProductGroup(ProductGroup productGroup)
    {
        this.productGroup = productGroup;
    }

    /**
     * Gets the productGroupId.
     * @return Returns a int
     */
    public int getProductGroupId()
    {
        return productGroupId;
    }

    /**
     * Sets the productGroupId.
     * @param productGroupId The productGroupId to set
     */
    public void setProductGroupId(int productGroupId)
    {
        this.productGroupId = productGroupId;
    }

    /**
     * Gets the supplierId.
     * @return Returns a int
     */
    public int getSupplierId()
    {
        return supplierId;
    }

    /**
     * Sets the supplierId.
     * @param supplierId The supplierId to set
     */
    public void setSupplierId(int supplierId)
    {
        this.supplierId = supplierId;
    }

    /**
     * Gets the unit.
     * @return Returns a String
     */
    public String getUnit()
    {
        return unit;
    }

    /**
     * Sets the unit.
     * @param unit The unit to set
     */
    public void setUnit(String unit)
    {
        this.unit = unit;
    }

}
