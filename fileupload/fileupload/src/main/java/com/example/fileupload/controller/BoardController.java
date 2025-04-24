package com.example.fileupload.controller;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fileupload.FileuploadApplication;
import com.example.fileupload.dto.BoardForm;
import com.example.fileupload.entity.Board;
import com.example.fileupload.entity.BoardMapping;
import com.example.fileupload.entity.Boardfile;
import com.example.fileupload.repository.BoardRepository;
import com.example.fileupload.repository.BoardfileRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class BoardController {
	@Autowired BoardRepository boardRepository;
	@Autowired BoardfileRepository boardfileRepository;
	
	@GetMapping("/modifyBoard")
	public String modifyBoard(@RequestParam("bno") int bno, Model model) {
		Board board = boardRepository.findById(bno).orElse(null);
		model.addAttribute("title", board.getTitle());
		model.addAttribute("bno", board.getBno());
		return "modifyBoard";
	}
	
	@PostMapping("/modifyBoard")
	public String modifyBoard(@RequestParam("bno") int bno
								,@RequestParam("pw") String pw
								,@RequestParam("title") String title
								,RedirectAttributes redirectAttributes) {
		
		Board board = boardRepository.findById(bno).orElse(null);
		if (!board.getPw().equals(pw)) {
	        redirectAttributes.addFlashAttribute("msg", "비밀번호가 일치하지 않습니다.");
	        return "redirect:/boardOne?bno=" + bno;
	    }
		board.setTitle(title);
	    boardRepository.save(board);
	    
	    redirectAttributes.addFlashAttribute("msg", "게시글이 성공적으로 수정되었습니다.");
		return "redirect:/boardOne?bno=" + bno;
	}
	
	@GetMapping("/removeBoardForm")
	public String removeBoardForm(@RequestParam(value = "bno") int bno, Model model) {
		model.addAttribute("bno", bno);
		return "removeBoardForm";
	}
	
	@PostMapping("/removeBoard")
	public String removeBoard(@RequestParam(value = "bno") int bno
								,@RequestParam("pw") String pw
								,RedirectAttributes redirectAttributes) {
	    
	    Board board = boardRepository.findById(bno).orElse(null);
	    
	    if (!board.getPw().equals(pw)) {
	        redirectAttributes.addFlashAttribute("msg", "비밀번호가 일치하지 않습니다.");
	        return "redirect:/boardOne?bno=" + bno;
	    }
	    
	    if (board != null) {
	        try {
	            boardRepository.deleteById(bno);
	            redirectAttributes.addFlashAttribute("msg", "게시글이 삭제되었습니다.");
	            return "redirect:/boardList";
	        } catch (DataIntegrityViolationException e) {
	            // 외래 키 제약 조건 때문에 삭제 실패한 경우
	            redirectAttributes.addFlashAttribute("msg", "삭제할 수 없습니다. 관련된 데이터가 존재합니다.");
	            return "redirect:/boardOne?bno=" + bno;
	        }
	    } 
	    
	    // 게시글이 존재하지 않는 경우
	    redirectAttributes.addFlashAttribute("msg", "해당 게시글이 존재하지 않습니다.");
	    return "redirect:/boardList";
	}

	
	
	@GetMapping("/boardOne")
	public String boardOne(Model model, @RequestParam(value = "bno") int bno) {
		BoardMapping boardMapping = boardRepository.findByBno(bno);
		
		
		List<Boardfile> fileList = boardfileRepository.findByBno(bno);
		log.debug("size:"+fileList.size());
		
		model.addAttribute("boardMapping", boardMapping);
		model.addAttribute("fileList", fileList);
		return "boardOne";
	}
	
	@GetMapping({"/","/boardList"})
	public String boardList(Model model) {
		// 페이징
		// sort : bno DESC
		// pageRequest : 0, 10
		// page<BoardMapping>
		List<BoardMapping> list = boardRepository.findAllBy();
		model.addAttribute("list", list);
		return "boardList";
	}
	
	// 입력폼
	@GetMapping("/addBoard")
	public String addBoard() {
		return "addBoard";
	}	
	// 입력액션
	@PostMapping("/addBoard")
	public String addBaord(BoardForm boardForm) {
		log.debug(boardForm.toString());
		// 이슈: 파일을 첨부하지 않아도 fileSize는 1 이다
		log.debug("MultipartFile Size: " + boardForm.getFileList().size());
		
		Board board = new Board();
		board.setTitle(boardForm.getTitle());
		board.setPw(boardForm.getPw());
		boardRepository.save(board); // board 저장
		int bno = board.getBno(); //board insert 후 bno 변경되었는지
		log.debug("bno: "+bno);
		
		// 파일 분리
		List<MultipartFile> fileList = boardForm.getFileList();
		long firtFileSize = fileList.get(0).getSize();
		log.debug("firtFileSize: " + firtFileSize);
		
		// 이슈: 파일을 첨부하지 않아도 fileSize는 1 이다
		if(firtFileSize > 0) { // 첫번째 파일사이즈가 0이상이다 -> 첨부된 파일이 있다 
			for(MultipartFile f : fileList) {
				if(f.getContentType().equals("application/octet-stream") || f.getSize() > 1024*1024*10) {
					return "redirect:/boardList";
				}
			}
			
			
			
			for(MultipartFile f : fileList) {
				log.debug("파일타입: "+f.getContentType());
				log.debug("원본이름: "+f.getOriginalFilename());
				log.debug("파일용량: "+f.getSize());
				// 확장자 추출
				String ext = f.getOriginalFilename().substring(f.getOriginalFilename().lastIndexOf(".")+1);
				log.debug("확장자: "+ext);
				String saveName = UUID.randomUUID().toString().replace("-", "");
				log.debug("저장파일이름: "+saveName);
				
				File emptyFile = new File("c:/project/upload/"+saveName+"."+ext);
				// f의 byte -> emptyFile 복사
				try {
					f.transferTo(emptyFile);
				} catch (Exception e) {
					log.error("파일저장실패!");
					e.printStackTrace();
				}
				
				// boardfile테이블에도 파일정보 저장
				Boardfile boardfile = new Boardfile();
				boardfile.setBno(board.getBno());
				boardfile.setFext(ext);
				boardfile.setFname(saveName);
				boardfile.setForiginname(f.getOriginalFilename());
				boardfile.setFsize(f.getSize());
				boardfile.setFtype(f.getContentType());
				boardfileRepository.save(boardfile);
			}
		}
		return "redirect:/boardList";
	}
}
