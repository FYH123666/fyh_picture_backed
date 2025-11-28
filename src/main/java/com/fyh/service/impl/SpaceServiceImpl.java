package com.fyh.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fyh.exception.BusinessException;
import com.fyh.exception.ErrorCode;
import com.fyh.exception.ThrowUtils;
import com.fyh.model.dto.picture.PictureQueryRequest;
import com.fyh.model.dto.space.SpaceAddRequest;
import com.fyh.model.dto.space.SpaceQueryRequest;
import com.fyh.model.entity.Picture;
import com.fyh.model.entity.Space;
import com.fyh.model.entity.User;
import com.fyh.model.enums.SpaceLevelEnum;
import com.fyh.model.vo.PictureVO;
import com.fyh.model.vo.SpaceVO;
import com.fyh.model.vo.UserVO;
import com.fyh.service.SpaceService;
import com.fyh.mapper.SpaceMapper;
import com.fyh.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
* @author 16193
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-11-26 16:29:16
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{



    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;
    /**
     * 校验空间数据
     *
     * @param space
     * @param add   是新增操作还是修改操作
     */
    public void validSpace(Space space ,boolean add)
    {
        ThrowUtils.throwIf(space==null, ErrorCode.PARAMS_ERROR);
        String spaceName=space.getSpaceName();
        Integer spaceLevel=space.getSpaceLevel();
        SpaceLevelEnum levelEnum=SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 仅新增时，校验名称
        if(add)
        {
            if (StrUtil.isBlank(spaceName))
            {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称不能为空");
            }
            if (spaceLevel==null)
            {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间级别不能为空");
            }
        }
        if (spaceLevel!=null&&levelEnum==null)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间级别不存在");
        }
        if(StrUtil.isNotBlank(spaceName)&&spaceName.length()>30)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称过长");
        }
    }

    public void fillSpaceBySpaceLevel(Space space)
    {
        //根据空间级别填充最大图片数量和大小
        SpaceLevelEnum levelEnum=SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if(levelEnum!=null)
        {

            long maxSize=levelEnum.getMaxSize();
            if (space.getMaxSize()==null)
            {
                space.setMaxSize(maxSize);
            }
            long maxCount=levelEnum.getMaxCount();
            if (space.getMaxCount()==null)
            {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 获取空间查询包装类
     * @param spaceQueryRequest
     * @return
     */
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if(spaceQueryRequest==null){
            return queryWrapper;
        }
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        queryWrapper.eq(ObjUtil.isNotEmpty(id),"id",id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId),"userId",userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName),"spaceName",spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel),"spaceLevel",spaceLevel);

        queryWrapper.orderBy(
                StrUtil.isNotBlank(sortField),  // 条件：是否排序
                sortOrder.equals("ascend"),     // 排序方向：true=升序，false=降序
                sortField                       // 排序字段
        );//ascend升序的意思，对应sql中asc
        return queryWrapper;
    }

    /**
     * 获取空间封装类
     * @param space
     * @param request
     * @return
     */
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        //对象封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        //获取用户信息
        Long userId = space.getId();
        if(userId!=null && userId>0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }


    /**
     * 获取空间分页封装类
     * @param spacePage
     * @param request
     * @return
     */
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList=spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if(CollUtil.isEmpty(spaceList))
        {
            return spaceVOPage;
        }
        //对象列表--->封装类列表
        List<SpaceVO> spaceVOList=spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        //关联查询用户信息
        Set<Long> userIdSet=spaceList.stream()
                .map(Space::getUserId)
                .collect(Collectors.toSet());
        Map<Long,List< User>> userIdUserMap=userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //填充用户信息
        spaceVOList.forEach(spaceVO -> {
            Long userId=spaceVO.getUserId();
            User user=null;
            if(userIdUserMap.containsKey(userId))
            {
                user=userIdUserMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));

        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }


    // 全局：锁池，使用包装类来记录引用计数
    private final ConcurrentHashMap<Long, LockWrapper> lockMap = new ConcurrentHashMap<>();

    private static class LockWrapper {
        final Object lock = new Object();
        final AtomicInteger refCount = new AtomicInteger(0);
    }
    public long addSpace(SpaceAddRequest spaceAddRequest,User loginUser)
    {
//        1.转换实体类

        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest,space);
//        2.设置名字（默认值
        String spaceName = spaceAddRequest.getSpaceName();
        Integer spaceLevel = spaceAddRequest.getSpaceLevel();
        if(StrUtil.isBlank(spaceName))
        {
            space.setSpaceName(loginUser.getUserName()+"的空间");
        }
//
//        3.设置默认空间等级
        if(spaceLevel==null)
        {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }

//        4.填充数据
        this.fillSpaceBySpaceLevel(space);
//
//        5.数据校验
        this.validSpace(space,true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
//
//        6获取到userId进行权限校验
        if(SpaceLevelEnum.COMMON.getValue()!=spaceAddRequest.getSpaceLevel()&&!userService.isAdmin(loginUser))
        {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限创建指定级别的空间");
        }
//
//        7针对用户进行加锁
//        String lock=String.valueOf(userId).intern();
//        synchronized (lock)
//        {
//            Long newSpaceId=transactionTemplate.execute(status -> {
//
//                boolean exists=this.lambdaQuery().eq(Space::getUserId,userId).exists();
//                ThrowUtils.throwIf(exists,ErrorCode.PARAMS_ERROR,"每个用户仅能创建一个私有空间");
//                //接入数据库
//                boolean result = this.save(space);
//                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间创建失败");
//                return space.getId();
//            });
//            return Optional.ofNullable(newSpaceId).orElse(-1L);
//        }

        // ---------- 7. 针对用户加锁（错误版） ----------
//        //加锁优化，使用ConcurrentHashMap
//        Object lock = lockMap.computeIfAbsent(userId, key -> new Object());
//        synchronized (lock) {
//            try {
//                //存在性校验
//                boolean exists=this.lambdaQuery().eq(Space::getUserId,userId).exists();
//                ThrowUtils.throwIf(exists,ErrorCode.PARAMS_ERROR,"每个用户仅能创建一个私有空间");
//                // 数据库操作
//                boolean result = this.save(space);
//                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间创建失败");
//                return space.getId();
//            } finally {
//                // 防止内存泄漏
//                lockMap.remove(userId);
//            }
//        }

        // ---------- 7. 针对用户加锁（正确版） ----------
        LockWrapper wrapper = lockMap.computeIfAbsent(userId, k -> new LockWrapper());
        wrapper.refCount.incrementAndGet(); // 引用 +1

        synchronized (wrapper.lock) {
            try {
                //存在性校验
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.PARAMS_ERROR,
                        "每个用户仅能创建一个私有空间");

                //事务操作
                Long newSpaceId = transactionTemplate.execute(status -> {
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间创建失败");
                    return space.getId();
                });

                return Optional.ofNullable(newSpaceId).orElse(-1L);

            } finally {
                // 引用 -1，并仅在完全没有线程使用时释放锁对象
                int left = wrapper.refCount.decrementAndGet();
                if (left == 0) {
                    lockMap.remove(userId, wrapper);
                }
            }
        }
    }
}




