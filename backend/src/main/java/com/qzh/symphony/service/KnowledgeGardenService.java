package com.qzh.symphony.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qzh.symphony.DAO.KnowledgeGarden;
import com.qzh.symphony.DAO.Mistake;

import java.io.IOException;

public interface KnowledgeGardenService extends IService<KnowledgeGarden> {
    void asyncGenerateKnowledgeGarden(Mistake mistake, String userId);
    void generateKnowledgeGarden(Mistake mistake, String userId) throws IOException;
}
