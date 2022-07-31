package com.cheung.shadowsocks.utils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.HibernateValidator;

import java.util.Set;


public enum ValidateUtils {
    utils;

    private Validator validator = Validation.byProvider(HibernateValidator.class)
            .configure()
            .failFast(true)
            .buildValidatorFactory()
            .getValidator();

    public <T> String validate(T object) {
        Set<ConstraintViolation<T>> validate = validator.validate(object);
        return validate.stream().findFirst().map(ConstraintViolation::getMessage).orElse("");
    }
}
