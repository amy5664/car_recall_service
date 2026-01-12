package com.boot.controller;

import com.boot.dto.BoardDTO;
import com.boot.dto.Criteria;
import com.boot.dto.PageDTO;
import com.boot.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService service;

    @GetMapping("/list")
    public String list(Criteria cri, Model model) {
        log.info("@# Board list");
        log.info("@# cri => " + cri);

        List<BoardDTO> list = service.listWithPaging(cri);
        int total = service.getTotalCount(cri);

        model.addAttribute("list", list);
        model.addAttribute("pageMaker", new PageDTO(cri, total));
        return "report_recallInfo"; // 확장자 없음 - OK
    }

    @PostMapping("/write")
    public String write(BoardDTO boardDTO) {
        log.info("@# write()");
        log.info("@# boardDTO=>"+boardDTO);

        // 서비스 계층에서 파일 정보를 포함하여 게시글을 저장하도록 호출
        service.write(boardDTO);

        return "redirect:/admin/press/list"; // 관리자 목록 페이지로 리다이렉트
    }

    @GetMapping("/write_view")
    public String write_view() {
        log.info("@# write_view()");
        return "report_write_view"; // 확장자 없음 - OK
    }

    @GetMapping("/get")
    public String get(@RequestParam("boardNo") int boardNo, @ModelAttribute("cri") Criteria cri, Model model) {
        log.info("@# /get");

        // service.contentView() 메서드에 조회수 증가 로직이 포함되어 있으므로 별도 호출 불필요
        BoardDTO dto = service.contentView(boardNo);
        model.addAttribute("board", dto); // 모델 이름을 'board'로 변경
        // @ModelAttribute("cri")는 cri 객체를 모델에 자동으로 추가해줍니다.
        return "report_content_view"; // 확장자 없음 - OK
    }

    @GetMapping("/report_modify_view")
    public String report_modify_view(@RequestParam("boardNo") int boardNo,
                                     @RequestParam("pageNum") int pageNum,
                                     @RequestParam("amount") int amount,
                                     Model model) {
        log.info("@# report_modify_view()");
        // 수정 폼에서는 조회수가 오르면 안되므로, 별도의 메서드가 필요하지만 일단 contentView 사용
        BoardDTO dto = service.contentView(boardNo);
        model.addAttribute("content_view", dto);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("amount", amount); // 확장자 없음 - OK
        return "report_modify_view";
    }

    @PostMapping("/report_modify")
    public String report_modify(BoardDTO boardDTO, Criteria cri, RedirectAttributes rttr) {
        log.info("@# report_modify()");
        service.modify(boardDTO);
        rttr.addAttribute("pageNum", cri.getPageNum());
        rttr.addAttribute("amount", cri.getAmount());
        return "redirect:/board/list";
    }

    @PostMapping("/report_delete")
    public String report_delete(@RequestParam("boardNo") int boardNo, Criteria cri, RedirectAttributes rttr) {
        log.info("@# report_delete()");
        service.delete(boardNo);
        rttr.addAttribute("pageNum", cri.getPageNum());
        rttr.addAttribute("amount", cri.getAmount());
        return "redirect:/board/list";
    }
}
