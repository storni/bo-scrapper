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
import reactor.core.publisher.Hooks;
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
        //Hooks.onOperatorDebug();

        List<String> identifiers = Arrays.asList("176621", "176622");

        FileStorageService storageService = mock(FileStorageService.class);
        identifiers.forEach(identifier ->
            when(storageService.write(argThat(o -> o != null && o.equals(identifier)), argThat(o -> true)))
                .thenReturn("/file-location/" + identifier));

        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        String categoryName = "Decisiones Administrativas";
        Category category = new Category(categoryName);
        when(categoryRepository.findOneByNameEquals(argThat(o -> o != null && categoryName.equals(o))))
            .thenReturn(Mono.empty())
            .thenReturn(Mono.just(category));
        when(categoryRepository.save(argThat(o -> o.getName().equals(categoryName))))
            .thenReturn(Mono.just(category));

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
        Publication publication = new Publication(publicationDate);
        PublicationRepository publicationRepository = mock(PublicationRepository.class);
        when(publicationRepository.findOneByAppearsOnEquals(publicationDate))
            .thenReturn(Mono.empty())
            .thenReturn(Mono.just(publication))
            .thenReturn(Mono.just(publication));
        when(publicationRepository.save(argThat(o -> o != null && o.getAppearsOn().equals(publicationDate))))
            .thenReturn(Mono.just(publication));

        PublicationEntryRepository publicationEntryRepository = mock(PublicationEntryRepository.class);
        identifiers.forEach(identifier -> when(publicationEntryRepository.findOneByIdentifierEquals(Integer.valueOf(identifier)))
            .thenReturn(Mono.empty()));
        identifiers.stream().map(Integer::parseInt)
            .forEach(identifier -> when(publicationEntryRepository.save(argThat(o -> o != null
                    && o.getIdentifier() == identifier.intValue()
                    && o.getPublication().getAppearsOn().equals(publicationDate))))
            .thenReturn(Mono.just(buildPublicationEntry(identifier, publication, category,
                    identifier == 176621 ? new Sector(sectorJustDdhh) :  new Sector(sectorHac)))));
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
            boEndpointEntryDetails,
            null,
            false,
            true);

        Flux<PublicationEntry> entries = scannerService.scan(2017, 1);
        entries.blockLast(); // wait for the last entry

        identifiers.forEach(identifier ->
            verify(storageService).write(argThat(o -> o != null && o.equals(identifier)), argThat(o -> true)));

        // TODO fix me, it should be 1 time
        verify(categoryRepository, times(2)).save(argThat(o -> o != null && o.getName().equals(categoryName)));
        verify(categoryRepository, times(2)).findOneByNameEquals(argThat(o -> o != null && o.equals(categoryName)));

        verify(sectorRepository).findOneByNameEquals(argThat(o -> o != null && o.equals(sectorJustDdhh)));
        verify(sectorRepository).save(argThat(o -> o != null && o.getName().equals(sectorJustDdhh)));

        verify(sectorRepository).findOneByNameEquals(argThat(o -> o != null && o.equals(sectorHac)));
        verify(sectorRepository).save(argThat(o -> o != null && o.getName().equals(sectorHac)));

        verify(publicationRepository, times(4)).findOneByAppearsOnEquals(publicationDate);
        verify(publicationRepository).save(argThat(o -> o != null && o.getAppearsOn().equals(publicationDate)));

        identifiers.forEach(identifier -> verify(publicationEntryRepository).findOneByIdentifierEquals(Integer.valueOf(identifier)));
        identifiers.stream().map(Integer::parseInt)
            .forEach(identifier -> verify(publicationEntryRepository).save(argThat(o -> o != null
                    && o.getIdentifier() == identifier
                    && o.getSector() != null
                    && o.getSector().getName() != null
                    && o.getCategory() != null
                    && o.getCategory().getName().equals(categoryName)
                    && o.getPublication() != null
                    && o.getPublication().getAppearsOn().equals(publicationDate)
            )));

        identifiers.stream().forEach(identifier -> verify(storageService).write(argThat(o -> o != null && o.equals(identifier)), argThat(o -> o != null)));

        verifyNoMoreInteractions(storageService, categoryRepository, sectorRepository, publicationRepository, publicationEntryRepository);
    }

    private PublicationEntry buildPublicationEntry(Integer identifier, Publication publication, Category category, Sector sector) {
        PublicationEntry entry = new PublicationEntry();
        entry.setIdentifier(identifier);
        entry.setPublication(publication);
        entry.setSector(sector);
        entry.setCategory(category);

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
