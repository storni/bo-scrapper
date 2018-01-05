package com.jstorni.bo.boscrapper;

import com.jstorni.bo.boscrapper.entityextraction.EntityExtractor;
import com.jstorni.bo.boscrapper.model.BaseEntity;
import com.jstorni.bo.boscrapper.model.Category;
import com.jstorni.bo.boscrapper.model.PublicationEntry;
import com.jstorni.bo.boscrapper.model.Sector;
import com.jstorni.bo.boscrapper.repositories.PublicationEntryRepository;
import com.jstorni.bo.boscrapper.service.EntityExtractorService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EntityExtractorServiceTest {

    @Test
    public void test() throws InterruptedException {

        PublicationEntry entry1 = new PublicationEntry();
        entry1.setId("1");

        PublicationEntry entry2 = new PublicationEntry();
        entry2.setId("2");

        PublicationEntryRepository entryRepository = mock(PublicationEntryRepository.class);
        when(entryRepository.findByEntitiesExtractedFalse()).thenReturn(Flux.fromIterable(Arrays.asList(entry1, entry2)));

        List<Object> categories = new ArrayList<>();
        List<Object> sectors = new ArrayList<>();
        for (int n=0; n < 5; n++) {
            categories.add(new Category("cat" + n));
            sectors.add(new Sector("sec" + n));
        }

        EntityExtractor extractor1 = mock(EntityExtractor.class);
        when(extractor1.extract(argThat(entry -> entry != null && entry.getId().equals(entry1.getId())))).thenReturn(Flux.fromIterable(categories));
        when(extractor1.extract(argThat(entry -> entry != null && entry.getId().equals(entry2.getId())))).thenReturn(Flux.empty());

        EntityExtractor extractor2 = mock(EntityExtractor.class);
        when(extractor2.extract(argThat(entry -> entry != null && entry.getId().equals(entry2.getId())))).thenReturn(Flux.fromIterable(sectors));
        when(extractor1.extract(argThat(entry -> entry != null && entry.getId().equals(entry1.getId())))).thenReturn(Flux.empty());

        EntityExtractorService service = new EntityExtractorService(entryRepository, Arrays.asList(extractor1, extractor2));

        Flux<Object> entities = service.extract();
        entities.blockLast();

        entities.count().doOnNext(count -> Assert.assertEquals(10, count.longValue()));

    }
}
