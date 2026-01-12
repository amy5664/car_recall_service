package com.boot.service;

import com.boot.dao.BoardAttachDAO;
import com.boot.dao.BoardDAO;
import com.boot.dto.BoardDTO;
import com.boot.dto.BoardAttachDTO;
import com.boot.dto.Criteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardServiceImpl implements BoardService {

    private final BoardDAO boardDAO;
    private final BoardAttachDAO attachDAO;

    @Override
    public List<BoardDTO> list() {
        return boardDAO.list();
    }

    @Override
    public List<BoardDTO> listWithPaging(Criteria cri) {
        return boardDAO.listWithPaging(cri);
    }

    @Override
    public int getTotalCount(Criteria cri) {
        return boardDAO.getTotalCount(cri);
    }

    @Override
    @Transactional
    public void write(BoardDTO board) {
        log.info("@# BoardServiceImpl write()");
        
        // 1. 게시글 DB 저장 (이후 board.boardNo에 생성된 pk가 담김)
        boardDAO.write(board);
        log.info("@# writer board =>" + board);

        // 2. 첨부파일이 없으면 여기서 로직 종료
        if (board.getUploadFile() == null || board.getUploadFile().isEmpty()) {
            return;
        }

        // 3. 첨부파일 업로드 및 DB 저장
        board.getUploadFile().forEach(attach -> {
            if (attach != null && !attach.isEmpty()) {
                String uploadFolder = "C:\\upload"; // 실제 파일 저장 경로
                String uploadFolderPath = getFolder();
                File uploadPath = new File(uploadFolder, uploadFolderPath);

                if (!uploadPath.exists()) {
                    uploadPath.mkdirs();
                }

                BoardAttachDTO attachDTO = new BoardAttachDTO();
                String originalFileName = attach.getOriginalFilename();
                UUID uuid = UUID.randomUUID();
                String uploadFileName = uuid.toString() + "_" + originalFileName;

                try {
                    File saveFile = new File(uploadPath, uploadFileName);
                    attach.transferTo(saveFile);

                    attachDTO.setUuid(uuid.toString());
                    attachDTO.setUploadPath(uploadFolderPath);
                    attachDTO.setFileName(originalFileName);
                    attachDTO.setBoardNo(board.getBoardNo());

                    if (checkImageType(saveFile)) {
                        attachDTO.setImage(true);
                    }

                    log.info("@# attachDTO => " + attachDTO);
                    attachDAO.insert(attachDTO);

                } catch (IOException e) {
                    log.error("File upload error", e);
                }
            }
        });
    }

    @Override
    @Transactional
    public BoardDTO contentView(int boardNo) {
        boardDAO.hitUp(boardNo);
        BoardDTO board = boardDAO.contentView(boardNo);
        board.setAttachList(attachDAO.findByBoardNo(boardNo));
        return board;
    }

    @Override
    @Transactional
    public void modify(BoardDTO board) {
        // 파일 처리 로직 추가 필요 (기존 파일 삭제, 새 파일 추가)
        boardDAO.modify(board);
    }

    @Override
    @Transactional
    public void delete(int boardNo) {
        log.info("@# delete board: " + boardNo);
        attachDAO.deleteAll(boardNo); // 첨부파일 정보 먼저 삭제
        boardDAO.delete(boardNo); // 게시글 삭제
    }

    @Override
    public List<BoardDTO> searchByKeyword(String keyword) {
        return boardDAO.searchByKeyword(keyword);
    }

    @Override
    public List<BoardAttachDTO> getFileList(int boardNo) {
        log.info("@# getFileList by boardNo: " + boardNo);
        return attachDAO.findByBoardNo(boardNo);
    }

    // 날짜별 폴더 생성 유틸리티 메서드
    private String getFolder() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return sdf.format(date);
    }

    // 이미지 파일 여부 체크 유틸리티 메서드
    private boolean checkImageType(File file) {
        try {
            String contentType = Files.probeContentType(file.toPath());
            if (contentType != null) {
                return contentType.startsWith("image");
            }
        } catch (IOException e) {
            log.error("Error checking image type", e);
        }
        return false;
    }
}
