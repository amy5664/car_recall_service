package com.boot.dao;

import com.boot.dto.Criteria;
import com.boot.dto.FaqDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FaqDAO {
    List<FaqDTO> listWithPaging(Criteria cri);
    int getTotalCount();
    FaqDTO getFaq(long faq_id);
    void write(FaqDTO faqDTO);
    void modify(FaqDTO faqDTO);
    void delete(long faq_id);
    List<FaqDTO> searchByKeyword(@Param("keyword") String keyword);
}
