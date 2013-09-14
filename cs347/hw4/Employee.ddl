--@(#) script.ddl
  
CREATE TABLE EmployeeTable.Employee.Employee
(
	firstName,
	lastName,
	phone,
	jobTitle,
	salary,
	id_Employee integer,
	PRIMARY KEY(id_Employee)
);  
   
CREATE SEQUENCE EmployeeTable.Employee.Employee_SEQ;
  
CREATE TABLE EmployeeTable.Employee.SalesRepresentive
(
	commission,
	id_Employee integer,
	PRIMARY KEY(id_Employee),
	FOREIGN KEY(id_Employee) REFERENCES EmployeeTable.Employee.Employee (id_Employee)
);  
  