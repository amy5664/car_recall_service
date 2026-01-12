package com.boot.service;

import com.boot.dao.FaqDAO;
import com.boot.dto.Criteria;
import com.boot.dto.FaqDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqServiceImpl implements FaqService {
    private final FaqDAO faqDAO;

    @Override
    public List<FaqDTO> getFaqList(Criteria cri) {
        return faqDAO.listWithPaging(cri);
    }

    @Override
    public int getTotal() {
        return faqDAO.getTotalCount();
    }

    @Override
    public FaqDTO getFaq(long faq_id) {
        return faqDAO.getFaq(faq_id);
    }

    @Override
    public void writeFaq(FaqDTO faqDTO) {
        faqDAO.write(faqDTO);
    }

    @Override
    public void modifyFaq(FaqDTO faqDTO) {
        faqDAO.modify(faqDTO);
    }

    @Override
    public void deleteFaq(long faq_id) {
        faqDAO.delete(faq_id);
    }

    @Override
    public List<FaqDTO> searchByKeyword(String keyword) {
        return faqDAO.searchByKeyword(keyword);
    }
}
