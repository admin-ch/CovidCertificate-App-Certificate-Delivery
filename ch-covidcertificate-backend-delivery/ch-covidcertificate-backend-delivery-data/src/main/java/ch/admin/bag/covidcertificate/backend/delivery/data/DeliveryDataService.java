package ch.admin.bag.covidcertificate.backend.delivery.data;

import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeAlreadyExistsException;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeNotFoundException;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import java.util.List;

public interface DeliveryDataService {
    public void initTransfer(DeliveryRegistration registration) throws CodeAlreadyExistsException;

    public List<CovidCert> findCovidCerts(String code) throws CodeNotFoundException;

    public void closeTransfer(String code) throws CodeNotFoundException;

    public void insertPushRegistration(PushRegistration registration);

    public void removePushRegistration(PushRegistration registration);

    public List<PushRegistration> findAllPushRegistrations();
}
