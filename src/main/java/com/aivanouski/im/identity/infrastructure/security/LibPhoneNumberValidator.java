package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.PhoneNumberValidator;
import com.aivanouski.im.shared.exception.ValidationException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.springframework.stereotype.Component;

@Component
public class LibPhoneNumberValidator implements PhoneNumberValidator {
    private static final PhoneNumberUtil util = PhoneNumberUtil.getInstance();

    @Override
    public String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new ValidationException("phone is required.");
        }
        try {
            Phonenumber.PhoneNumber parsed = util.parse(phone, "ZZ");
            if (!util.isValidNumber(parsed)) {
                throw new ValidationException("Invalid phone number.");
            }
            return util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Invalid phone number.");
        }
    }
}
