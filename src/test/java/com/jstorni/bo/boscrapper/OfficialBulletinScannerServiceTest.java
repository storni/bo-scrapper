package com.jstorni.bo.boscrapper;

import com.jstorni.bo.boscrapper.filestorage.FileStorageService;
import com.jstorni.bo.boscrapper.model.Publication;
import com.jstorni.bo.boscrapper.model.PublicationEntry;
import com.jstorni.bo.boscrapper.model.Sector;
import com.jstorni.bo.boscrapper.repositories.CategoryRepository;
import com.jstorni.bo.boscrapper.model.Category;
import com.jstorni.bo.boscrapper.repositories.PublicationEntryRepository;
import com.jstorni.bo.boscrapper.repositories.PublicationRepository;
import com.jstorni.bo.boscrapper.repositories.SectorRepository;
import com.jstorni.bo.boscrapper.service.OfficialBulletinScannerService;
import io.fabric8.mockwebserver.DefaultMockServer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OfficialBulletinScannerServiceTest {

    private DefaultMockServer mockWebServer;
    private String boEndpointBase;
    private String boEndpointPublicationsByYear;
    private String boEndpointEntriesByPublication;
    private String boEndpointEntryDetails;

    @Before
    public void startup() throws Exception {
        mockWebServer = new DefaultMockServer();
        mockWebServer.start();
        boEndpointBase = mockWebServer.url("/");
        boEndpointPublicationsByYear = "/secciones/fechasEdicionesAnio";
        boEndpointEntriesByPublication = "/secciones/secciones.json";
        boEndpointEntryDetails = "/norma/detallePrimera";
    }

    @After
    public void shutdown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testScan() throws Exception {

        List<String> identifiers = Arrays.asList("176621", "176622");

        FileStorageService storageService = mock(FileStorageService.class);
        identifiers.forEach(identifier ->
            when(storageService.write(argThat(o -> o != null && o.equals(identifier)), argThat(o -> true)))
                .thenReturn("/file-location/" + identifier));

        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        String categoryName = "Decisiones Administrativas";
        when(categoryRepository.findOneByNameEquals(categoryName))
            .thenReturn(Mono.empty())
            .thenReturn(Mono.just(new Category(categoryName)));
        when(categoryRepository.save(argThat(o -> o.getName().equals(categoryName))))
            .thenReturn(Mono.just(new Category(categoryName)));

        SectorRepository sectorRepository = mock(SectorRepository.class);
        String sectorJustDdhh = "MINISTERIO DE JUSTICIA Y DERECHOS HUMANOS";
        when(sectorRepository.findOneByNameEquals(sectorJustDdhh))
            .thenReturn(Mono.empty());
        when(sectorRepository.save(argThat(o -> o != null && o.getName().equals(sectorJustDdhh))))
            .thenReturn(Mono.just(new Sector(sectorJustDdhh)));

        String sectorHac = "MINISTERIO DE HACIENDA";
        when(sectorRepository.findOneByNameEquals(sectorHac))
            .thenReturn(Mono.empty());
        when(sectorRepository.save(argThat(o -> o != null && o.getName().equals(sectorHac))))
            .thenReturn(Mono.just(new Sector(sectorHac)));

        Date publicationDate = new SimpleDateFormat("yyyyMMdd").parse("20171226");
        PublicationRepository publicationRepository = mock(PublicationRepository.class);
        when(publicationRepository.findOneByAppearsOnEquals(publicationDate))
            .thenReturn(Mono.empty());
        when(publicationRepository.save(argThat(o -> o != null && o.getAppearsOn().equals(publicationDate))))
            .thenReturn(Mono.just(new Publication(publicationDate)));

        PublicationEntryRepository publicationEntryRepository = mock(PublicationEntryRepository.class);
        identifiers.forEach(identifier -> when(publicationEntryRepository.findOneByIdentifierEquals(Integer.valueOf(identifier)))
            .thenReturn(Mono.empty()));
        identifiers.stream().map(Integer::parseInt)
            .forEach(identifier -> when(publicationEntryRepository.save(argThat(o -> o != null && o.getIdentifier() == identifier.intValue())))
            .thenReturn(Mono.just(buildPublicationEntry(identifier))));
        identifiers.forEach(identifier -> addDetailsRequestExpectation(identifier));
        addPublicationsByYearExpectation(2017);
        addEntriesByPublicationExpectation("20171226");

        OfficialBulletinScannerService scannerService = new OfficialBulletinScannerService(storageService,
            categoryRepository,
            sectorRepository,
            publicationRepository,
            publicationEntryRepository,
            boEndpointBase,
            boEndpointPublicationsByYear,
            boEndpointEntriesByPublication,
            boEndpointEntryDetails);

        Flux<PublicationEntry> entries = scannerService.scan(2017, 1);
        Assert.notNull(entries.blockFirst());
    }

    private PublicationEntry buildPublicationEntry(Integer identifier) {
        PublicationEntry entry = new PublicationEntry();
        entry.setIdentifier(identifier);

        return entry;
    }

    private void addDetailsRequestExpectation(String identifier) {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        try {
            IOUtils.copy(getClass().getResourceAsStream("/publication-details-" + identifier + ".json"), content);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        mockWebServer.expect()
            .post()
            .withPath(boEndpointEntryDetails + "/?x-identifier=" + identifier)
            .andReturn(200, new String(content.toByteArray()))
            .withHeader("Content-Type", "application/json")
            .times(1);
    }

    private void addEntriesByPublicationExpectation(String dateAsString) {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        try {
            IOUtils.copy(getClass().getResourceAsStream("/publications-by-date-" + dateAsString + ".json"), content);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        mockWebServer.expect()
            .post()
            .withPath(boEndpointEntriesByPublication + "/?x-date=" + dateAsString)
            .andReturn(200, new String(content.toByteArray()))
            .withHeader("Content-Type", "application/json")
            .times(1);
    }

    private void addPublicationsByYearExpectation(int year)  {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        try {
            IOUtils.copy(getClass().getResourceAsStream("/publications-by-year-" + year + ".json"), content);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        mockWebServer.expect()
            .post()
            .withPath(boEndpointPublicationsByYear + "/?x-year=" + year)
            .andReturn(200, new String(content.toByteArray()))
            .withHeader("Content-Type", "application/json")
            .times(1);
    }

}
