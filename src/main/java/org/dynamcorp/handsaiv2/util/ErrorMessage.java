package org.dynamcorp.handsaiv2.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorMessage {
    private String message;
    private String details;
    private String stackTrace;
    private int status;
}
