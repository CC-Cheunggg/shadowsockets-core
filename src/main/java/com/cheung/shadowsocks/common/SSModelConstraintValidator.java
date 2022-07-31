package com.cheung.shadowsocks.common;

import com.cheung.shadowsocks.model.SSModel;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SSModelConstraintValidator implements ConstraintValidator<SSModelValidator, SSModel> {

    @Override
    public boolean isValid(SSModel value, ConstraintValidatorContext context) {
        return (value.getHost() != null) && (value.getPort() != 0);
    }
}
