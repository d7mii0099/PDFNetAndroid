package com.pdftron.collab.service;

import androidx.annotation.Nullable;

import com.pdftron.collab.db.entity.AnnotationEntity;

import java.util.ArrayList;

public class MyService implements CustomService {

    @Override
    public void sendAnnotation(String action, String xfdfCommand, ArrayList<AnnotationEntity> annotations, String documentId, @Nullable String userName) {

    }
}
