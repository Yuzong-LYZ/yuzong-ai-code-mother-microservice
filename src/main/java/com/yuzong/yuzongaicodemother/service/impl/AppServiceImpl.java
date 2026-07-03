package com.yuzong.yuzongaicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yuzong.yuzongaicodemother.model.entity.App;
import com.yuzong.yuzongaicodemother.mapper.AppMapper;
import com.yuzong.yuzongaicodemother.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author 齐夏：Yuzong，我看到了生生不息的激荡
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
