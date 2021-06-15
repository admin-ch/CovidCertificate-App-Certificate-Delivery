package ch.admin.bag.covidcertificate.backend.delivery.data.mapper;

import ch.admin.bag.covidcertificate.backend.delivery.data.impl.PushRegistrationWrapper;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushType;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class PushRegistrationRowMapper implements RowMapper<PushRegistrationWrapper> {

    @Override
    public PushRegistrationWrapper mapRow(ResultSet rs, int rowNum) throws SQLException {
        var pushRegistration = new PushRegistration();
        pushRegistration.setPushToken(rs.getString("push_token"));
        pushRegistration.setPushType(PushType.valueOf(rs.getString("push_type")));
        return new PushRegistrationWrapper(rs.getInt("pk_push_registration_id"), pushRegistration);
    }
}
