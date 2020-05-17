package com.hcs.mapper;

import com.hcs.pojo.Users;
import com.hcs.pojo.vo.FriendRequestVO;
import com.hcs.pojo.vo.MyFriendsVO;
import com.hcs.utils.MyMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface UsersMapperCustom extends MyMapper<Users> {

    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    public List<MyFriendsVO> queryMyFriends(String userId);

    public void batchUpdateMsgSigned(List<String> msgIdList);
}
