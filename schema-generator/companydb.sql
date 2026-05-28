CREATE TABLE employee(
    ssn CHAR(9),
    fname VARCHAR(50),
    minit CHAR(1),
    lname VARCHAR(50),
    bdate DATE,
    address VARCHAR(100),
    sex CHAR(1),
    salary DECIMAL(10,2),
    super_ssn CHAR(9),
    dno SMALLINT,
    CONSTRAINT pk_employee PRIMARY KEY (ssn),
    CONSTRAINT chk_employee_sex CHECK (sex IN ('M','F')),
    CONSTRAINT chk_salary CHECK (salary > 0)
);

CREATE TABLE department(
    dname VARCHAR(30),
    dnumber SMALLINT,
    mgr_ssn CHAR(9),
    mgr_start_date DATE,
    CONSTRAINT pk_department PRIMARY KEY (dnumber),
    CONSTRAINT uq_department_name UNIQUE (dname)
);

CREATE TABLE dept_locations(
    dnumber SMALLINT,
    dlocation VARCHAR(20),
    CONSTRAINT pk_dept_loc PRIMARY KEY (dnumber, dlocation)
);

CREATE TABLE project(
    pnumber SMALLINT,
    pname VARCHAR(30),
    plocation VARCHAR(30),
    dnum SMALLINT,
    CONSTRAINT pk_project PRIMARY KEY (pnumber),
    CONSTRAINT uq_project_name UNIQUE (pname)
);

CREATE TABLE works_on(
    essn CHAR(9),
    pno SMALLINT,
    hours DECIMAL(4,1),
    CONSTRAINT pk_works_on PRIMARY KEY (essn, pno),
    CONSTRAINT chk_hours CHECK (hours >= 0)
);

CREATE TABLE dependent(
    essn CHAR(9),
    dependent_name VARCHAR(30),
    sex CHAR(1),
    bdate DATE,
    relationship VARCHAR(20),
    CONSTRAINT pk_dependent PRIMARY KEY (essn, dependent_name)
);


INSERT INTO employee VALUES
('123456789','John','B','Smith','1965-01-09','731 Fondren, Houston, TX','M',30000,'333445555',5),
('333445555','Franklin','T','Wong','1955-01-09','638 Fondren, Houston, TX','M',40000,'888665555',5),
('999887777','Alicia','J','Zelaya','1968-01-09','3321 Fondren, Houston, TX','M',25000,'987654321',4),
('987654321','Jennifer','S','Wallace','1941-01-09','21 Fondren, Houston, TX','F',43000,'888665555',4),
('666884444','Ramesh','K','Narayan','1962-01-09','975 Fondren, Houston, TX','M',38000,'333445555',5),
('453453453','Joyce','A','English','1972-01-09','5631 Fondren, Houston, TX','F',25000,'333445555',5),
('987987987','Ahmad','V','Jabbar','1969-01-09','980 Fondren, Houston, TX','M',25000,'987654321',4),
('888665555','James','E','Borg','1937-01-09','450 Fondren, Houston, TX','M',55000,NULL,1);

INSERT INTO department VALUES
('Research',5,'333445555','1988-05-22'),
('Administration',4,'987654321','1995-05-22'),
('Headquarters',1,'888665555','1981-05-22');

INSERT INTO dept_locations VALUES
(1, 'Houston'),
(4, 'Stafford'),
(5, 'Bellaire'),
(5, 'Sugarland'),
(5, 'Houston');

INSERT INTO project VALUES
(1,'ProductX','Bellaire',5),
(2,'ProductY','Sugarland',5),
(3,'ProductZ','Houston',5),
(10,'Computerization','Stafford',4),
(20,'Reorganization','Houston',1),
(30,'Newbenefits','Stafford',4);

INSERT INTO works_on VALUES
('123456789', 1, 32.5),
('123456789', 2, 7.5),
('666884444', 3, 40.0),
('453453453', 1, 20.0),
('453453453', 2, 20.0),
('333445555', 2, 10.0),
('333445555', 3, 10.0),
('333445555', 10, 10.0),
('333445555', 20, 10.0),
('999887777', 30, 30.0),
('999887777', 10, 10.0),
('987987987', 10, 35.0),
('987987987', 30, 5.0),
('987654321', 30, 20.0),
('987654321', 20, 15.0);

INSERT INTO dependent VALUES
('333445555', 'Alice', 'F', '1986-04-05', 'Daughter'),
('333445555', 'Theodore', 'M', '1983-04-05', 'Son'),
('333445555', 'Joy', 'F', '1958-04-05', 'Spouse'),
('987654321', 'Abner', 'M', '1942-04-05', 'Spouse'),
('123456789', 'Michael', 'M', '1988-04-05', 'Son'),
('123456789', 'Alice', 'M', '1988-04-05', 'Daughter'),
('123456789', 'Elizabeth', 'F', '1967-04-05', 'Spouse');


ALTER TABLE employee
ADD CONSTRAINT fk_super_ssn FOREIGN KEY (super_ssn)
REFERENCES employee(ssn)
ON DELETE SET NULL;

ALTER TABLE employee
ADD CONSTRAINT fk_dno FOREIGN KEY (dno)
REFERENCES department(dnumber)
ON DELETE SET NULL;

ALTER TABLE department
ADD CONSTRAINT fk_mgr_ssn FOREIGN KEY (mgr_ssn)
REFERENCES employee(ssn)
ON DELETE SET NULL;

ALTER TABLE dept_locations
ADD CONSTRAINT fk_dnumber FOREIGN KEY (dnumber)
REFERENCES department(dnumber);

ALTER TABLE project
ADD CONSTRAINT fk_dnum FOREIGN KEY (dnum)
REFERENCES department(dnumber);

ALTER TABLE works_on
ADD CONSTRAINT fk_essn FOREIGN KEY (essn)
REFERENCES employee(ssn);

ALTER TABLE works_on
ADD CONSTRAINT fk_pno FOREIGN KEY (pno)
REFERENCES project(pnumber);

ALTER TABLE dependent
ADD CONSTRAINT fk_dep_essn FOREIGN KEY (essn)
REFERENCES employee(ssn);