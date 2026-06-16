package com.adsplatform.safety;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowContentInitializer implements ApplicationRunner {

    private final VectorSafetyService vectorSafetyService;

    @Override
    public void run(ApplicationArguments args){
        log.info("Seeding show content into Redis vector store...");
        vectorSafetyService.indexShowBatch(List.of(
                new VectorSafetyService.ShowContent(
                        "show-001", "Stranger Things",
                        "A group of kids uncover supernatural mysteries in a small town. " +
                                "Themes of friendship, courage, and the unknown.",
                        "Sci-fi Drama", "TV-14"
                ),
                new VectorSafetyService.ShowContent(
                        "show-002", "Wednesday",
                        "Wednesday Addams navigates life at Nevermore Academy. " +
                                "Dark humor, mystery, and teenage identity themes.",
                        "Comedy Horror", "TV-14"
                ),
                new VectorSafetyService.ShowContent(
                        "show-003", "The Crown",
                        "The political rivalries and personal intrigues of the British Royal Family. " +
                                "Themes of duty, power, and legacy.",
                        "Historical Drama", "TV-MA"
                ),new VectorSafetyService.ShowContent(
                        "show-004", "Bridgerton",
                        "Romance and scandal in Regency-era London high society. " +
                                "Themes of love, ambition, and social class.",
                        "Romance Drama", "TV-MA"
                ),
                new VectorSafetyService.ShowContent(
                        "show-005", "Squid Game",
                        "Desperate people compete in deadly children's games for a massive cash prize. " +
                                "Themes of inequality, desperation, and violence.",
                        "Thriller", "TV-MA"
                )
        ));
        log.info("Show content seeding complete.");
    }
}
