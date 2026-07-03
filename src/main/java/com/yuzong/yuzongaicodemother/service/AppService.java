package com.yuzong.yuzongaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yuzong.yuzongaicodemother.model.dto.app.AppQueryRequest;
import com.yuzong.yuzongaicodemother.model.entity.App;
import com.yuzong.yuzongaicodemother.model.vo.AppVO;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author 齐夏：Yuzong，我看到了生生不息的激荡
 */
public interface AppService extends IService<App> {

    /**
     * 1. 应用类转VO  --获取应用包装类（封装类也行）
     *
     * @param app 应用
     * @return 应用包装类VO
     */
    AppVO getAppVO(App app);

    /**
     * 2. 获取查询条件--构造应用查询条件
     *
     * @param appQueryRequest 应用查询参数
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 3. 获取应用列表封装
     *
     * @param appList 应用列表
     * @return 应用列表VO
     */
    List<AppVO> getAppVOList(List<App> appList);
}
