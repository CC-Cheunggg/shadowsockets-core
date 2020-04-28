package com.cheung.shadowsocks.common;

import com.cheung.shadowsocks.model.SSModel;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

class MessageConstraintValidator implements ConstraintValidator<SSModelValidator, SSModel> {

    @Override
    public boolean isValid(SSModel value, ConstraintValidatorContext context) {
        return (value.getHost() != null) && (value.getPort() != 0);
    }
}
