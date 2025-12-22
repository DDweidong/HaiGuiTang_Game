package com.weidong.gamebackend.controller;

import com.weidong.gamebackend.mapper.TurtleSoupMapper;
import com.weidong.gamebackend.model.TurtleSoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/turtle-soups")
public class TurtleSoupController {

    @Autowired
    private TurtleSoupMapper turtleSoupMapper;

    // GET: 查询所有海龟汤
    @GetMapping
    public ResponseEntity<List<TurtleSoup>> getAllTurtleSoups() {
        List<TurtleSoup> soups = turtleSoupMapper.findAll();
        return ResponseEntity.ok(soups);
    }

    // GET: 根据 ID 查询
    @GetMapping("/{userId}")
    public ResponseEntity<List<TurtleSoup>> getTurtleSoupsByUserId(@PathVariable String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<TurtleSoup> soups = turtleSoupMapper.findByUserId(userId);
        return ResponseEntity.ok(soups);
    }

    // POST: 新增一条海龟汤
    @PostMapping
    public ResponseEntity<TurtleSoup> createTurtleSoup(@RequestBody TurtleSoup soup) {
        int rows = turtleSoupMapper.insert(soup);
        if (rows > 0) {
            return ResponseEntity.ok(soup); // 返回包含生成 ID 的对象
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/test")
    public TurtleSoup test() {
        TurtleSoup soup = new TurtleSoup();
        soup.setUserId("test123");
        soup.setTitle("test title");
        System.out.println(soup); // 能否正常打印？
        return soup;
    }
}