package com.boot.dao;

import com.boot.dto.BoardDTO;
import com.boot.dto.Criteria;

import java.util.ArrayList;

public interface PageDAO {

	public ArrayList<BoardDTO> listWithPaging(Criteria cri);
	public int getTotalCount(Criteria cri);
}


























