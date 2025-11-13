package com.victor.ai_practice_room.vo;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "题目筛选选项")
public class QuestionQueryVo {
    /**
     * @param categoryId 分类ID筛选条件，可选
     * @param difficulty 难度筛选条件（EASY/MEDIUM/HARD），可选
     * @param type 题型筛选条件（CHOICE/JUDGE/TEXT），可选
     * @param keyword 关键词搜索，对题目标题进行模糊查询，可选
     */
    @Parameter(description = "分类ID筛选条件")
    private Long categoryId;
    @Parameter(description = "难度筛选条件，可选值：EASY/MEDIUM/HARD")
    private String difficulty;
    @Parameter(description = "题型筛选条件，可选值：CHOICE/JUDGE/TEXT")
    private String type;
    @Parameter(description = "关键词搜索，对题目标题进行模糊查询")
    private String keyword;
}
