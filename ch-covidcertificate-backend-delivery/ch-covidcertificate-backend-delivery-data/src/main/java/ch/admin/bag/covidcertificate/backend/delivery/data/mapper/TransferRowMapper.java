package ch.admin.bag.covidcertificate.backend.delivery.data.mapper;

import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbTransfer;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class TransferRowMapper implements RowMapper<DbTransfer> {

    @Override
    public DbTransfer mapRow(ResultSet rs, int rowNum) throws SQLException {
        var transfer = new DbTransfer();
        transfer.setPk(rs.getInt("pk_transfer_id"));
        transfer.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        transfer.setCode(rs.getString("code"));
        transfer.setPublicKey(rs.getString("public_key"));
        transfer.setAlgorithm(Algorithm.valueOf(rs.getString("algorithm")));
        return transfer;
    }
}
