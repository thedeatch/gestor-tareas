package cl.gestortareas.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator
    implements ConstraintValidator<ValidPassword, String> {

    private static final int MINIMUM_LENGTH = 8;
    private static final int MAXIMUM_LENGTH = 72;

    @Override
    public boolean isValid(
        String password,
        ConstraintValidatorContext context
    ) {
        if (password == null
            || password.length() < MINIMUM_LENGTH
            || password.length() > MAXIMUM_LENGTH) {
            return false;
        }

        boolean hasLetter = password.codePoints().anyMatch(Character::isLetter);
        boolean hasDigit = password.codePoints().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }
}
