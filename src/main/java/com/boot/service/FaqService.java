package com.boot.service;

import com.boot.dto.Criteria;
import com.boot.dto.FaqDTO;

import java.util.List;

public interface FaqService {
    List<FaqDTO> getFaqList(Criteria cri);
    int getTotal();
    FaqDTO getFaq(long faq_id);
    void writeFaq(FaqDTO faqDTO);
    void modifyFaq(FaqDTO faqDTO);
    void deleteFaq(long faq_id);
    List<FaqDTO> searchByKeyword(String keyword);
}
