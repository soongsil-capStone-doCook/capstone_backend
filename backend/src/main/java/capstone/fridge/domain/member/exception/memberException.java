package capstone.fridge.domain.member.exception;

import capstone.fridge.global.error.code.status.BaseErrorCode;
import capstone.fridge.global.exception.GeneralException;

public class memberException extends GeneralException {

    public memberException(BaseErrorCode code) { super(code); }
}
