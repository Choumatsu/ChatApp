package com.hcs.service;

import com.hcs.netty.ChatMsg;
import com.hcs.pojo.Users;
import com.hcs.pojo.vo.FriendRequestVO;
import com.hcs.pojo.vo.MyFriendsVO;

import java.util.List;

public interface UserService {

    /**
     * 判断用户名是否存在
     * @param username
     * @return
     */
    public boolean queryUsernameIsExist(String username);


    /**
     * 查询用户是否存在
     * @param username
     * @param password
     * @return
     */
    public Users queryUserForLogin(String username,String password);

    /**
     * 保存注册用户
     * @param user
     * @return
     */
    public Users saveUser(Users user);

    /**
     * 修改用户记录
     * @return
     */
    public Users updateUserInfo(Users user);

    /**
     * 搜索用户的前置条件
     * @param myUserId
     * @param friendUsername
     * @return 返回对应的状态码
     */
    public Integer preconditionSearchUser(String myUserId,String friendUsername);

    /**
     * 根据用户名查询用户对象
     * @param username
     * @return
     */
    public Users queryUserInfoByUsername(String username);

    /**
     * 发送好友请求
     * @param myUserId
     * @param friendUsername
     */
    public void sendFriendRequest(String myUserId,String friendUsername);

    /**
     * 接收好友请求
     * @param acceptUserId
     * @return
     */
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    /**
     * 删除好友请求记录
     * @param acceptUserId
     * @param sendUserId
     */
    public void deleteFriendRequest(String acceptUserId,String sendUserId);

    /**
     * 通过好友请求记录
     * @param acceptUserId
     * @param sendUserId
     */
    public void passFriendRequest(String acceptUserId,String sendUserId);


    /**
     * 查询好友列表
     * @param userId
     * @return
     */
    public List<MyFriendsVO> queryMyFriends(String userId);


    /**
     * 保存聊天消息到数据库
     * @param chatMsg
     * @return
     */
    public String saveMsg(ChatMsg chatMsg);


    //批量签收消息
    public void updateMsgSigned(List<String> msgIdList);


    //查询未签收的消息列表
    public List<com.hcs.pojo.ChatMsg> getUnreadMsgList(String acceptUserId);
}
