package com.boot.service;

import com.boot.dto.Criteria;
import com.boot.dto.NoticeDTO;

import java.util.List;

public interface NoticeService {
    List<NoticeDTO> listWithPaging(Criteria cri);
    int getTotalCount();
    void write(NoticeDTO notice);
    NoticeDTO getNotice(Long notice_id);
    NoticeDTO getNoticeWithoutViews(Long notice_id);
    void modify(NoticeDTO notice);
    void delete(Long notice_id);
    List<NoticeDTO> searchByKeyword(String keyword);
}
