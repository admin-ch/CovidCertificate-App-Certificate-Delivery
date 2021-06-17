package ch.admin.bag.covidcertificate.backend.delivery.data;

import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeAlreadyExistsException;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeNotFoundException;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbCovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbTransfer;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushType;
import java.util.List;

public interface DeliveryDataService {
    public void initTransfer(DeliveryRegistration registration) throws CodeAlreadyExistsException;

    boolean transferCodeExists(String code);

    public List<CovidCert> findCovidCerts(String code) throws CodeNotFoundException;

    public Integer findPkTransferId(String code) throws CodeNotFoundException;

    public DbTransfer findTransfer(String code) throws CodeNotFoundException;

    public void closeTransfer(String code) throws CodeNotFoundException;

    public void insertPushRegistration(PushRegistration registration);

    void removeRegistrations(List<String> tokensToRemove);

    List<PushRegistration> getPushRegistrationByType(PushType pushType, int prevMaxId);

    public void insertCovidCert(DbCovidCert covidCert);
}
