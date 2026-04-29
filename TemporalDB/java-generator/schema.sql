-- Auto-generated from Companydb_Schema_XML

CREATE TABLE `Employee_history` (
  `history_id` INT AUTO_INCREMENT PRIMARY KEY,
  `ssn` VARCHAR(255) NOT NULL,
  `salary` FLOAT,
  `dno` INT,
  `super_ssn` VARCHAR(255),
  `valid_from` DATETIME NOT NULL,
  `valid_to` DATETIME NULL DEFAULT NULL,
  `tx_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE `Department_history` (
  `history_id` INT AUTO_INCREMENT PRIMARY KEY,
  `dnumber` INT NOT NULL,
  `mgr_ssn` VARCHAR(255),
  `valid_from` DATETIME NOT NULL,
  `valid_to` DATETIME NULL DEFAULT NULL,
  `tx_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE `works_on_history` (
  `history_id` INT AUTO_INCREMENT PRIMARY KEY,
  `essn` VARCHAR(255) NOT NULL,
  `pno` INT NOT NULL,
  `hours` FLOAT,
  `valid_from` DATETIME NOT NULL,
  `valid_to` DATETIME NULL DEFAULT NULL,
  `tx_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

