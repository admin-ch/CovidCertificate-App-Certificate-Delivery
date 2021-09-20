package ch.admin.bag.covidcertificate.backend.delivery.data;

import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeAlreadyExistsException;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeNotFoundException;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.PublicKeyAlreadyExistsException;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushType;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbCovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbTransfer;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface DeliveryDataService {
    public void initTransfer(DeliveryRegistration registration)
            throws CodeAlreadyExistsException, PublicKeyAlreadyExistsException,
                    NoSuchAlgorithmException;

    boolean transferCodeExists(String code);

    public List<CovidCert> findCovidCerts(String code) throws CodeNotFoundException;

    public Integer findPkTransferId(String code) throws CodeNotFoundException;

    public DbTransfer findTransfer(String code) throws CodeNotFoundException;

    public List<DbTransfer> findTransferWithoutCovidCert(Instant importedBefore);

    public void closeTransfer(String code) throws CodeNotFoundException;

    public void upsertPushRegistration(PushRegistration registration);

    @Transactional(readOnly = false)
    void removeRegistration(String registerId);

    void removeRegistrations(List<String> tokensToRemove);

    List<PushRegistration> getPushRegistrationByType(PushType pushType, int prevMaxId);

    public void insertCovidCert(DbCovidCert covidCert);

    @Transactional(readOnly = false)
    void cleanDB(Duration retentionPeriod);
}
