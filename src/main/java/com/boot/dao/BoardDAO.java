package com.boot.dao;

import java.util.HashMap;
import java.util.List;
import com.boot.dto.Criteria;

import com.boot.dto.BoardDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BoardDAO {
    // 게시글 목록 조회
    public List<BoardDTO> list();

    // 페이징 처리된 목록 조회 (관리자용)
    public List<BoardDTO> listWithPaging(Criteria cri);

    // 전체 게시글 수 조회 (페이징용)
    public int getTotalCount(Criteria cri);

    // 게시글 작성 (인자는 HashMap으로 유지)
    public void write(BoardDTO board);

    // 💡 게시글 내용 조회 (int 타입 boardNo를 받고 @Param 적용)
    public BoardDTO contentView(@Param("boardNo") int boardNo);

    // 💡 게시글 조회수 증가 기능 추가 (contentView 호출 전 서비스에서 사용)
    public void hitUp(@Param("boardNo") int boardNo);

    // 게시글 수정
    public void modify(BoardDTO board);

    // 게시글 삭제
    public void delete(int boardNo);

    // 키워드 검색
    List<BoardDTO> searchByKeyword(@Param("keyword") String keyword);
}
