package ch.admin.bag.covidcertificate.backend.delivery.data.mapper;

import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushType;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class PushRegistrationRowMapper implements RowMapper<PushRegistration> {

    @Override
    public PushRegistration mapRow(ResultSet rs, int rowNum) throws SQLException {
        var pushRegistration = new PushRegistration();
        pushRegistration.setPushToken(rs.getString("push_token"));
        pushRegistration.setPushType(PushType.valueOf(rs.getString("push_type")));
        pushRegistration.setRegisterId(rs.getString("register_id"));
        pushRegistration.setId(rs.getInt("pk_push_registration_id"));
        if(rs.getTimestamp("last_push") != null){
            pushRegistration.setLastPush(rs.getTimestamp("last_push").toInstant());
        }
        return pushRegistration;
    }
}
