package com.weidong.gamebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weidong.gamebackend.common.PageResult;
import com.weidong.gamebackend.model.TurtleSoup;
import com.weidong.gamebackend.vo.TurtleSoupVO;

/**
 * 完成记录服务。
 * 继承 IService 获得 MyBatis-Plus 的通用 CRUD 能力。
 */
public interface TurtleSoupService extends IService<TurtleSoup> {

    /** 分页查询指定用户的完成记录，按完成时间倒序 */
    PageResult<TurtleSoupVO> pageSoupsByUser(String userId, long pageNum, long pageSize);

    /** 保存一条完成记录，返回含生成 ID 的视图对象 */
    TurtleSoupVO saveTurtleSoup(TurtleSoup soup);
}
