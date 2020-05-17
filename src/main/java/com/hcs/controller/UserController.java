package com.hcs.controller;

import com.hcs.enums.OperatorFriendRequestTypeEnum;
import com.hcs.enums.SearchFriendsStatusEnum;
import com.hcs.pojo.Users;
import com.hcs.pojo.bo.UsersBO;
import com.hcs.pojo.vo.MyFriendsVO;
import com.hcs.pojo.vo.UsersVO;
import com.hcs.service.UserService;
import com.hcs.utils.AppJSONResult;
import com.hcs.utils.FastDFSClient;
import com.hcs.utils.FileUtils;
import com.hcs.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("u")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;

    @PostMapping("/registOrLogin")
    public AppJSONResult registOrLogin(@RequestBody Users user) throws Exception {
        //判断用户名和密码不为空
        if (StringUtils.isBlank(user.getUsername())
                || StringUtils.isBlank(user.getPassword())) {
            return AppJSONResult.errorMsg("用户名或密码不能为空");
        }
        //判断用户名是否存在
        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());

        Users userRes = null;

        if (usernameIsExist) {
            //登录
            userRes = userService.queryUserForLogin(user.getUsername(), MD5Utils.getMD5Str(user.getPassword()));
            if (userRes == null) {
                return AppJSONResult.errorMsg("用户名或密码错误");
            }
        } else {
            //注册
            user.setNickname(user.getUsername());
            user.setFaceImage("");
            user.setFaceImageBig("");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
            userRes = userService.saveUser(user);
        }

        UsersVO usersVO = new UsersVO();

        BeanUtils.copyProperties(userRes, usersVO);

        return AppJSONResult.ok(usersVO);
    }


    @PostMapping("/uploadFaceBase64")
    public AppJSONResult uploadFaceBase64(@RequestBody UsersBO usersBO) throws Exception {
        //获取前端传来的Base64字符串转换为文件对象上传
        String base64Data = usersBO.getFaceData();

        String userFacePath = "F:\\课程\\test\\" + usersBO.getUserId() + "userface.png";

        FileUtils.base64ToFile(userFacePath, base64Data);

        //上传文件到fastdfs
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);

        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println(url);

        //获取缩略图的url
        String thump = "_80x80.";
        String arr[] = url.split("\\.");
        String thumpImgUrl = arr[0] + thump + arr[1];

        Users user = new Users();
        user.setId(usersBO.getUserId());
        user.setFaceImage(thumpImgUrl);
        user.setFaceImageBig(url);

        Users userRes = userService.updateUserInfo(user);

        UsersVO usersVO = new UsersVO();

        BeanUtils.copyProperties(userRes, usersVO);


        return AppJSONResult.ok(usersVO);
    }

    @PostMapping("/setNickname")
    public AppJSONResult setNickname(@RequestBody UsersBO userBO) {

        if (StringUtils.isBlank(userBO.getNickname())) {
            return AppJSONResult.errorMsg("昵称不能为空!");
        }
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setNickname(userBO.getNickname());

        Users userRes = userService.updateUserInfo(user);

        UsersVO usersVO = new UsersVO();

        BeanUtils.copyProperties(userRes, usersVO);


        return AppJSONResult.ok(usersVO);

    }

    //搜索好友 匹配查询
    @PostMapping("/searchUser")
    public AppJSONResult searchUser(String myUserId, String friendUsername) {

        //判断不为空
        if (StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)) {
            return AppJSONResult.errorMsg("");
        }

        /**
         * SUCCESS(0, "添加成功"),
         * USER_NOT_EXIST(1, "无此用户!"),
         * NOT_YOURSELF(2, "不能添加你自己!"),
         * ALREADY_FRIENDS(3, "该用户已经是你的好友!");
         */
        Integer status = userService.preconditionSearchUser(myUserId, friendUsername);

        if (status.equals(SearchFriendsStatusEnum.SUCCESS.status)) {
            Users user = userService.queryUserInfoByUsername(friendUsername);

            UsersVO usersVO = new UsersVO();

            BeanUtils.copyProperties(user, usersVO);

            return AppJSONResult.ok(usersVO);
        } else {
            String errMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return AppJSONResult.errorMsg(errMsg);
        }

    }

    //发送添加好友请求
    @PostMapping("/addFriendRequest")
    public AppJSONResult addFriendRequest(String myUserId, String friendUsername) {

        //判断不为空
        if (StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)) {
            return AppJSONResult.errorMsg("");
        }

        /**
         * SUCCESS(0, "添加成功"),
         * USER_NOT_EXIST(1, "无此用户!"),
         * NOT_YOURSELF(2, "不能添加你自己!"),
         * ALREADY_FRIENDS(3, "该用户已经是你的好友!");
         */
        Integer status = userService.preconditionSearchUser(myUserId, friendUsername);

        if (status.equals(SearchFriendsStatusEnum.SUCCESS.status)) {


            userService.sendFriendRequest(myUserId, friendUsername);

        } else {
            String errMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return AppJSONResult.errorMsg(errMsg);
        }

        return AppJSONResult.ok();

    }

    //查询收到的好友请求
    @PostMapping("/queryFriendRequest")
    public AppJSONResult queryFriendRequest(String userId) {
        //判断不为空
        if (StringUtils.isBlank(userId)) {
            return AppJSONResult.errorMsg("");
        }

        return  AppJSONResult.ok(userService.queryFriendRequestList(userId));

    }

    @PostMapping("/operFriendRequest")
    public AppJSONResult operFriendRequest(String acceptUserId,String sendUserId,Integer operType){

        //判断不为空
        if (StringUtils.isBlank(acceptUserId)
                || StringUtils.isBlank(sendUserId)
                ||operType==null) {
            return AppJSONResult.errorMsg("");
        }

        String type = OperatorFriendRequestTypeEnum.getMsgByType(operType);

        if(StringUtils.isBlank(type)){
            return AppJSONResult.errorMsg("");
        }

        if(operType == OperatorFriendRequestTypeEnum.IGNORE.type){
            //忽略，直接删除表记录
            userService.deleteFriendRequest(acceptUserId,sendUserId);

        }else if(operType == OperatorFriendRequestTypeEnum.PASS.type){
            //通过，增加记录，删除请求记录
            userService.passFriendRequest(acceptUserId,sendUserId);
        }

        List<MyFriendsVO> myFriends = userService.queryMyFriends(acceptUserId);

        return AppJSONResult.ok(myFriends);


    }

    //查询好友列表
    @PostMapping("/myFriends")
    public AppJSONResult myFriends(String userId){
        //判断不为空
        if (StringUtils.isBlank(userId)) {
            return AppJSONResult.errorMsg("");
        }

        List<MyFriendsVO> myFriends = userService.queryMyFriends(userId);

        return AppJSONResult.ok(myFriends);
    }

    /**
     * 获取未签收的消息列表
     * @param acceptUserId
     * @return
     */
    @PostMapping("/getUnreadMsgList")
    public AppJSONResult getUnreadMsgList(String acceptUserId){
        //判断不为空
        if (StringUtils.isBlank(acceptUserId)) {
            return AppJSONResult.errorMsg("");
        }

        List<com.hcs.pojo.ChatMsg> unreadMsgList = userService.getUnreadMsgList(acceptUserId);


        return AppJSONResult.ok(unreadMsgList);
    }


}
