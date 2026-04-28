package com.yoon.pickz.common.response;

import java.util.List;

public record ApiError(
    String code,
    String message,
    List<String> details,
    String traceId
) {}
