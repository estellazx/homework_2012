--@(#) script.ddl
   
CREATE TABLE AggregationTable.Aggregation.Customer
(
	name,
	id_Customer integer,
	PRIMARY KEY(id_Customer)
);  
  
CREATE SEQUENCE AggregationTable.Aggregation.Order_SEQ;

CREATE SEQUENCE AggregationTable.Aggregation.Customer_SEQ;

CREATE SEQUENCE AggregationTable.Aggregation.Prescription_SEQ;
   ---SKIPPED: Class REMOVED_BY_CUSTOM_TRANSFORMATION
  
CREATE TABLE AggregationTable.Aggregation.Order
(
	id_Order integer,
	fk_Customerid_Customer integer NOT NULL,
	PRIMARY KEY(id_Order),
	FOREIGN KEY(fk_Customerid_Customer) REFERENCES AggregationTable.Aggregation.Customer (id_Customer)
);  
  
CREATE TABLE AggregationTable.Aggregation.Prescription
(
	content,
	id_Prescription integer,
	fk_Orderid_Order integer NOT NULL,
	PRIMARY KEY(id_Prescription),
	FOREIGN KEY(fk_Orderid_Order) REFERENCES AggregationTable.Aggregation.Order (id_Order)
);  
  