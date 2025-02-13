package dev.burak.rlversionchecker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("project")
@JsonIgnoreProperties(ignoreUnknown = true)
public record Project(String version) {
}
