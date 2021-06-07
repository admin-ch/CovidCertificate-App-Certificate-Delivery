package ch.admin.bag.covidcertificate.backend.delivery.data.mapper;

import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCert;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class CovidCertRowMapper implements RowMapper<CovidCert> {

    @Override
    public CovidCert mapRow(ResultSet rs, int rowNum) throws SQLException {
        var covidCert = new CovidCert();
        covidCert.setEncryptedHcert(rs.getString("encrypted_hcert"));
        covidCert.setEncryptedPdf(rs.getString("encrypted_pdf"));
        return covidCert;
    }
}
