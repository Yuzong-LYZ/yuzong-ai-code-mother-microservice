package com.yuzong.yuzongaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yuzong.yuzongaicodemother.exception.BusinessException;
import com.yuzong.yuzongaicodemother.exception.ErrorCode;
import com.yuzong.yuzongaicodemother.model.dto.user.UserQueryRequest;
import com.yuzong.yuzongaicodemother.model.entity.User;
import com.yuzong.yuzongaicodemother.mapper.UserMapper;
import com.yuzong.yuzongaicodemother.model.enums.UserRoleEnum;
import com.yuzong.yuzongaicodemother.model.vo.LoginUserVO;
import com.yuzong.yuzongaicodemother.model.vo.UserVO;
import com.yuzong.yuzongaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.yuzong.yuzongaicodemother.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author 齐夏：Yuzong，我看到了生生不息的激荡
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

    /**
     * 1. 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查是否重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密
        String encryptPassword = getBCryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("Momo"); // 默认用户昵称
        // 默认头像
        user.setUserAvatar("https://img-yuzong.oss-cn-guangzhou.aliyuncs.com/picturesProject/public/0-default-profile-picture/头像momo.png");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }
    /**
     * 2. 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短，需要>=4");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短，需要>=8");
        }

        // 2. 检查用户账号是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            log.info("用户登录失败，用户不存在");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不存在或者密码错误");
        }


        // 3. 验证密码（使用BCrypt的matches方法）（这里应该和密码加密封装在一起的，但是当时忘了，不想改了）
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
// 这里是使用BCrypt的matches方法进行密码验证：将输入进来的密码转化为A部分，然后用A+B部分进行未知算法运算，得出来的C和数据库存放C一致就可以登录了。
        if (!encoder.matches(userPassword, user.getUserPassword())) {
            log.info("用户登录失败，密码错误");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不存在或者密码错误");
        }
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 5. 获得脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

    /**
     * 【加密】BCrypt密码加密方法
     * @return 加密后的密码
     * 加密原理：加密后的密码=A部分（明文密码转化的固定的玩意）+B部分（随机盐）+C部分组成。
     *    A部分密码相同的情况下是固定不变的（大致如此），B部分是随机的盐，C部分是由A部分+B部分进行一个未知算法得出的C。
     * 密码验证原理：检验密码的时候只需要将输入进来的密码转化为A部分，然后用A+B部分进行未知算法运算，得出来的C和数据库存放C一致就可以登录了。
     */
    @Override
    public String getBCryptPassword(String userPassword) {
//      1.new一个BCryptPasswordEncoder对象，就可以使用这个对象进行密码加密了，里面有个encode方法，这个方法就是进行密码加密的。
//        12是迭代次数，迭代次数越多，加密越安全，但是加密速度越慢。
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
//      2.进行加密，返回结果用Password接收
        String Password = encoder.encode(userPassword);
//      3.返回加密后的密码
        return Password;
    }

    /**
     * 获取脱敏的已登录用户信息 实体类转VO
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 3. 获取当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }
    /**
     * 4. 用户注销
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 5. 获得脱敏的用户信息（单个）
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 6. 获得脱敏的用户信息（分页）
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 7. 根据查询条件构造数据查询参数
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }


}
