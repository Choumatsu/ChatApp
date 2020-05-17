package com.hcs.service.impl;

import com.hcs.enums.MsgActionEnum;
import com.hcs.enums.MsgSignFlagEnum;
import com.hcs.enums.SearchFriendsStatusEnum;
import com.hcs.mapper.*;
import com.hcs.netty.ChatMsg;
import com.hcs.netty.DataContent;
import com.hcs.netty.UserChannelRel;
import com.hcs.pojo.FriendsRequest;
import com.hcs.pojo.MyFriends;
import com.hcs.pojo.Users;
import com.hcs.pojo.vo.FriendRequestVO;
import com.hcs.pojo.vo.MyFriendsVO;
import com.hcs.service.UserService;
import com.hcs.utils.FastDFSClient;
import com.hcs.utils.FileUtils;
import com.hcs.utils.JsonUtils;
import com.hcs.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.util.Sqls;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private UsersMapperCustom usersMapperCustom;

    @Autowired
    private Sid sid;

    @Autowired
    private QRCodeUtils qrCodeUtils;

    @Autowired
    private FastDFSClient fastDFSClient;

    @Autowired
    private MyFriendsMapper myFriendsMapper;

    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Autowired
    private ChatMsgMapper chatMsgMapper;


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUsernameIsExist(String username) {

        Users user = new Users();
        user.setUsername(username);

        Users result = usersMapper.selectOne(user);


        return result != null;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserForLogin(String username, String password) {

        Example userExample = new Example(Users.class);
        Example.Criteria criteria = userExample.createCriteria();

        criteria.andEqualTo("username", username);
        criteria.andEqualTo("password", password);

        Users result = usersMapper.selectOneByExample(userExample);


        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users saveUser(Users user) {
        String userId = sid.nextShort();

        //为用户生成唯一的二维码
        String qrCodePath = "F:\\课程\\test\\user\\" + userId + "qrCode.png";
        //格式nisechat_qrcode:[username]
        qrCodeUtils.createQRCode(qrCodePath, "nisechat_qrcode:" + user.getId());
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);
        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        user.setQrcode(qrCodeUrl);

        user.setId(userId);

        usersMapper.insert(user);
        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUserInfo(Users user) {
        usersMapper.updateByPrimaryKeySelective(user);
        return queryUserById(user.getId());

    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Users queryUserById(String userId) {
        return usersMapper.selectByPrimaryKey(userId);
    }


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchUser(String myUserId, String friendUsername) {
        Users user = queryUserInfoByUsername(friendUsername);

        if (user == null) {

            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }

        if (user.getId().equals(myUserId)) {
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }

        Example mfe = new Example(MyFriends.class);
        Example.Criteria mfc = mfe.createCriteria();
        mfc.andEqualTo("myUserId", myUserId);
        mfc.andEqualTo("myFriendUserId", user.getId());

        MyFriends myFriends = myFriendsMapper.selectOneByExample(mfe);
        if (myFriends != null) {
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }

        return SearchFriendsStatusEnum.SUCCESS.status;
    }


    @Transactional(propagation = Propagation.SUPPORTS)
    public Users queryUserInfoByUsername(String username) {
        Example ue = new Example(Users.class);
        Example.Criteria uc = ue.createCriteria();

        uc.andEqualTo("username", username);
        return usersMapper.selectOneByExample(ue);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {

        Users friend = queryUserInfoByUsername(friendUsername);


        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();

        frc.andEqualTo("sendUserId", myUserId);
        frc.andEqualTo("acceptUserId", friend.getId());
        FriendsRequest friendsRequest = friendsRequestMapper.selectOneByExample(fre);

        if (friendsRequest == null) {//不是好友且记录未添加
            String requestId = sid.nextShort();
            FriendsRequest request = new FriendsRequest();

            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());
            friendsRequestMapper.insert(request);


        }


    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String acceptUserId, String sendUserId) {

        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();

        frc.andEqualTo("sendUserId", sendUserId);
        frc.andEqualTo("acceptUserId", acceptUserId);

        friendsRequestMapper.deleteByExample(fre);


    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String acceptUserId, String sendUserId) {
        saveFriends(acceptUserId, sendUserId);
        saveFriends(sendUserId, acceptUserId);
        deleteFriendRequest(acceptUserId, sendUserId);

        Channel senderChannel = UserChannelRel.get(sendUserId);
        if(senderChannel!=null){
            //使用ws主动推送消息到申请者，更新通讯录
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);

            senderChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContent)));
        }


    }


    /**
     * 保存好友
     *
     * @param acceptUserId
     * @param sendUserId
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveFriends(String acceptUserId, String sendUserId) {


        String newFriendId = sid.nextShort();
        MyFriends newFriend = new MyFriends();
        newFriend.setId(newFriendId);
        newFriend.setMyUserId(acceptUserId);
        newFriend.setMyFriendUserId(sendUserId);
        myFriendsMapper.insert(newFriend);

    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        List<MyFriendsVO> myFriends = usersMapperCustom.queryMyFriends(userId);


        return myFriends;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {

        com.hcs.pojo.ChatMsg msgBD = new com.hcs.pojo.ChatMsg();
        String msgId = sid.nextShort();
        msgBD.setId(msgId);
        msgBD.setAcceptUserId(chatMsg.getReceiverId());
        msgBD.setSendUserId(chatMsg.getSenderId());
        msgBD.setCreateTime(new Date());
        msgBD.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgBD.setMsg(chatMsg.getMsg());

        chatMsgMapper.insert(msgBD);


        return msgId;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
        usersMapperCustom.batchUpdateMsgSigned(msgIdList);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<com.hcs.pojo.ChatMsg> getUnreadMsgList(String acceptUserId) {

        Example chatExample = new Example(com.hcs.pojo.ChatMsg.class);
        Example.Criteria chatExampleCriteria = chatExample.createCriteria();
        chatExampleCriteria.andEqualTo("singFlag",0);
        chatExampleCriteria.andEqualTo("acceptUserId",acceptUserId);

        List<com.hcs.pojo.ChatMsg> msgList = chatMsgMapper.selectByExample(chatExample);


        return msgList;
    }
}
