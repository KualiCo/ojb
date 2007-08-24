package org.apache.ojb.broker;

public class ArticleWithStockDetail implements java.io.Serializable
{

    /** return a string representation of an article*/
    public String toString()
    {
        return "----\n" +
                "Article No:   " + articleId + "\n" +
                "Description:  " + articleName + "\n" +
                "stock:        " + stock + "\n" +
                "price:        " + price + "\n" +
                "stock-value   " + getStockValue() + "\n";

    }

    /** maps to db-column "Artikel-Nr";INT;PrimaryKey*/
    protected int articleId;
    /** maps to db-column Artikelname;CHAR*/
    protected String articleName;
    /** maps to db-column Auslaufartikel;SMALL INT*/
    boolean isSelloutArticle;
    /** maps to db-column Mindestbestand;INT*/
    int minimumStock;
    /** maps to db-column BestellteEinheiten;INT*/
    int orderedUnits;

    /** this attribute is not filled through a reference lookup but with a RowReader !*/
    protected StockDetail stockDetail;


    /** maps to db-column Einzelpreis;DECIMAL*/
    protected double price;
    /** maps to db-column Kategorie-Nr;INT*/
    protected int productGroupId;
    /** maps to db-column Lagerbestand;INT*/
    int stock;
    /** maps to db-column Lieferanten-Nr;INT*/
    protected int supplierId;
    /** maps to db-column Liefereinheit;CHAR*/
    String unit;

    /** increase the amount of articles in stock by diff*/
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
    public ArticleWithStockDetail()
    {
    }

    /**
     * Insert the method's description here.
     * Creation date: (05.01.2001 19:21:38)
     */
    public ArticleWithStockDetail(
            int artId,
            String artName,
            int suppId,
            int pgId,
            String uni,
            double pric,
            int sto,
            int ordUnits,
            int minStock,
            boolean isSellout)
    {
        articleId = artId;
        articleName = artName;
        supplierId = suppId;
        productGroupId = pgId;
        unit = uni;
        price = pric;
        stock = sto;
        orderedUnits = ordUnits;
        minimumStock = minStock;
        isSelloutArticle = isSellout;
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
     * @return java.lang.String
     */
    public String getUnit()
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

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newUnit java.lang.String
     */
    public void setUnit(String newUnit)
    {
        unit = newUnit;
    }

    public StockDetail getDetail()
    {
        return stockDetail;
    }
    /**
     * Gets the stock.
     * @return Returns a int
     */
    public int getStock()
    {
        return stock;
    }

    /**
     * Sets the stock.
     * @param stock The stock to set
     */
    public void setStock(int stock)
    {
        this.stock = stock;
    }

}
