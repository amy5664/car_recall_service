package com.boot.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.boot.dto.ComplainDTO;

@Mapper
public interface ComplainDAO {
	public ArrayList<ComplainDTO> complain_list();
	public ArrayList<ComplainDTO> find_modify_content(HashMap<String, String> param);
	public void complain_write(ComplainDTO complainDTO);
	public ComplainDTO contentView(HashMap<String, String> param);
	public void update(ComplainDTO complainDTO); // complain_modify를 대체
	public void complain_delete(HashMap<String, String> param);
	public void updateAnswer(HashMap<String, String> param);
	public ComplainDTO getComplainById(int reportId);
	public List<ComplainDTO> getComplainListByReporterName(@Param("reporterName") String reporterName);
}
