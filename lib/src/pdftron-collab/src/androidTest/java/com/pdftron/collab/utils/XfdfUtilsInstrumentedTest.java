package com.pdftron.collab.utils;

import android.content.Context;
import android.graphics.Color;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.pdftron.collab.R;
import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFNet;
import com.pdftron.pdf.model.AnnotReviewState;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class XfdfUtilsInstrumentedTest {

    private static String sJSONStr = "{\"dId\":\"UmLSYDcq-BbB2JJCln0XyXumZejHA6VqaYw2DUsmZLk=\",\"aId\":\"2f0b8e56-c624-4e3a-a3a6-f072311694ad\",\"author\":\"832D8592F7141CB7A7674261C181A3DC\",\"aName\":\"Cobra\",\"xfdf\":\"<add><square style=\\\"solid\\\" width=\\\"5\\\" color=\\\"#E44234\\\" opacity=\\\"1\\\" creationdate=\\\"D:20190326220941Z\\\" flags=\\\"print\\\" date=\\\"D:20190326220941Z\\\" name=\\\"2f0b8e56-c624-4e3a-a3a6-f072311694ad\\\" page=\\\"0\\\" rect=\\\"68.1528,33.8239,173.984,120.879\\\" title=\\\"832D8592F7141CB7A7674261C181A3DC\\\" \\/><\\/add>\",\"at\":\"create\"}";
    private static String sRealDocId = "REAL_DOC_ID";

    private static String sXfdfStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xfdf xmlns=\"http://ns.adobe.com/xfdf/\" xml:space=\"preserve\">\n" +
            "\t<add>\n" +
            "\t\t<highlight style=\"solid\" width=\"1\" color=\"#FFFF00\" opacity=\"1\" creationdate=\"D:20190730220921Z\" flags=\"print\" date=\"D:20190730220921Z\" name=\"-Ll3vZHuHShOSSWRSP_8\" page=\"1\" coords=\"243.585,435.735,297.051,435.735,243.585,425.268,297.051,425.268\" rect=\"241.95,425.268,298.686,435.735\" state=\"Completed\" statemodel=\"Review\" title=\"0997A7EDCD312D70A89C5A737576CEBA\">\n" +
            "\t\t\t<popup date=\"D:20190730220921Z\" page=\"1\" rect=\"241.95,425.268,298.686,435.735\" />\n" +
            "\t\t\t<contents>memorandum</contents>\n" +
            "\t\t\t<apref blend-mode=\"multiply\" y=\"435.735\" x=\"241.95\" gennum=\"1\" objnum=\"655\" />\n" +
            "\t\t</highlight>\n" +
            "\t</add>\n" +
            "\t<modify />\n" +
            "\t<delete />\n" +
            "\t<pdf-info version=\"2\" xmlns=\"http://www.pdftron.com/pdfinfo\" />\n" +
            "</xfdf>";

    @Before
    public void initPDFNet() throws PDFNetException {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        PDFNet.initialize(appContext, R.raw.pdfnet, "test");
    }

    @Test
    public void parseAnnotationEntity() throws JSONException {
        JSONObject jsonObject = new JSONObject(sJSONStr);
        AnnotationEntity annotationEntity = JsonUtils.parseRetrieveMessage(jsonObject, sRealDocId);
        Assert.assertNotNull(annotationEntity);
        Assert.assertEquals(sRealDocId, annotationEntity.getDocumentId());
    }

    @Test
    public void xfdfToAnnotationEntity() throws JSONException {
        JSONObject jsonObject = new JSONObject(sJSONStr);
        String docId = "UmLSYDcq-BbB2JJCln0XyXumZejHA6VqaYw2DUsmZLk=";
        AnnotationEntity annotationEntity = XfdfUtils.xfdfToAnnotationEntity(docId, jsonObject);
        Assert.assertNotNull(annotationEntity);
        Assert.assertEquals(docId, annotationEntity.getDocumentId());
        Assert.assertEquals("2f0b8e56-c624-4e3a-a3a6-f072311694ad", annotationEntity.getId());
        Assert.assertEquals("832D8592F7141CB7A7674261C181A3DC", annotationEntity.getAuthorId());
        Assert.assertEquals("Cobra", annotationEntity.getAuthorName());
        Assert.assertEquals(Color.parseColor("#E44234"), annotationEntity.getColor());
        Date expectedDate = XfdfUtils.deserializeDate("D:20190326220941Z");
        Assert.assertNotNull(expectedDate);
        Assert.assertTrue(Math.abs(expectedDate.getTime() - annotationEntity.getCreationDate().getTime()) <= 2f);
        Assert.assertEquals(1, annotationEntity.getPage());
    }

    @Test
    public void parseXfdf() {
        HashMap<String, AnnotationEntity> map = XfdfUtils.parseXfdf(sRealDocId, sXfdfStr);
        Assert.assertNotNull(map);
        String annotId = "-Ll3vZHuHShOSSWRSP_8";
        AnnotationEntity entity = map.get(annotId);
        Assert.assertNotNull(entity);
        Assert.assertEquals(annotId, entity.getId());
        Assert.assertEquals("0997A7EDCD312D70A89C5A737576CEBA", entity.getAuthorId());
        Assert.assertEquals(sRealDocId, entity.getDocumentId());
        Assert.assertEquals(Annot.e_Highlight, entity.getType());
        String color = String.valueOf(entity.getColor());
        Assert.assertNotNull(color);
        String htmlCode = Integer.toHexString(Integer.parseInt(color));
        Assert.assertEquals("FFFFFF00".toLowerCase(), htmlCode.toLowerCase());
        Assert.assertEquals("memorandum", entity.getContents());
        Assert.assertEquals(2, entity.getPage());
        Assert.assertEquals(435.735, entity.getYPos(), 0.1);
        Assert.assertEquals(1.0, entity.getOpacity(), 0.1);
        Assert.assertEquals(AnnotReviewState.COMPLETED.getValue(), entity.getReviewState());
    }
}
