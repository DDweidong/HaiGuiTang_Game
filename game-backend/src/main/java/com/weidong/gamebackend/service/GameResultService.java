package com.weidong.gamebackend.service;

import com.weidong.gamebackend.mapper.TurtleSoupMapper;
import com.weidong.gamebackend.model.TurtleSoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GameResultService {

    @Autowired
    private TurtleSoupMapper turtleSoupMapper;

    public void saveTurtleSoup(TurtleSoup soup) {
        turtleSoupMapper.insert(soup);
    }
}