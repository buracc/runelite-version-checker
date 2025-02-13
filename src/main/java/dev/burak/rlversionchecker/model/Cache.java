package dev.burak.rlversionchecker.model;

public record Cache(
        String version,
        String snapshotVersion,
        String launcherVersion
) {
}
