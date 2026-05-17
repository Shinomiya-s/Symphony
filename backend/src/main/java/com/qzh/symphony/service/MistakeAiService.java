package com.qzh.symphony.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qzh.symphony.DAO.Mistake;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MistakeAiService {
    void extractKnowledgePoints(String mistakeId, String question, String wrongReason);
    List<Mistake> findByUserId(String userId);
    Map<String, Object> analyzeKnowledgePoints(String userId) throws IOException;
    String buildPrompt(Map<String, Integer> statistic, int totalCount);
    List<Map<String, String>> generatePractice(String userId, List<String> weakPoints) throws IOException;

    LocalDate calcNextReviewTime(int reviewCount);
    List<Mistake> getTodayReview(String userId);
}