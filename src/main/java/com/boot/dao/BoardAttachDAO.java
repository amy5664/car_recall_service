package com.boot.dao;

import com.boot.dto.BoardAttachDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BoardAttachDAO {
    public void insert(BoardAttachDTO dto);
    public List<BoardAttachDTO> findByBoardNo(int boardNo);
    public void deleteAll(int boardNo);
}