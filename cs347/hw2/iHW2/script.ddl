--@(#) script.ddl

CREATE SCHEMA SalesSqlPackage.Salespackage;

CREATE TABLE SalesSqlPackage.Salespackage.Order
(
	orderDate date,
	soldBy,
	total,
	id_Order integer,
	PRIMARY KEY(id_Order)
);

CREATE TABLE SalesSqlPackage.Salespackage.Product
(
	UPC,
	prodName,
	mfgr,
	model,
	unitListPrice,
	UnitsinStock,
	id_Product integer,
	PRIMARY KEY(id_Product)
);

CREATE TABLE SalesSqlPackage.Salespackage.OrderLine
(
	quantity,
	unitSalePrice,
	subtotal,
	id_OrderLine integer,
	fk_Orderid_Order integer NOT NULL,
	fk_Productid_Product integer NOT NULL,
	PRIMARY KEY(id_OrderLine),
	FOREIGN KEY(fk_Orderid_Order) REFERENCES SalesSqlPackage.Salespackage.Order (id_Order),
	FOREIGN KEY(fk_Productid_Product) REFERENCES SalesSqlPackage.Salespackage.Product (id_Product)
);
