--@(#) script.ddl
   
CREATE TABLE CompositionTable.Composition.Customer
(
	name,
	id_Customer integer,
	PRIMARY KEY(id_Customer)
);  
  
CREATE SEQUENCE CompositionTable.Composition.Order_SEQ;

CREATE SEQUENCE CompositionTable.Composition.Customer_SEQ;

CREATE SEQUENCE CompositionTable.Composition.Prescription_SEQ;
   ---SKIPPED: Class REMOVED_BY_CUSTOM_TRANSFORMATION
  
CREATE TABLE CompositionTable.Composition.Order
(
	id_Order integer,
	fk_Customerid_Customer integer NOT NULL,
	PRIMARY KEY(id_Order),
	FOREIGN KEY(fk_Customerid_Customer) REFERENCES CompositionTable.Composition.Customer (id_Customer)
);  
  
CREATE TABLE CompositionTable.Composition.Prescription
(
	content,
	id_Prescription integer,
	fk_Orderid_Order integer NOT NULL,
	PRIMARY KEY(id_Prescription, fk_Orderid_Order),
	FOREIGN KEY(fk_Orderid_Order) REFERENCES CompositionTable.Composition.Order (id_Order)
);  
  