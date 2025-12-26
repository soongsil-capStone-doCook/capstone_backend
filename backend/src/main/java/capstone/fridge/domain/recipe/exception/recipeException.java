package capstone.fridge.domain.recipe.exception;

import capstone.fridge.global.error.code.status.BaseErrorCode;
import capstone.fridge.global.exception.GeneralException;

public class recipeException extends GeneralException {

    public recipeException(BaseErrorCode code) { super(code); }
}
