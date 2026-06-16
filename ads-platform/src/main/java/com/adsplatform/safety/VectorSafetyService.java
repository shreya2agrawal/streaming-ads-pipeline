package com.adsplatform.safety;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorSafetyService {
    private final VectorStore vectorStore;
    @Value("${ads.safety.vector.safe-threshold:0.75}")
    private double safeThreshold;
    @Value("${ads.safety.vector.review-threshold:0.50}")
    private double reviewThreshold;

    public void indexShowContent(String showId, String title,
                                 String synopsis, String genre, String contentRating){
        String content = String.format(
                "Title: %s. Genre: %s. Rating: %s. Synopsis: %s",
                title, genre, contentRating, synopsis
        );
        Document doc = new Document(
                content,
                Map.of(
                        "showId", showId,
                        "title", title,
                        "genre", genre,
                        "contentRating", contentRating,
                        "type", "show"
                )
        );
        vectorStore.add(List.of(doc));
        log.info("Indexed show content for: {}", title);
    }

    public VectorSafetyResult scoreAdCreative(String creativeText,
                                              String advertiser,
                                              String targetShowId){
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(creativeText)
                        .topK(5)
                        .similarityThreshold(0.0)
                        .build()
        );
        if(results.isEmpty()){
            return VectorSafetyResult.builder()
                    .decision(VectorSafetyResult.SafetyDecision.REVIEW)
                    .score(0.0)
                    .reason("No show content indexed to compare against")
                    .build();
        }

        double bestScore = results.get(0).getScore() != null
                ? results.get(0).getScore()
                : 0.0;

        String topTitle = (String) results.get(0)
                .getMetadata().getOrDefault("title","Unknown");

        VectorSafetyResult.SafetyDecision decision;
        String reason;

        if(bestScore >= safeThreshold){
            decision = VectorSafetyResult.SafetyDecision.SAFE;
            reason = String.format(
                    "High brand-content alignment (%.2f)", bestScore);
        } else if (bestScore >= reviewThreshold) {
            decision = VectorSafetyResult.SafetyDecision.REVIEW;
            reason = String.format(
                    "Moderate alignment — human review needed (%.2f)", bestScore);
        } else {
            decision = VectorSafetyResult.SafetyDecision.BLOCK;
            reason = String.format(
                    "Low brand-content alignment (%.2f) — creative blocked", bestScore);
        }

        log.info("Safety score for advertiser '{}': {} → {}",
                advertiser, bestScore, decision);

        return VectorSafetyResult.builder()
                .decision(decision)
                .score(bestScore)
                .reason(reason)
                .topMatchTitle(topTitle)
                .build();
    }

    public void indexShowBatch(List<ShowContent> shows){
        List<Document> docs = shows.stream().map( s ->
                new Document(
                        String.format(
                                "Title: %s. Genre: %s. Rating: %s. Synopsis: %s",
                                s.title(), s.genre(), s.contentRating(), s.synopsis()
                        ),
                        Map.of(
                                "showId", s.showId(),
                                "title", s.title(),
                                "genre", s.genre(),
                                "type", "show"
                        )
                )
        ).toList();
        vectorStore.add(docs);
        log.info("Indexed {} shows into vector store", docs.size());
    }

    public record ShowContent(
            String showId,
            String title,
            String synopsis,
            String genre,
            String contentRating
    ){}
}
