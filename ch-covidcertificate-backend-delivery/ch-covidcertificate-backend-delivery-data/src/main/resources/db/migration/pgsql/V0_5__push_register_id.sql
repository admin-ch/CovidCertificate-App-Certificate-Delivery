/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

ALTER TABLE t_push_registration
    ADD COLUMN register_id character varying(255) UNIQUE NOT NULL;