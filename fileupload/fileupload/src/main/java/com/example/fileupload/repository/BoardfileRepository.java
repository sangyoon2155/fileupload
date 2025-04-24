package com.example.fileupload.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fileupload.entity.Board;
import com.example.fileupload.entity.Boardfile;

public interface BoardfileRepository extends JpaRepository<Boardfile, Integer>{
	List<Boardfile> findByBno(int bno);
	
	// PK 한정삭제
	// void delelteById(int key) 사용
	
	// FK 여러행 삭제(Board 삭제 시 같이 삭제 : 트랜잭션처리)
	void deleteByBno(int bno);
	
}
