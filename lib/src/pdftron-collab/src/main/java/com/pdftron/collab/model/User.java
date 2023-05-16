package com.pdftron.collab.model;

/**
 * Interface for a user
 */
public interface User {
    String getId();
    String getName();
    Long getDate();
    boolean isCurrentUser();
    String getActiveAnnotation();
}
