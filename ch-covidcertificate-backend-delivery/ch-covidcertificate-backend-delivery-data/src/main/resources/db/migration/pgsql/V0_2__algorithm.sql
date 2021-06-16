/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

delete from t_transfer;
alter table t_transfer add column algorithm character varying(10) not null;