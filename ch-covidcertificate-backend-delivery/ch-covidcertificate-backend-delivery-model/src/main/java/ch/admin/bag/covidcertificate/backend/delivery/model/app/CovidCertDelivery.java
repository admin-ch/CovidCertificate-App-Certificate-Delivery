package ch.admin.bag.covidcertificate.backend.delivery.model.app;

import ch.ubique.openapi.docannotations.Documentation;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

public class CovidCertDelivery {
    @Documentation(description = "empty if not ready for delivery or already delivered")
    @NotNull
    private List<CovidCert> covidCerts;

    public CovidCertDelivery(List<CovidCert> covidCerts) {
        if (covidCerts == null) {
            covidCerts = new ArrayList<>();
        }
        this.covidCerts = covidCerts;
    }

    public List<CovidCert> getCovidCerts() {
        return covidCerts;
    }

    public void setCovidCerts(List<CovidCert> covidCerts) {
        this.covidCerts = covidCerts;
    }
}
