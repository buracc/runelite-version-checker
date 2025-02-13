package dev.burak.rlversionchecker.model;

import java.util.Map;

public record WorkflowDispatch(
        String ref,
        Map<String, Object> inputs
) {
}
