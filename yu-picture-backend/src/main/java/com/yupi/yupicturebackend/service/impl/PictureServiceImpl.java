package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.FileManager;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private FileManager fileManager;

    /**
     * 校验图片信息：id，url，introduction
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 1.从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 2.依次校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if(StrUtil.isNotBlank(url)){
            ThrowUtils.throwIf(url.length()>1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if(StrUtil.isNotBlank(introduction))
            ThrowUtils.throwIf(introduction.length()>800, ErrorCode.PARAMS_ERROR, "简介过长");
    }

    /**
     * 上传图片
     * @param multipartFile 文件
     * @param pictureUploadRequest  图片上传请求
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 1.校验参数：登录态才可上传
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2.判断是新增还是删除，若是更新判断是否存在图片
        Long picId = null;
        if(pictureUploadRequest != null){
            picId = pictureUploadRequest.getId();
        }
        if(picId != null){
            boolean exists = this.lambdaQuery().eq(Picture::getId, picId).exists();
            ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR, "图片不存在");        }

        // 3. 上传图片，得到信息，构造入库图片信息 根据用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // picId为null则表示新增，否则为更新(更新需要补充id和编辑时间)
        if(picId!=null){
            picture.setId(picId);
            picture.setEditTime(new Date());
        }
        // 4. 操作数据库，入库
        boolean b = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR, "图片上传失败,数据库操作失败");
        // 5. 返回图片VO
        return PictureVO.objToVo(picture);
    }

    /**
     * 转封装类
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = picture.getUserId();
        if(userId!=null && userId>0){
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 转换分页结果为封装分页结果
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if(CollUtil.isEmpty(pictureList))
            return pictureVOPage;
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 填充信息
        pictureVOList.forEach(
                pictureVO -> {
                    Long userId = pictureVO.getUserId();
                    User user = null;
                    if(userIdUserListMap.containsKey(userId)){
                        user = userIdUserListMap.get(userId).get(0);
                    }
                    pictureVO.setUser(userService.getUserVO(user));
                }
        );
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 获取queryWrapper
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 多字段中搜索
        if(StrUtil.isNotBlank(searchText)){
            queryWrapper.and(
                    qw -> qw.like("name", searchText).or().like("introduction", introduction)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id),"id",id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId),"userId", userId);
        queryWrapper.like(StrUtil.isNotEmpty(name),"name",name);
        queryWrapper.like(StrUtil.isNotEmpty(introduction),"introduction",introduction);
        queryWrapper.like(StrUtil.isNotEmpty(category),"category",category);
        queryWrapper.like(StrUtil.isNotEmpty(picFormat),"picFormat",picFormat);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        if(CollUtil.isNotEmpty(tags)){
            for(String tag : tags){
                queryWrapper.like("tags", "\""+tag+"\"");
            }
        }
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField); // 排序
        return queryWrapper;
    }
}

