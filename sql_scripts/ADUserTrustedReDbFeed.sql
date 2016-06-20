CREATE TABLE MY_ADUSER_RE_FEED
(	
	"Manager ID" VARCHAR2(256 CHAR), 
	"Trusted Status" VARCHAR2(25 CHAR), 
	"objectGUID" VARCHAR2(300 CHAR), 
	"User Id" VARCHAR2(256 CHAR), 
	"First Name" VARCHAR2(150 CHAR), 
	"Last Name" VARCHAR2(150 CHAR), 
	"Middle Name" VARCHAR2(80 CHAR), 
	"User Type" VARCHAR2(30 CHAR), 
	"Employee Type" VARCHAR2(255 CHAR),  
	"Organization" VARCHAR2(256 CHAR), 
	"E Mail" VARCHAR2(256 CHAR)
);

INSERT INTO MY_ADUSER_RE_FEED ("Manager ID", "Trusted Status", "objectGUID", 
"User Id", "First Name", "Last Name" , "Middle Name", "User Type", "Employee Type",  
"Organization", "E Mail") VALUES ('xelsysadm', 'Active', 'JPROUDMOORE',
'JPROUDMOORE', 'Jaina', 'Proudmoore', 'M', 'End-User', 'EMP',
'Xellerate Users', 'jproudmoore@wow.bnet');

INSERT INTO MY_ADUSER_RE_FEED ("Manager ID", "Trusted Status", "objectGUID", 
"User Id", "First Name", "Last Name" , "Middle Name", "User Type", "Employee Type",  
"Organization", "E Mail") VALUES ('xelsysadm', 'Disabled', 'EVANCLEEF',
'EVANCLEEF', 'Edwin', 'Vancleef', 'D', 'End-User', 'Full-Time',
'Xellerate Users', 'evancleef@wow.bnet');

INSERT INTO MY_ADUSER_RE_FEED ("Manager ID", "Trusted Status", "objectGUID", 
"User Id", "First Name", "Last Name" , "Middle Name", "User Type", "Employee Type",  
"Organization", "E Mail") VALUES ('JPROUDMOORE', 'Active', 'KTHUZAD',
'KTHUZAD', 'Kel', 'Thuzad', 'Z', 'End-User', 'Temp',
'Xellerate Users', 'kthuzad@wow.bnet');
