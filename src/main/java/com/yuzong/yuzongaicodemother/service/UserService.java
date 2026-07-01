package com.yuzong.yuzongaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yuzong.yuzongaicodemother.model.dto.user.UserQueryRequest;
import com.yuzong.yuzongaicodemother.model.entity.User;
import com.yuzong.yuzongaicodemother.model.vo.LoginUserVO;
import com.yuzong.yuzongaicodemother.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author 齐夏：Yuzong，我看到了生生不息的激荡
 */
public interface UserService extends IService<User> {


    /**
     * 1. 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 【加密】BCrypt密码加密方法
     * @return 加密后的密码
     * 加密原理：加密后的密码=A部分（明文密码转化的固定的玩意）+B部分（随机盐）+C部分组成。
     *    A部分密码相同的情况下是固定不变的（大致如此），B部分是随机的盐，C部分是由A部分+B部分进行一个未知算法得出的C。
     * 密码验证原理：检验密码的时候只需要将输入进来的密码转化为A部分，然后用A+B部分进行未知算法运算，得出来的C和数据库存放C一致就可以登录了。
     */
    String getBCryptPassword(String userPassword);

    /**
     * 获取脱敏的已登录用户信息 实体类转VO
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 2. 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 3. 获取当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 4. 用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 5. 获得脱敏的用户信息（单个）
     */
    UserVO getUserVO(User user);
    /**
     * 6. 获得脱敏的用户信息（分页）
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 7. 根据查询条件构造数据查询参数
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
}
