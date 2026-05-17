package com.qzh.symphony.common.utils;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HanLPUtils {
    //中文分词
    public static List<String> segment(String text) {
        // 用 ansj 分词库把一段文字切成词语
        return ToAnalysis.parse(text).getTerms().stream()
                .map(Term::getName)
                //过滤掉单字（"的""了""在"这类没意义的词）
                .filter(w -> w.length() > 1)
                //返回词语列表
                .collect(Collectors.toList());
    }

    //Jaccard相似度计算
    public static double jaccardSimilarity(String a, String b) {
        List<String> tokensA = segment(a);
        List<String> tokensB = segment(b);
        if (tokensA.isEmpty() && tokensB.isEmpty()) return 1.0;
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0;

        Set<String> setA = new HashSet<>(tokensA);
        Set<String> setB = new HashSet<>(tokensB);
        Set<String> intersection = new HashSet<>(setA);
        //交集
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        //并集
        union.addAll(setB);
        //利用交集除以并集计算相似度,结果在0到1之间
        return (double) intersection.size() / union.size();
    }
}