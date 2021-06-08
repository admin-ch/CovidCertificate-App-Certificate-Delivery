package ch.admin.bag.covidcertificate.backend.delivery.data.impl;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeAlreadyExistsException;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeNotFoundException;
import ch.admin.bag.covidcertificate.backend.delivery.data.mapper.CovidCertRowMapper;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class JdbcDeliveryDataServiceImpl implements DeliveryDataService {
    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert transferInsert;
    private final SimpleJdbcInsert pushRegistrationInsert;
    private final SimpleJdbcInsert covidCertInsert;

    public JdbcDeliveryDataServiceImpl(DataSource dataSource) {
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
    }

    @Override
    @Transactional(readOnly = false)
    public void initTransfer(DeliveryRegistration registration) throws CodeAlreadyExistsException {
        if (!transferCodeExists(registration.getCode())) {
            transferInsert.execute(createTransferParams(registration));
        } else {
            throw new CodeAlreadyExistsException();
        }
    }

    private boolean transferCodeExists(String code) {
        return jt.queryForObject(
                "select exists(" + "select * from t_transfer" + " where code = :code" + ")",
                new MapSqlParameterSource("code", code),
                Boolean.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CovidCert> findCovidCerts(String code) throws CodeNotFoundException {
        Integer pkTransferId = null;
        try {
            pkTransferId =
                    jt.queryForObject(
                            "select pk_transfer_id from t_transfer where code = :code",
                            new MapSqlParameterSource("code", code),
                            Integer.class);
        } catch (EmptyResultDataAccessException e) {
            throw new CodeNotFoundException();
        }

        String sql = "select * from t_covidcert where fk_transfer_id = :fk_transfer_id";
        return jt.query(
                sql,
                new MapSqlParameterSource("fk_transfer_id", pkTransferId),
                new CovidCertRowMapper());
    }

    @Override
    @Transactional(readOnly = false)
    public void closeTransfer(String code) throws CodeNotFoundException {
        if (transferCodeExists(code)) {
            jt.update(
                    "delete from t_transfer where code = :code",
                    new MapSqlParameterSource("code", code));
        } else {
            throw new CodeNotFoundException();
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void insertPushRegistration(PushRegistration registration) {
        if (!pushRegistrationExists(registration)) {
            pushRegistrationInsert.execute(createPushRegistrationParams(registration));
        }
    }

    private boolean pushRegistrationExists(PushRegistration registration) {
        String sql =
                "select exists("
                        + "select * from t_push_registration"
                        + " where push_token = :push_token"
                        + " and push_type = :push_type"
                        + ")";
        MapSqlParameterSource params = createPushRegistrationParams(registration);
        return jt.queryForObject(sql, params, Boolean.class);
    }

    @Override
    @Transactional(readOnly = false)
    public void removePushRegistration(PushRegistration registration) {
        String sql =
                "delete from t_push_registration"
                        + " where push_token = :push_token"
                        + " and push_type = :push_type";
        jt.update(sql, createPushRegistrationParams(registration));
    }

    private MapSqlParameterSource createPushRegistrationParams(PushRegistration registration) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("push_token", registration.getPushToken());
        params.addValue("push_type", registration.getPushType().name());
        return params;
    }

    private MapSqlParameterSource createTransferParams(DeliveryRegistration registration) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("code", registration.getCode());
        params.addValue("public_key", registration.getPublicKey());
        return params;
    }
}
