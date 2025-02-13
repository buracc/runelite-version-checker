package dev.burak.rlversionchecker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Bootstrap(String version) {
}
