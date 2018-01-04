package com.jstorni.bo.boscrapper.controllers;

import com.jstorni.bo.boscrapper.model.PublicationEntry;
import com.jstorni.bo.boscrapper.service.OfficialBulletinScannerService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Flux;

@Controller
public class AdminController {

    private final OfficialBulletinScannerService scannerService;

    public AdminController(OfficialBulletinScannerService scannerService) {
        this.scannerService = scannerService;
    }

    @PostMapping("/scan/{year}/{section}")
    public Flux<PublicationEntry> scanEntries(@PathVariable("year") int year, @PathVariable("section") int section) {
        return scannerService.scan(year, section);
    }

}
