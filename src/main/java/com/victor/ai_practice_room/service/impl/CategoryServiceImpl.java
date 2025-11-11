package com.victor.ai_practice_room.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.victor.ai_practice_room.entity.Category;
import com.victor.ai_practice_room.entity.Question;
import com.victor.ai_practice_room.exception.ExamException;
import com.victor.ai_practice_room.mapper.CategoryMapper;
import com.victor.ai_practice_room.mapper.QuestionMapper;
import com.victor.ai_practice_room.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    QuestionMapper questionMapper;
    /*
    获取所有类别并通过Stream方法将每一个类别的数量绑定到返回对象上
     */
    @Override
    public List<Category> getAllCategories() {
        //获取所有分类并按照sort升序排序
        List<Category> categories = baseMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        addCategoryQuestionCount(categories);
        return categories;
    }

    /*
    获取分类树
     */
    @Override
    public List<Category> getCategoryTree() {
        //获取所有的分类，按照sort排序
        List<Category> categories = baseMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        //调用给分类题目添加数量的方法
        addCategoryQuestionCount(categories);
        //调用构建分类树方法
        List<Category> categoriesTree = buildCategoryTree(categories);
        return categoriesTree;
    }
    @Override
    public boolean save(Category category) {
        /**
         * 业务需求：
         * 1、判断新的分类在当前父分类中是否已经存在
         * 2、根据输入的新的分类的名字和parent_id查询新添加的分类是否已经存在
         */
        LambdaQueryWrapper<Category> categoryLambdaQueryWrapper = new LambdaQueryWrapper<>();
        categoryLambdaQueryWrapper.eq(Category::getName,category.getName()).eq(Category::getParentId,category.getParentId());
        boolean exists = baseMapper.exists(categoryLambdaQueryWrapper);
        if(exists){
            System.out.println(category.getParentId());
            Category parentCategory = baseMapper.selectById(category.getParentId());
            System.out.println(parentCategory);
            throw new ExamException(441,"在父分类<"+parentCategory.getName()+">下已经存在 '"+category.getName()+"'");
        }
        return super.save(category);
    }

    @Override
    public Boolean update(Category category) {
        //更新分类的时候不能更新为该父分类下其他分类的名称
        boolean exists = baseMapper.exists(new LambdaQueryWrapper<Category>().eq(Category::getName, category.getName()).eq(Category::getParentId, category.getParentId()).ne(Category::getId, category.getId()));
        if(exists){
            Category parentCategory = baseMapper.selectById(category.getParentId());
            throw new ExamException(442,"在父分类<"+parentCategory.getName()+">下已经存在 '"+category.getName()+"'，请修改一个其他分类名");
        }
        return baseMapper.updateById(category) > 0;
    }

    @Override
    public Boolean deleteById(Long id) {
        //判断当前分类下有无子分类
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        if(count>0){
            throw new ExamException(443,"该分类下有子分类，不能删除");
        }
        //判断当前题目下有无题目
        Long questionCount = questionMapper.selectCount(new LambdaQueryWrapper<Question>().eq(Question::getCategoryId, id));
        if(questionCount>0){
            throw new ExamException(443,"该子分类下有题目，不能删除");
        }
        return baseMapper.deleteById(id)>0;
    }

    //给每一个分类添加题目数量
    private void addCategoryQuestionCount(List<Category> categories) {
        //获取每一个category对应的题目数量
        List<Map<String, Long>> categoriesCount = questionMapper.getCategoriesCount();
        //将List<Map<String, Integer>>转换为Map<String, Integer>
        Map<Long, Long> categoryMap = categoriesCount.stream().collect(Collectors.toMap(
                k -> k.get("category_id"),
                v -> v.get("count")
        ));
        //遍历所有分类
        categories.forEach(category->{
            Long count = categoryMap.getOrDefault(category.getId(),0L);
            category.setCount(count);
        });
    }

    //构建分类树的方法
    private List<Category> buildCategoryTree(List<Category> categories) {
        //根据parentId分组获取每个分类下的子分类列表
        Map<Long, List<Category>> categoryMap = categories.stream().collect(Collectors.groupingBy(Category::getParentId));
        //遍历所有的分类
        categories.forEach(category ->{
            //获取当前id分类
            Long categoryId = category.getId();
            //从categoryMap中获取当前分类的子分类列表，这里的new ArrayList是因为要确保返回一个空列表而非null值
            List<Category> childrenCategoryList = categoryMap.getOrDefault(categoryId,new ArrayList<>());
            //给当前分类设置孩子
            category.setChildren(childrenCategoryList);
            //获取当前分类中所有子分类中题目数量总和
            long childrenCategoryCounts = childrenCategoryList.stream().mapToLong(Category::getCount).sum();
            //获取当前分类的题目数量
            Long currentCategoryCounts = category.getCount();
            //重新设置当前分类的题目数量
            category.setCount(currentCategoryCounts+childrenCategoryCounts);
        });
        //获取所有parentId为0的分类
        List<Category> parentCategoryList = categories.stream().filter(category -> category.getParentId() == 0).collect(Collectors.toList());
        return parentCategoryList;
    }

}