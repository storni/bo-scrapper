package com.jstorni.bo.boscrapper.service;

import com.jstorni.bo.boscrapper.filestorage.FileStorageService;
import com.jstorni.bo.boscrapper.inputmodel.PublicationDetailsByPublication;
import com.jstorni.bo.boscrapper.inputmodel.PublicationSearch;
import com.jstorni.bo.boscrapper.inputmodel.PublicationSummary;
import com.jstorni.bo.boscrapper.inputmodel.PublicationsByDate;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final String publicationEntryContentFileTemplate;
    private final boolean enableBoEndpointFileCache; // TODO find a way to capture actual responses from bo endpoint
    private final boolean useQueryParams;
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
            @Value("${boscrapper.endpoints.bo-entry-details}") String boEndpointEntryDetails,
            @Value("${boscrapper.files.publication-entry-content-file-template}") String publicationEntryContentFileTemplate,
            @Value("${boscrapper.endpoints.enable-file-cache}") boolean enableBoEndpointFileCache,
            @Value("${boscrapper.endpoints.use-query-params:#{false}}") boolean useQueryParams) {
        this.fileStorageService = fileStorageService;
        this.categoryRepository = categoryRepository;
        this.sectorRepository = sectorRepository;
        this.publicationRepository = publicationRepository;
        this.publicationEntryRepository = publicationEntryRepository;
        this.boEndpoint = boEndpoint;
        this.boEndpointPublicationsByYear = boEndpointPublicationsByYear;
        this.boEndpointEntriesByPublication = boEndpointEntriesByPublication;
        this.boEndpointEntryDetails = boEndpointEntryDetails;
        this.publicationEntryContentFileTemplate = publicationEntryContentFileTemplate;
        this.enableBoEndpointFileCache = enableBoEndpointFileCache;
        this.useQueryParams = useQueryParams;
        this.boDateFmt = new SimpleDateFormat("yyyyMMdd");
    }

    /**
     * Scan for entries and persist new ones. Publication specific entries (i.e.: <code>Person</code> and <code>Assignment</code>
     * will not be parsed at this stage).
     *
     * @param year    the year to scan
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
                .uri(boEndpointPublicationsByYear + (useQueryParams ? "/?x-year=" + year : ""))
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
                .doOnNext(publication -> logger.debug("Going to process publication of {}", publication.getAppearsOn()))
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
                            .uri(boEndpointEntriesByPublication + (useQueryParams ? "/?x-date=" + dateAsString : ""))
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .syncBody(publicationEntriesParams)
                            .retrieve()
                            .bodyToMono(PublicationsByDate.class)
                            .doOnError(ex -> logger.error("Error while loading publications for date {}", dateAsString));
                })
                .flatMap(publicationsByDate -> Flux.fromIterable(
                        publicationsByDate.getPublications().stream()
                                .filter(Objects::nonNull)
                                .filter(o -> List.class.isAssignableFrom(o.getClass()))
                                .collect(Collectors.toList()))
                )
                .flatMap(o -> Flux.fromIterable((List<PublicationSummary>) deserializeFromMap((List) o)))
                .doOnNext(summary -> logger.debug("Found publication in date {} - id {} - category {} - sector {} ",
                        summary.getPublicationDate(), summary.getIdentifier(), summary.getCategoryName(), summary.getSectorName()))
                .filterWhen(publicationSummary ->
                        // skip if publication entry was already processed
                        publicationEntryRepository.findOneByIdentifierEquals(Integer.valueOf(
                                publicationSummary.getIdentifier())).hasElement().map(found -> !found)
                )
                .flatMap(summary -> {
                    MultiValueMap<String, String> publicationDetailsParams = new LinkedMultiValueMap<>();
                    publicationDetailsParams.add("fechaPublication", summary.getPublicationDate());
                    publicationDetailsParams.add("numeroTramite", summary.getIdentifier());
                    publicationDetailsParams.add("origenDetalle", "0");
                    publicationDetailsParams.add("idSesion", "");

                    return boClient
                            .post()
                            .uri(boEndpointEntryDetails + (useQueryParams ? "/?x-identifier=" + summary.getIdentifier() : ""))
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .syncBody(publicationDetailsParams)
                            .retrieve()
                            .bodyToMono(PublicationDetailsByPublication.class)
                            .doOnNext(details -> details.getDetails().setSummary(summary))
                            .doOnError(ex -> logger.error("Error while loading publication details for id {}", summary.getIdentifier()));
                })
                .map(PublicationDetailsByPublication::getDetails)
                .doOnNext(publicationDetails -> fileStorageService.write(
                        MessageFormat.format(publicationEntryContentFileTemplate, publicationDetails.getIdentifier()),
                        new ByteArrayInputStream(publicationDetails.getContent().getBytes())))
                .doOnNext(publicationDetails -> logger.debug("Found publication details id {}", publicationDetails.getIdentifier()))
                .flatMap(publicationDetails -> {
                    PublicationSummary summary = publicationDetails.getSummary();
                    final PublicationEntry entry = new PublicationEntry();
                    entry.setHasAttachment(publicationDetails.getPdfIdentifier() != null);
                    entry.setPdfIdentifier(publicationDetails.getPdfIdentifier());
                    entry.setIdentifier(Integer.valueOf(publicationDetails.getIdentifier()));
                    entry.setSummary(summary.getPublicationSummary());
                    entry.setTitle(publicationDetails.getTitle());

                    String categoryName = clean(summary.getCategoryName());
                    String sectorName = clean(summary.getSectorName());

                    return findOrCreateCategory(categoryName)
                            .doOnNext(category -> entry.setCategory(category))
                            .then(findOrCreateSector(sectorName))
                            .doOnNext(sector -> entry.setSector(sector))
                            .then(publicationRepository.findOneByAppearsOnEquals(buildDateFromString(summary.getPublicationDate())))
                            .doOnSuccess(publication -> {
                                if (publication == null) {
                                    logger.warn("Skipping publication entry in date {} - id {} - category {} - sector {} ."
                                                    + "Reason: publication not found for date", summary.getPublicationDate(),
                                            entry.getIdentifier(), entry.getCategory().getName(), entry.getSector().getName());
                                } else {
                                    logger.debug("Going to attach publication entry {} to date {}", entry.getIdentifier(), publication.getAppearsOn());
                                }
                            })
                            .filter(Objects::nonNull)
                            .flatMap(publication -> {
                                entry.setPublication(publication);
                                return publicationEntryRepository.save(entry);
                            });

                })
                .filter(Objects::nonNull)
                .doOnNext(entry -> logger.info("Processed publication entry in date {} - id {} - category {} - sector {} ",
                        entry.getPublication().getAppearsOn(), entry.getIdentifier(), entry.getCategory().getName(), entry.getSector().getName()))
                .doOnError(throwable -> logger.error("Error while scanning entries", throwable));
    }

    private Mono<Category> findOrCreateCategory(String categoryName) {
        return categoryRepository.findOneByNameEquals(categoryName)
                .switchIfEmpty(categoryRepository.save(new Category(categoryName)))
                .onErrorResume(ex -> {
                    if (ex.getClass().equals(DuplicateKeyException.class)) {
                        return  categoryRepository.findOneByNameEquals(categoryName);
                    } else {
                        logger.error("Error while creating category {}", categoryName, ex);
                        return Mono.empty();
                    }
                });
    }

    private Mono<Sector> findOrCreateSector(String sectorName) {
        return sectorRepository.findOneByNameEquals(sectorName)
                .switchIfEmpty(sectorRepository.save(new Sector(sectorName)))
                .onErrorResume(ex -> {
                    if (ex.getClass().equals(DuplicateKeyException.class)) {
                        return  sectorRepository.findOneByNameEquals(sectorName);
                    } else {
                        logger.error("Error while creating sector {}", sectorName, ex);
                        return Mono.empty();
                    }
                });
    }


    private String clean(String source) {
        String string = StringUtils.trimWhitespace(source);
        string = StringUtils.capitalize(source);
        return StringUtils.deleteAny(source, "\r\n\t");
    }

    private Date buildDateFromString(String source) {
        Date date = null;
        try {
            date = boDateFmt.parse(source);
        } catch (Exception ex) {
            logger.error("Invalid date value: {}", source);
        }

        return date;
    }

    /**
     * Cannot deserialize using object mapping as returned data is a polyphormic list.<br>
     * See publications-by-date-20171226.json file for a sample response.
     *
     * @param data the raw data, a List of Map instance with key, values of the publication summary
     * @return a <code>PublicationSummary</code> instance or null if the input data is not a Map
     */
    private List<PublicationSummary> deserializeFromMap(List<Object> data) {
        return data.stream()
                .filter(o -> Map.class.isAssignableFrom(o.getClass()))
                .map(o -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> map = (Map) o;

                    PublicationSummary summary = new PublicationSummary();
                    summary.setCategoryName(map.get("rubro"));
                    summary.setSectorName(map.get("organismo"));
                    summary.setIdentifier(map.get("idTamite")); // yes.... "tamite"
                    summary.setPublicationDate(map.get("fechaPublicacion"));
                    summary.setPublicationSummary(map.get("sintesis"));

                    return summary;
                })
                .collect(Collectors.toList());
    }
}
