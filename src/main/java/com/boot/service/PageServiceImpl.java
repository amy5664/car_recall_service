package com.boot.service;

import com.boot.dao.PageDAO;
import com.boot.dto.BoardDTO;
import com.boot.dto.Criteria;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
public class PageServiceImpl implements PageService{

	@Autowired
	private SqlSession sqlSession;

	@Override
	public ArrayList<BoardDTO> listWithPaging(Criteria cri) {
		log.info("@# listWithPaging()");
		log.info("@# cri=>"+cri);

		PageDAO dao = sqlSession.getMapper(PageDAO.class);
		ArrayList<BoardDTO> list = dao.listWithPaging(cri);

		return list;
	}

	@Override
	public int getTotalCount(Criteria cri) {
		PageDAO dao = sqlSession.getMapper(PageDAO.class);

		return dao.getTotalCount(cri);
	}
}




