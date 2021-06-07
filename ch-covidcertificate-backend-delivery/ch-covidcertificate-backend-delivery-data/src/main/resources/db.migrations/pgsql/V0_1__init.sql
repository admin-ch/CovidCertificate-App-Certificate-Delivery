/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

CREATE TABLE t_transfer
(
    pk_transfer_id serial                   NOT NULL,
    created_at     timestamp with time zone NOT NULL DEFAULT now(),
    code           character varying(9)     NOT NULL,
    public_key     text                     NOT NULL,
    CONSTRAINT PK_t_transfer PRIMARY KEY (pk_transfer_id),
    CONSTRAINT unique_transfer_code UNIQUE (code)
);

CREATE TABLE t_push_registration
(
    pk_push_registration_id serial                  NOT NULL,
    created_at              timestamp with time zone NOT NULL DEFAULT now(),
    push_token              character varying(4096) NOT NULL,
    push_type               character varying(3)   NOT NULL,
    CONSTRAINT PK_t_push_registration PRIMARY KEY (pk_push_registration_id)
);

CREATE TABLE t_covidcert
(
    pk_covidcert_id serial                   NOT NULL,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    encrypted_hcert text                     NOT NULL,
    encrypted_pdf   text                     NOT NULL,
    fk_transfer_id  integer                  NOT NULL,
    CONSTRAINT PK_t_covidcert PRIMARY KEY (pk_covidcert_id),
    CONSTRAINT FK_20 FOREIGN KEY (fk_transfer_id) REFERENCES t_transfer (pk_transfer_id)
);

CREATE INDEX fkIdx_21 ON t_covidcert ( fk_transfer_id );