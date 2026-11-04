package org.apache.maven.artifact.versioning;

/**
 * Universal interface for Maven artifact versioning used by Forge/FML mod containers.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public interface ArtifactVersion extends Comparable<ArtifactVersion> {

    int getMajorVersion();

    int getMinorVersion();

    int getIncrementalVersion();

    int getBuildNumber();

    String getQualifier();
}
