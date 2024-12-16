package com.yupi.yupicturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.picture.PictureEditRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUpdateRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureTagCategory;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    /**
     * 上传图片接口
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片接口
     * @param deleteRequest
     * @param request
     * @return
     */
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        // 1.参数校验
        if(deleteRequest == null || deleteRequest.getId()<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.判断图片是否存在
        Long id = deleteRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture==null, ErrorCode.NOT_FOUND_ERROR);
        // 3.判断是否有删除权限：本人或管理员
        if(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 4.操作数据库-删除
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 修改图片接口-仅管理员可操作
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest){
        // 1.入参校验
        if(pictureUpdateRequest == null || pictureUpdateRequest.getId()<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.判断待修改图片是否存在
        Long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture==null, ErrorCode.NOT_FOUND_ERROR);
        // 3.DTO转为实体类，并校验图片相关参数
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));    // list转为string类型
        pictureService.validPicture(picture);
        // 4.操作数据库-更新
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 修改图片接口 - 用户可操作
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request){
        // 1.参数校验
        if(pictureEditRequest == null || pictureEditRequest.getId()<=0)
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        // 2.判断待修改图片是否存在
        Long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture==null, ErrorCode.NOT_FOUND_ERROR);
        // 3. dto类转实体类
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());
        pictureService.validPicture(picture);
        // 4.逻辑校验：本人或管理员可修改
        User loginUser = userService.getLoginUser(request);
        if(!loginUser.getId().equals(picture.getUserId()) && !userService.isAdmin(loginUser))
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        // 5.操作数据库-修改图片
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
    /**
     * 根据id查询图片 - 管理员权限
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(Long id, HttpServletRequest request){
        // 1.参数校验
        ThrowUtils.throwIf(id<=0 , ErrorCode.PARAMS_ERROR);
        // 2.操作数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture==null, ErrorCode.OPERATION_ERROR);
        // 3.返回结果
        return ResultUtils.success(picture);
    }

    /**
     * 根据id查询图片封装类VO
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id, HttpServletRequest request){
        // 1.参数校验
        ThrowUtils.throwIf(id<=0, ErrorCode.PARAMS_ERROR);
        // 2.查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture==null, ErrorCode.NOT_FOUND_ERROR);
        // 3.结果返回
        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 获取标签tag、分类category(写死的，固定的标签和分类)
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTageCatetory(){
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 分页查询图片-仅管理员权限操作
     * @param pictureQueryRequest
     * @return
     */
    @GetMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest){
        // 1.封装分页查询参数
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 2.操作数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页查询图片
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request){
        // 1.封装分页查询参数
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 2.限制爬虫
        ThrowUtils.throwIf(pageSize>20, ErrorCode.PARAMS_ERROR);
        // 2.操作数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }
}
