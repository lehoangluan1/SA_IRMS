package SA.irms.common.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends DomainException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "not_found", message);
    }
}
