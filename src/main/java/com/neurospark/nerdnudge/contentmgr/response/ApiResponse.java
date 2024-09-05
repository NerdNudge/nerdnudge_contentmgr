package com.neurospark.nerdnudge.contentmgr.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    private double timeTaken;
    private int statusCode;
}
