package capstone.fridge.domain.scrap.exception;

import capstone.fridge.global.error.code.status.BaseErrorCode;
import capstone.fridge.global.exception.GeneralException;

public class scrapException extends GeneralException {

    public scrapException(BaseErrorCode code) { super(code); }
}
