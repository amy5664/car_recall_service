package com.boot.service;

import com.boot.dao.NoticeDAO;
import com.boot.dto.Criteria;
import com.boot.dto.NoticeDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeDAO noticeDAO;

    @Override
    public List<NoticeDTO> listWithPaging(Criteria cri) {
        return noticeDAO.listWithPaging(cri);
    }

    @Override
    public int getTotalCount() {
        return noticeDAO.getTotalCount();
    }

    @Override
    public void write(NoticeDTO notice) {
        noticeDAO.write(notice);
    }

    @Transactional
    @Override
    public NoticeDTO getNotice(Long notice_id) {
        noticeDAO.incrementViews(notice_id);
        return noticeDAO.getNotice(notice_id);
    }

    @Override
    public NoticeDTO getNoticeWithoutViews(Long notice_id) {
        return noticeDAO.getNotice(notice_id);
    }

    @Override
    public void modify(NoticeDTO notice) {
        noticeDAO.modify(notice);
    }

    @Override
    public void delete(Long notice_id) {
        noticeDAO.delete(notice_id);
    }

    @Override
    public List<NoticeDTO> searchByKeyword(String keyword) {
        return noticeDAO.searchByKeyword(keyword);
    }
}
