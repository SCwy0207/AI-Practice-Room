package com.victor.ai_practice_room.service;

import com.victor.ai_practice_room.entity.Paper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.victor.ai_practice_room.vo.PaperVo;

import java.util.List;

/**
 * 试卷服务接口
 */
public interface PaperService extends IService<Paper> {

    List<Paper> selectPaperByName(String name);

    boolean savePaperVo(PaperVo paperVo);

    boolean updatePaperById(PaperVo paperVo, Integer id);

    boolean updatePaperStatus(Integer id, String status);

    boolean deletePaperById(Integer id);

    Paper getPaperDetailsById(Integer id);
}