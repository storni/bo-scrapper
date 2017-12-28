package com.jstorni.bo.boscrapper.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstorni.bo.boscrapper.inputmodel.PublicationsByDate;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RunWith(JUnit4.class)
public class PublicationsByDateDeserializerTest {

    @Test
    public void test() throws Exception {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        IOUtils.copy(getClass().getResourceAsStream("/publications-by-date-20171226.json"), content);

        PublicationsByDate instance = new ObjectMapper().readValue(content.toByteArray(), PublicationsByDate.class);
        System.out.println(instance);
    }
}
