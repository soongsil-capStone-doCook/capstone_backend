package capstone.fridge.domain.fridge.exception;

import capstone.fridge.global.error.code.status.BaseErrorCode;
import capstone.fridge.global.exception.GeneralException;

public class fridgeException extends GeneralException {

    public fridgeException(BaseErrorCode code) { super(code); }
}
