package com.jstorni.bo.boscrapper.service;

import com.jstorni.bo.boscrapper.filestorage.FileStorageService;
import com.jstorni.bo.boscrapper.inputmodel.*;
import com.jstorni.bo.boscrapper.model.Category;
import com.jstorni.bo.boscrapper.model.Publication;
import com.jstorni.bo.boscrapper.model.PublicationEntry;
import com.jstorni.bo.boscrapper.model.Sector;
import com.jstorni.bo.boscrapper.repositories.CategoryRepository;
import com.jstorni.bo.boscrapper.repositories.PublicationEntryRepository;
import com.jstorni.bo.boscrapper.repositories.PublicationRepository;
import com.jstorni.bo.boscrapper.repositories.SectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Scan official bulleting site https://www.boletinoficial.gob.ar/ for publications and publication entries
 *
 * @author javier
 */
@Service
public class OfficialBulletinScannerService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FileStorageService fileStorageService;
    private final CategoryRepository categoryRepository;
    private final SectorRepository sectorRepository;
    private final PublicationRepository publicationRepository;
    private final PublicationEntryRepository publicationEntryRepository;
    private final String boEndpoint;
    private final String boEndpointPublicationsByYear;
    private final String boEndpointEntriesByPublication;
    private final String boEndpointEntryDetails;
    private final DateFormat boDateFmt;

    public OfficialBulletinScannerService(
        FileStorageService fileStorageService,
        CategoryRepository categoryRepository,
        SectorRepository sectorRepository,
        PublicationRepository publicationRepository,
        PublicationEntryRepository publicationEntryRepository,
        @Value("${boscrapper.endpoints.bo-base}") String boEndpoint,
        @Value("${boscrapper.endpoints.bo-publications-by-year}") String boEndpointPublicationsByYear,
        @Value("${boscrapper.endpoints.bo-entries-by-publication}") String boEndpointEntriesByPublication,
        @Value("${boscrapper.endpoints.bo-entry-details}") String boEndpointEntryDetails) {
        this.fileStorageService = fileStorageService;
        this.categoryRepository = categoryRepository;
        this.sectorRepository = sectorRepository;
        this.publicationRepository = publicationRepository;
        this.publicationEntryRepository = publicationEntryRepository;
        this.boEndpoint = boEndpoint;
        this.boEndpointPublicationsByYear = boEndpointPublicationsByYear;
        this.boEndpointEntriesByPublication = boEndpointEntriesByPublication;
        this.boEndpointEntryDetails = boEndpointEntryDetails;

        this.boDateFmt = new SimpleDateFormat("yyyyMMdd");
    }

    /**
     * Scan for entries and persist new ones. Publication specific entries (i.e.: <code>Person</code> and <code>Assignment</code>
     * will not be parsed at this stage).
     * @param year the year to scan
     * @param section the section of the official bulletin to scan
     * @return new entries found
     */
    public Flux<PublicationEntry> scan(int year, int section) {
        String sectionName;
        switch (section) {
            case 1:
                sectionName = "primera";
                break;
            default:
                throw new IllegalArgumentException("Section [" + section + "] not supported");
        }

        WebClient boClient = WebClient.create(boEndpoint);

        MultiValueMap<String, String> publicationsByYearParams = new LinkedMultiValueMap<>();
        publicationsByYearParams.add("anio", String.valueOf(year));
        publicationsByYearParams.add("seccion", String.valueOf(section));

        return boClient
            .post()
            .uri(boEndpointPublicationsByYear + "/?x-year=" + year)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .syncBody(publicationsByYearParams)
            .retrieve()
            .bodyToMono(PublicationSearch.class)
            .flatMapMany(publicationSearch -> Flux.fromIterable(publicationSearch.getPublications()))
            .filter(publicationSearchKey -> buildDateFromString(publicationSearchKey.getDate()) != null)
            .filterWhen(publicationSearchKey -> publicationRepository.findOneByAppearsOnEquals(
                buildDateFromString(publicationSearchKey.getDate()))
                .filter(Publication::isImportCompleted)
                .hasElement()
                .map(found -> !found))
            .map(publicationSearchKey ->
                publicationRepository.findOneByAppearsOnEquals(buildDateFromString(publicationSearchKey.getDate()))
                    .switchIfEmpty(publicationRepository.save(new Publication(buildDateFromString(publicationSearchKey.getDate()))))
                    .block()
            )
            .flatMap(publication -> {
                String dateAsString = boDateFmt.format(publication.getAppearsOn());
                MultiValueMap<String, String> publicationEntriesParams = new LinkedMultiValueMap<>();
                publicationEntriesParams.add("nombreSeccion", sectionName);
                publicationEntriesParams.add("subCat", "all");
                publicationEntriesParams.add("offset", "1");
                publicationEntriesParams.add("itemsPerPage", "500");
                publicationEntriesParams.add("fecha", dateAsString);

                return boClient
                    .post()
                    .uri(boEndpointEntriesByPublication + "/?x-date=" + dateAsString)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .syncBody(publicationEntriesParams)
                    .retrieve()
                    .bodyToMono(PublicationsByDate.class);
            })
            .flatMap(publicationsByDate -> Flux.fromArray(publicationsByDate.getPublications()))
            .take(1)
            .flatMap(publicationSummaries -> Flux.fromArray(publicationSummaries.getPublications()))
            .filterWhen(publicationSummary ->
                // skip if publication entry was already processed
                publicationEntryRepository.findOneByIdentifierEquals(Integer.valueOf(
                    publicationSummary.getIdentifier())).hasElement().map(found -> !found)
            )
            .flatMap(summary -> {
                MultiValueMap<String, String> publicationDetailsParams = new LinkedMultiValueMap<>();
                publicationDetailsParams.add("fechaPublication", boDateFmt.format(null));
                publicationDetailsParams.add("numeroTramite", summary.getIdentifier());
                publicationDetailsParams.add("origenDetalle", "0");
                publicationDetailsParams.add("idSesion", "");

                return boClient
                    .post()
                    .uri(boEndpointEntryDetails + "/?x-identifier=" + summary.getIdentifier())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .syncBody(publicationDetailsParams)
                    .retrieve()
                    .bodyToMono(PublicationDetailsByPublication.class)
                    .flatMapMany(details -> Flux.fromIterable(details.getDetails()))
                    .doOnNext(details -> details.setSummary(summary));
            })
            .doOnNext(publicationDetails -> fileStorageService.write(publicationDetails.getIdentifier(),
                new ByteArrayInputStream(publicationDetails.getContent().getBytes())))
            .flatMap(publicationDetails -> {
                PublicationSummary summary = publicationDetails.getSummary();
                final PublicationEntry entry = new PublicationEntry();
                entry.setHasAttachment(publicationDetails.getPdfIdentifier() != null);
                entry.setPdfIdentifier(publicationDetails.getPdfIdentifier());
                entry.setIdentifier(Integer.valueOf(publicationDetails.getIdentifier()));
                entry.setSummary(summary.getPublicationSummary());
                entry.setTitle(publicationDetails.getTitle());

                return categoryRepository.findOneByNameEquals(summary.getCategoryName())
                    .switchIfEmpty(categoryRepository.save(new Category(summary.getCategoryName())))
                    .doOnNext(category -> entry.setCategory(category))
                    .then(sectorRepository.findOneByNameEquals(summary.getSectorName()))
                    .switchIfEmpty(sectorRepository.save(new Sector(summary.getSectorName())))
                    .doOnNext(sector -> entry.setSector(sector))
                    .then(publicationEntryRepository.save(entry));
            })
            .doOnNext(publicationEntry -> logger.info("Processed entry {}", publicationEntry.getIdentifier()))
            .doOnError(throwable -> logger.error("Error while scanning entries", throwable));
    }

    private Date buildDateFromString(String source) {
        Date date = null;
        try {
            date = boDateFmt.parse(source);
        } catch (ParseException ex) {
            logger.error("Invalid date value: {}", source);
        }

        return date;
    }
}
