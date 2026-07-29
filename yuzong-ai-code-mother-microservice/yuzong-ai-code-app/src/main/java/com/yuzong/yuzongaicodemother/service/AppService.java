package com.yuzong.yuzongaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yuzong.yuzongaicodemother.model.dto.app.AppAddRequest;
import com.yuzong.yuzongaicodemother.model.dto.app.AppQueryRequest;
import com.yuzong.yuzongaicodemother.model.entity.App;
import com.yuzong.yuzongaicodemother.model.entity.User;
import com.yuzong.yuzongaicodemother.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author 齐夏：Yuzong，我看到了生生不息的激荡
 */
public interface AppService extends IService<App> {

    /**
     * 4. 调用 AI（门面） 生成代码--通过对话生成应用代码
     *
     * @param appId     应用 ID
     * @param message   用户消息
     * @param loginUser 登录用户
     * @return 生成的代码
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

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


    /**
     * 5. 部署应用
     *
     * @param appId     应用 ID
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 6. 创建应用
     *
     * @param appAddRequest 创建应用的请求参数
     * @param loginUser     登录用户
     * @return 新建的应用ID
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);
}
