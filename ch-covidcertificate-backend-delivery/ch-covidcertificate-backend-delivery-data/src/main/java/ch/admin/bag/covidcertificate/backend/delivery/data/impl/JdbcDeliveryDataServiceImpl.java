package ch.admin.bag.covidcertificate.backend.delivery.data.impl;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeAlreadyExistsException;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeNotFoundException;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.PublicKeyAlreadyExistsException;
import ch.admin.bag.covidcertificate.backend.delivery.data.mapper.CovidCertRowMapper;
import ch.admin.bag.covidcertificate.backend.delivery.data.mapper.PushRegistrationRowMapper;
import ch.admin.bag.covidcertificate.backend.delivery.data.mapper.TransferRowMapper;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushType;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbCovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbTransfer;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.logging.log4j.util.Strings;
import org.postgresql.util.PGInterval;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class JdbcDeliveryDataServiceImpl implements DeliveryDataService {

    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert transferInsert;
    private final SimpleJdbcInsert pushRegistrationInsert;
    private final SimpleJdbcInsert covidCertInsert;
    private final int pushBatchSize;

    public JdbcDeliveryDataServiceImpl(DataSource dataSource, int pushBatchSize) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.transferInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_transfer")
                        .usingGeneratedKeyColumns("pk_transfer_id", "created_at");
        this.pushRegistrationInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_push_registration")
                        .usingGeneratedKeyColumns("pk_push_registration_id", "created_at");
        this.covidCertInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_covidcert")
                        .usingGeneratedKeyColumns("pk_covidcert_id", "created_at");
        this.pushBatchSize = pushBatchSize;
    }

    @Override
    @Transactional(readOnly = false)
    public void initTransfer(DeliveryRegistration registration, Instant expiresAt, Instant failsAt)
            throws CodeAlreadyExistsException, PublicKeyAlreadyExistsException,
                    NoSuchAlgorithmException {
        if (transferCodeExists(registration.getCode())) {
            throw new CodeAlreadyExistsException();
        } else if (publicKeyExists(registration.getPublicKey())) {
            throw new PublicKeyAlreadyExistsException(
                    registration.getPublicKey(), registration.getCode());
        } else {
            transferInsert.execute(createTransferParams(registration, expiresAt, failsAt));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean transferCodeExists(String code) {
        return jt.queryForObject(
                "select exists(" + "select * from t_transfer" + " where code = :code" + ")",
                new MapSqlParameterSource("code", code),
                Boolean.class);
    }

    private boolean publicKeyExists(String publicKey) throws NoSuchAlgorithmException {
        return jt.queryForObject(
                "select exists("
                        + "select * from t_transfer"
                        + " where public_key_sha_256 = :public_key_sha_256"
                        + ")",
                new MapSqlParameterSource("public_key_sha_256", HashUtil.getSha256Hash(publicKey)),
                Boolean.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CovidCert> findCovidCerts(String code) throws CodeNotFoundException {
        String sql = "select * from t_covidcert where fk_transfer_id = :fk_transfer_id";
        return jt.query(
                sql,
                new MapSqlParameterSource("fk_transfer_id", findPkTransferId(code)),
                new CovidCertRowMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer findPkTransferId(String code) throws CodeNotFoundException {
        try {
            return jt.queryForObject(
                    "select pk_transfer_id from t_transfer where code = :code",
                    new MapSqlParameterSource("code", code),
                    Integer.class);
        } catch (EmptyResultDataAccessException e) {
            throw new CodeNotFoundException(code);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DbTransfer findTransfer(String code) throws CodeNotFoundException {
        try {
            return jt.queryForObject(
                    "select * from t_transfer where code = :code",
                    new MapSqlParameterSource("code", code),
                    new TransferRowMapper());
        } catch (EmptyResultDataAccessException e) {
            throw new CodeNotFoundException(code);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DbTransfer> findTransferWithoutCovidCert(Instant createdBefore) {
        String sql =
                "select * from t_transfer"
                        + " where pk_transfer_id not in (select fk_transfer_id from t_covidcert)"
                        + " and created_at < :created_before";
        return jt.query(
                sql,
                new MapSqlParameterSource("created_before", Date.from(createdBefore)),
                new TransferRowMapper());
    }

    @Override
    @Transactional(readOnly = false)
    public void closeTransfer(String code) throws CodeNotFoundException {
        if (transferCodeExists(code)) {
            jt.update(
                    "delete from t_transfer where code = :code",
                    new MapSqlParameterSource("code", code));
        } else {
            throw new CodeNotFoundException(code);
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void upsertPushRegistration(PushRegistration registration) {
        if (registration.getPushToken() == null || Strings.isBlank(registration.getPushToken())) {
            removeRegistration(registration.getRegisterId());
        } else if (!pushRegistrationExists(registration)) {
            pushRegistrationInsert.execute(createPushRegistrationParams(registration));
        } else {
            var sql =
                    "update t_push_registration "
                            + "set push_token = :push_token, register_id = :register_id "
                            + "where push_token = :push_token or register_id = :register_id";
            jt.update(sql, createPushRegistrationParams(registration));
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void updateLastPushTImes(Collection<PushRegistration> registrations){
        jt.batchUpdate("update t_push_registration set last_push = now() where pk_push_registration_id = :id",
            SqlParameterSourceUtils.createBatch(registrations));
    }

    private boolean pushRegistrationExists(PushRegistration registration) {
        String sql =
                "select exists("
                        + "select * from t_push_registration"
                        + " where push_token = :push_token"
                        + " or register_id = :register_id"
                        + ")";
        MapSqlParameterSource params = createPushRegistrationParams(registration);
        return jt.queryForObject(sql, params, Boolean.class);
    }

    @Override
    @Transactional(readOnly = false)
    public void removeRegistration(String registerId) {
        final var sql = "delete from t_push_registration where register_id = :register_id";
        jt.update(sql, new MapSqlParameterSource("register_id", registerId));
    }

    @Override
    @Transactional(readOnly = false)
    public void removeRegistrations(List<String> tokensToRemove) {
        if (tokensToRemove != null && !tokensToRemove.isEmpty()) {
            jt.update(
                    "delete from t_push_registration where push_token in (:tokensToRemove)",
                    new MapSqlParameterSource("tokensToRemove", tokensToRemove));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PushRegistration> getPushRegistrationByType(
            final PushType pushType, int prevMaxId) {
        final var sql =
                "select * from t_push_registration where push_type = :push_type and pk_push_registration_id > :prev_max_id order by pk_push_registration_id asc limit :batch_size";
        final var params = new MapSqlParameterSource("push_type", pushType.name());
        params.addValue("prev_max_id", prevMaxId);
        params.addValue("batch_size", pushBatchSize);
        return jt.query(sql, params, new PushRegistrationRowMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PushRegistration> getDuePushRegistrations(PushType pushType,
        Duration timeSinceLastPush, int limit) {
        final var sql =
            "select * from t_push_registration where push_type = :push_type and last_push is null or last_push < now()  - :interval order by last_push asc limit :batch_size";
        final var params = new MapSqlParameterSource("push_type", pushType.name());
        PGInterval interval =  new PGInterval();
        interval.setSeconds(timeSinceLastPush.toSeconds());
        params.addValue("interval", interval);
        params.addValue("batch_size", limit);
        return jt.query(sql, params, new PushRegistrationRowMapper());
    }

    @Override
    @Transactional(readOnly = false)
    public void insertCovidCert(DbCovidCert covidCert) {
        covidCertInsert.execute(createCovidCertParams(covidCert));
    }

    @Override
    @Transactional(readOnly = false)
    public void cleanDB() {
        var sql = "delete from t_transfer where fails_at < :retention_time";
        var retentionTime = Instant.now();
        var params = new MapSqlParameterSource("retention_time", Date.from(retentionTime));
        jt.update(sql, params);
    }

    @Override
    public int countRegistrations() {
        return jt.queryForObject("select count(pk_push_registration_id) from t_push_registration", new HashMap<>(), Integer.class);
    }

    private MapSqlParameterSource createPushRegistrationParams(PushRegistration registration) {
        var params = new MapSqlParameterSource();
        params.addValue("push_token", registration.getPushToken());
        params.addValue("push_type", registration.getPushType().name());
        params.addValue("register_id", registration.getRegisterId());
        return params;
    }

    private MapSqlParameterSource createTransferParams(DeliveryRegistration registration, Instant expiresAt, Instant failsAt)
            throws NoSuchAlgorithmException {
        var params = new MapSqlParameterSource();
        params.addValue("code", registration.getCode());
        params.addValue("public_key", registration.getPublicKey());
        params.addValue("public_key_sha_256", HashUtil.getSha256Hash(registration.getPublicKey()));
        params.addValue("algorithm", registration.getAlgorithm().name());
        params.addValue("expires_at", Date.from(expiresAt));
        params.addValue("fails_at", Date.from(failsAt));
        return params;
    }

    private MapSqlParameterSource createCovidCertParams(DbCovidCert covidCert) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("fk_transfer_id", covidCert.getFkTransfer());
        params.addValue("encrypted_hcert", covidCert.getEncryptedHcert());
        params.addValue("encrypted_pdf", covidCert.getEncryptedPdf());
        return params;
    }
}
