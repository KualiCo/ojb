package org.apache.ojb.broker;

/**
 * @ojb.class generate-table-info="false"
 */
public abstract class AbstractArticle implements InterfaceArticle, java.io.Serializable
{

    /** return a string representaion of an article*/
    public String toString()
    {
        String groupId = "" + productGroupId;
        String groupName = productGroup != null ? productGroup.getName() : null;

        return "----\n" +
                "Article No:   " + articleId + "\n" +
                "Description:  " + articleName + "\n" +
                "ProductGroupId: " + groupId + "\n" +
                "ProductGroupName: " + groupName + "\n" +
                "stock:        " + stock + "\n" +
                "price:        " + price + "\n" +
                "stock-value   " + getStockValue() + "\n";

    }

    /** maps to db-column "Artikel-Nr";INT;PrimaryKey*/
    protected Integer articleId;
    /** maps to db-column Artikelname;CHAR*/
    protected String articleName;

    /**
     * maps to db-column Auslaufartikel;SMALL INT
     * @ojb.field column="Auslaufartikel"
     *            jdbc-type="INTEGER"
     *            conversion="org.apache.ojb.broker.accesslayer.conversions.Boolean2IntFieldConversion"
     *            id="10"
     * @ojb.attribute attribute-name="color"
     *                attribute-value="green"
     * @ojb.attribute attribute-name="size"
     *                attribute-value="small"
     */
    protected boolean isSelloutArticle;
    /** maps to db-column Mindestbestand;INT*/
    protected int minimumStock;
    /** maps to db-column BestellteEinheiten;INT*/
    protected int orderedUnits;
    /** maps to db-column Einzelpreis;DECIMAL*/
    protected double price;
    /** reference to the articles category*/
    protected InterfaceProductGroup productGroup;
    /** maps to db-column Kategorie-Nr;INT*/
    protected Integer productGroupId;
    /** maps to db-column Lagerbestand;INT*/
    protected int stock;
    /** maps to db-column Lieferanten-Nr;INT*/
    protected int supplierId;
    /** maps to db-column Liefereinheit;CHAR*/
    protected String unit;

    /**
     * Insert the method's description here.
     * Creation date: (05.01.2001 19:31:04)
     */
    public AbstractArticle()
    {
    }

    /**
     * return an articles unique id.
     * @return the articles unique id
     */
    public Integer getArticleId()
    {
        return articleId;
    }

    /**
     * set an articles unique id.
     * @param newArticleId int
     */
    public void setArticleId(Integer newArticleId)
    {
        articleId = newArticleId;
    }

    public void setProductGroup(InterfaceProductGroup newProductGroup)
    {
        productGroup = newProductGroup;
    }

    public void setProductGroupId(Integer newProductGroupId)
    {
        productGroupId = newProductGroupId;
    }

    public InterfaceProductGroup getProductGroup()
    {
        return productGroup;
    } /** compute the total value of an articles stock*/

    public Integer getProductGroupId()
    {
        return productGroupId;
    }

    /** increase the amount of articles in stock by diff*/
    public void addToStock(int diff)
    {
        stock += diff;
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

    public double getStockValue()
    {
        return price * stock;
    }

    /**
     * set an articles name.
     * @param newArticleName java.lang.String
     */
    public void setArticleName(java.lang.String newArticleName)
    {
        articleName = newArticleName;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return boolean
     */
    public boolean getIsSelloutArticle()
    {
        return isSelloutArticle;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return int
     */
    public int getMinimumStock()
    {
        return minimumStock;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return int
     */
    public int getOrderedUnits()
    {
        return orderedUnits;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return double
     */
    public double getPrice()
    {
        return price;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return int
     */
    public int getStock()
    {
        return stock;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return int
     */
    public int getSupplierId()
    {
        return supplierId;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return java.lang.String
     */
    public java.lang.String getUnit()
    {
        return unit;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newIsSelloutArticle int
     */
    public void setIsSelloutArticle(boolean newIsSelloutArticle)
    {
        isSelloutArticle = newIsSelloutArticle;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newMinimumStock int
     */
    public void setMinimumStock(int newMinimumStock)
    {
        minimumStock = newMinimumStock;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newOrderedUnits int
     */
    public void setOrderedUnits(int newOrderedUnits)
    {
        orderedUnits = newOrderedUnits;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newPrice double
     */
    public void setPrice(double newPrice)
    {
        price = newPrice;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newStock int
     */
    public void setStock(int newStock)
    {
        stock = newStock;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newSupplierId int
     */
    public void setSupplierId(int newSupplierId)
    {
        supplierId = newSupplierId;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newUnit java.lang.String
     */
    public void setUnit(java.lang.String newUnit)
    {
        unit = newUnit;
    }
}
