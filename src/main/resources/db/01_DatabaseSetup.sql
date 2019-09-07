-- TABLES CREATION

CREATE TABLE PLACE (
    ID bigint(20) NOT NULL AUTO_INCREMENT,
    NAME varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    LATITUDE double NOT NULL,
    LONGITUDE double NOT NULL,
    ELEVATION double NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT cannot_duplicate_place_name UNIQUE KEY(NAME),
    CONSTRAINT cannot_duplicate_place_cords UNIQUE KEY(LATITUDE, LONGITUDE)
);

CREATE TABLE USER (
    ID bigint(20) NOT NULL AUTO_INCREMENT,
    USERNAME varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    EMAIL varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    PASSWORD varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
    ROLE ENUM('user', 'admin') NOT NULL DEFAULT 'user',
    BANNED boolean NOT NULL DEFAULT false,
    DELETED boolean NOT NULL DEFAULT false,
    PRIMARY KEY (ID),
    CONSTRAINT cannot_duplicate_username UNIQUE KEY(USERNAME),
    CONSTRAINT cannot_duplicate_email UNIQUE KEY(EMAIL)
);

CREATE TABLE OPINION (
    ID bigint(20) NOT NULL AUTO_INCREMENT,
    UUID char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
    PLACE_ID bigint(20) NOT NULL,
    AUTHOR_ID bigint(20) NOT NULL,
    BODY varchar(4096) DEFAULT NULL,
    REFERENCE_DATE timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATION_DATE timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LAST_MODIFIED timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    BLOCKED boolean NOT NULL DEFAULT false,
    DELETED boolean NOT NULL DEFAULT false,
    PRIMARY KEY (ID),
    UNIQUE KEY (UUID),
    FOREIGN KEY (PLACE_ID) REFERENCES PLACE (ID),
    FOREIGN KEY (AUTHOR_ID) REFERENCES USER (ID)
);

CREATE TABLE TAG (
    ID bigint(20) NOT NULL AUTO_INCREMENT,
    UUID char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
    NAME varchar(80) NOT NULL,
    ENABLED boolean DEFAULT true,
    TAG_CATEGORY enum('subsoil', 'equipment', 'threats') NOT NULL,
    PRIMARY KEY (ID),
    UNIQUE KEY (UUID)
);

CREATE TABLE OPINION_TAG (
    OPINION_ID bigint(20) NOT NULL,
    TAG_ID bigint(20) NOT NULL,
    PRIMARY KEY (OPINION_ID, TAG_ID),
    FOREIGN KEY (OPINION_ID) REFERENCES OPINION (ID),
    FOREIGN KEY (TAG_ID) REFERENCES TAG (ID)
);

CREATE TABLE REPORT (
    ID bigint(20) NOT NULL AUTO_INCREMENT,
    AUTHOR_ID bigint(20) NOT NULL,
    OPINION_ID bigint(20) NOT NULL,
    REPORT_CATEGORY enum('misleading', 'vulgar', 'faulty') NOT NULL,
    BODY varchar(4096) DEFAULT NULL,
    CREATION_DATE timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ID),
    FOREIGN KEY (AUTHOR_ID) REFERENCES USER (ID),
    FOREIGN KEY (OPINION_ID) REFERENCES OPINION (ID)
);

CREATE TABLE OPINION_LIKE (
    OPINION_ID bigint(20) NOT NULL,
    USER_ID bigint(20) NOT NULL,
    FOREIGN KEY (OPINION_ID) REFERENCES OPINION (ID),
    FOREIGN KEY (USER_ID) REFERENCES USER (ID)
);
