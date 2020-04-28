/*******************************************************************************
 * $Header$
 * $Revision$
 * $Date$
 *
 *==============================================================================
 *
 * Copyright (c) 2016-2026 Primeton Technologies, Ltd.
 * All rights reserved.
 *
 * Created on 2020��4��28��
 *******************************************************************************/


package com.cheung.shadowsocks.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {MessageConstraintValidator.class})
public @interface SSModelValidator {

    String message() default "error";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
