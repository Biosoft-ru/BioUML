DROP TABLE IF EXISTS sequence_data;
DROP TABLE IF EXISTS experiment_condition;
DROP TABLE IF EXISTS `condition`;
DROP TABLE IF EXISTS experiment_pubmed;
DROP TABLE IF EXISTS pubmed;
DROP TABLE IF EXISTS experiment;
DROP TABLE IF EXISTS sequence_adapter;
DROP TABLE IF EXISTS geo_sample;
DROP TABLE IF EXISTS geo_series;
DROP TABLE IF EXISTS sra_experiment;
DROP TABLE IF EXISTS sra_project;
DROP TABLE IF EXISTS sequencing_platform;
DROP TABLE IF EXISTS cell_source;
DROP TABLE IF EXISTS species;

CREATE TABLE species (
  species_id VARCHAR(7) NOT NULL,
  latin_name LONGTEXT,
  common_name LONGTEXT,
  PRIMARY KEY(species_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE cell_source (
  cell_source_id VARCHAR(7) NOT NULL,
  title LONGTEXT,
  PRIMARY KEY(cell_source_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE sequencing_platform (
  sequencing_platform_id VARCHAR(7) NOT NULL,
  title LONGTEXT NOT NULL,
  PRIMARY KEY(sequencing_platform_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE sra_project (
  sra_project_id VARCHAR(9),
  PRIMARY KEY(sra_project_id)
) ENGINE=InnoDB CHARACTER SET=UTF8;

CREATE TABLE sra_experiment (
  sra_experiment_id VARCHAR(12),
  PRIMARY KEY(sra_experiment_id)
) ENGINE=InnoDB CHARACTER SET=UTF8;

CREATE TABLE geo_series (
  geo_series_id VARCHAR(8),
  PRIMARY KEY(geo_series_id)
) ENGINE=InnoDB CHARACTER SET=UTF8;

CREATE TABLE geo_sample (
  geo_sample_id VARCHAR(10),
  PRIMARY KEY(geo_sample_id)
) ENGINE=InnoDB CHARACTER SET=UTF8;

CREATE TABLE sequence_adapter(
  sequence_adapter_id VARCHAR(7) NOT NULL,
  title TEXT NOT NULL,
  sequence LONGTEXT NOT NULL,
  PRIMARY KEY(sequence_adapter_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE experiment (
  experiment_id VARCHAR(9) NOT NULL,
  title LONGTEXT,
  description LONGTEXT,
  species_id VARCHAR(7),
  cell_source_id VARCHAR(7),
  translation_inhibition LONGTEXT,
  min_fragment_size INTEGER,
  max_fragment_size INTEGER,
  digestion LONGTEXT,
  sequence_adapter_id VARCHAR(7),
  sequencing_platform_id VARCHAR(7),
  sra_project_id VARCHAR(9),
  sra_experiment_id VARCHAR(12),
  geo_series_id VARCHAR(8),
  geo_sample_id VARCHAR(10),
  PRIMARY KEY(experiment_id),
  FOREIGN KEY(species_id)
    REFERENCES species(species_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(cell_source_id)
    REFERENCES cell_source(cell_source_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(sequencing_platform_id)
    REFERENCES sequencing_platform(sequencing_platform_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(sra_project_id)
    REFERENCES sra_project(sra_project_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(sra_experiment_id)
    REFERENCES sra_experiment(sra_experiment_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(geo_series_id)
    REFERENCES geo_series(geo_series_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(geo_sample_id)
    REFERENCES geo_sample(geo_sample_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(sequence_adapter_id)
    REFERENCES sequence_adapter(sequence_adapter_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE pubmed(
  pubmed_id INTEGER NOT NULL,
  PRIMARY KEY(pubmed_id)
) ENGINE=InnoDB CHARACTER SET=UTF8;

CREATE TABLE experiment_pubmed (
  experiment_pubmed_id INTEGER NOT NULL AUTO_INCREMENT,
  experiment_id VARCHAR(9) NOT NULL,
  pubmed_id INTEGER NOT NULL,
  PRIMARY KEY(experiment_pubmed_id),
  FOREIGN KEY(experiment_id)
    REFERENCES experiment(experiment_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY(pubmed_id)
    REFERENCES pubmed(pubmed_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE `condition` (
  condition_id VARCHAR(9) NOT NULL,
  description LONGTEXT,
  PRIMARY KEY(condition_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE experiment_condition(
  experiment_condition_id INTEGER NOT NULL AUTO_INCREMENT,
  experiment_id VARCHAR(9) NOT NULL,
  condition_id VARCHAR(9) NOT NULL,
  PRIMARY KEY(experiment_condition_id),
  FOREIGN KEY(experiment_id)
    REFERENCES experiment(experiment_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY(condition_id)
    REFERENCES `condition`(condition_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE sequence_data(
  sequence_data_id INTEGER NOT NULL AUTO_INCREMENT,
  experiment_id VARCHAR(9) NOT NULL,
  format ENUM('FASTA','FASTQ','SRA','ELAND'),
  url LONGTEXT NOT NULL,
  PRIMARY KEY(sequence_data_id),
  FOREIGN KEY(experiment_id)
    REFERENCES experiment(experiment_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE mrna_experiment (
  mrna_experiment_id VARCHAR(10) NOT NULL,
  title LONGTEXT,
  description LONGTEXT,
  species_id VARCHAR(7),
  cell_source_id VARCHAR(7),
  sequencing_platform_id VARCHAR(7),
  sra_project_id VARCHAR(9),
  sra_experiment_id VARCHAR(9),
  geo_series_id VARCHAR(8),
  geo_sample_id VARCHAR(10),
  PRIMARY KEY(mrna_experiment_id),
  FOREIGN KEY(species_id)
    REFERENCES species(species_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(cell_source_id)
    REFERENCES cell_source(cell_source_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(sequencing_platform_id)
    REFERENCES sequencing_platform(sequencing_platform_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(sra_project_id)
    REFERENCES sra_project(sra_project_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(sra_experiment_id)
    REFERENCES sra_experiment(sra_experiment_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(geo_series_id)
    REFERENCES geo_series(geo_series_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  FOREIGN KEY(geo_sample_id)
    REFERENCES geo_sample(geo_sample_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB CHARACTER SET=UTF8;

CREATE TABLE mrna_experiment_pubmed (
  mrna_experiment_pubmed_id INTEGER NOT NULL AUTO_INCREMENT,
  mrna_experiment_id VARCHAR(10) NOT NULL,
  pubmed_id INTEGER NOT NULL,
  PRIMARY KEY(mrna_experiment_pubmed_id),
  FOREIGN KEY(mrna_experiment_id)
    REFERENCES mrna_experiment(mrna_experiment_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY(pubmed_id)
    REFERENCES pubmed(pubmed_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE mrna_experiment_condition(
  mrna_experiment_condition_id INTEGER NOT NULL AUTO_INCREMENT,
  mrna_experiment_id VARCHAR(10) NOT NULL,
  condition_id VARCHAR(9) NOT NULL,
  PRIMARY KEY(mrna_experiment_condition_id),
  FOREIGN KEY(mrna_experiment_id)
    REFERENCES mrna_experiment(mrna_experiment_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY(condition_id)
    REFERENCES `condition`(condition_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE mrna_sequence_data(
  mrna_sequence_data_id INTEGER NOT NULL AUTO_INCREMENT,
  mrna_experiment_id VARCHAR(10) NOT NULL,
  format ENUM('FASTA','FASTQ','SRA','ELAND'),
  url LONGTEXT NOT NULL,
  PRIMARY KEY(mrna_sequence_data_id),
  FOREIGN KEY(mrna_experiment_id)
    REFERENCES mrna_experiment(mrna_experiment_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=UTF8;

CREATE TABLE riboseq_mrna(
  riboseq_mrna_id VARCHAR(10) NOT NULL,
  riboseq_experiment_id VARCHAR(9) NOT NULL,
  mrna_experiment_id VARCHAR(10) NOT NULL,
  PRIMARY KEY(riboseq_mrna_id),
  FOREIGN KEY(riboseq_experiment_id)
    REFERENCES experiment(experiment_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY(mrna_experiment_id)
    REFERENCES mrna_experiment(mrna_experiment_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB CHARACTER SET=UTF8;