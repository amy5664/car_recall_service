package com.boot.dao;

import com.boot.dto.Criteria;
import com.boot.dto.NoticeDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeDAO {
    List<NoticeDTO> listWithPaging(Criteria cri);
    int getTotalCount();
    void write(NoticeDTO noticeDTO);
    NoticeDTO getNotice(long notice_id);
    void incrementViews(long notice_id);
    void modify(NoticeDTO noticeDTO);
    void delete(long notice_id);
    List<NoticeDTO> searchByKeyword(@Param("keyword") String keyword);
}
