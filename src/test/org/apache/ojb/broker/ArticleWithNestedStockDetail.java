package org.apache.ojb.broker;

public class ArticleWithNestedStockDetail implements java.io.Serializable
{
    /** maps to db-column "Artikel-Nr";INT;PrimaryKey*/
    protected int articleId;
    /** maps to db-column Artikelname;CHAR*/
    protected String articleName;
 
    /** this attribute is not filled through a reference lookup but with a RowReader !*/
    protected StockDetail stockDetail;


    /** maps to db-column Einzelpreis;DECIMAL*/
    protected double price;
    /** maps to db-column Kategorie-Nr;INT*/
    protected int productGroupId;
    /** maps to db-column Lieferanten-Nr;INT*/
    protected int supplierId;

    /** increase the amount of articles in stock by diff*/
    public void addToStock(int diff)
    {
        stockDetail.stock += diff;
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

    public double getStockValue()
    {
        return price * stockDetail.getStock();
    }

    /**
     * set an articles unique id.
     * @param newArticleId int
     */
    public void setArticleId(int newArticleId)
    {
        articleId = newArticleId;
    }

    /**
     * set an articles name.
     * @param newArticleName java.lang.String
     */
    public void setArticleName(String newArticleName)
    {
        articleName = newArticleName;
    }

    /**
     * Insert the method's description here.
     * Creation date: (05.01.2001 19:31:04)
     */
    public ArticleWithNestedStockDetail()
    {
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
    public int getProductGroupId()
    {
        return productGroupId;
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
     * @param newPrice double
     */
    public void setPrice(double newPrice)
    {
        price = newPrice;
    }


    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newProductGroupId int
     */
    public void setProductGroupId(int newProductGroupId)
    {
        productGroupId = newProductGroupId;
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


    public StockDetail getDetail()
    {
        return stockDetail;
    }


    /**
     * Returns the stockDetail.
     * @return StockDetail
     */
    public StockDetail getStockDetail()
    {
        return stockDetail;
    }

    /**
     * Sets the stockDetail.
     * @param stockDetail The stockDetail to set
     */
    public void setStockDetail(StockDetail stockDetail)
    {
        this.stockDetail = stockDetail;
    }

}
