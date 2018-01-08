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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
        when(extractor2.extract(argThat(entry -> entry != null && entry.getId().equals(entry1.getId())))).thenReturn(Flux.empty());

        EntityExtractorService service = new EntityExtractorService(entryRepository, Arrays.asList(extractor1, extractor2));

        Flux<Object> entities = service.extract();
        AtomicInteger count = new AtomicInteger();
        entities
            .doOnNext(o -> count.incrementAndGet())
            .doOnComplete(() -> {
                Assert.assertEquals(10, count.intValue());
                verify(extractor1).extract(argThat(o -> o != null && o.getId().equals(entry1.getId())));
                verify(extractor1).extract(argThat(o -> o != null && o.getId().equals(entry2.getId())));
                verify(extractor2).extract(argThat(o -> o != null && o.getId().equals(entry1.getId())));
                verify(extractor2).extract(argThat(o -> o != null && o.getId().equals(entry2.getId())));
                verify(entryRepository).findByEntitiesExtractedFalse();
                verify(entryRepository, times(2)).save(argThat(o -> o != null && o.isEntitiesExtracted()));
                verifyNoMoreInteractions(extractor1, extractor2, entryRepository);
            })
            .subscribe(o -> {
                if (o.getClass().equals(Sector.class)) {
                    Assert.assertTrue(((Sector)o).getName().startsWith("sec"));
                } else if (o.getClass().equals(Category.class)) {
                    Assert.assertTrue(((Category)o).getName().startsWith("cat"));
                } else {
                    Assert.fail("Unexpected entity class: " + o.getClass().getName());
                }
            });

    }
}
