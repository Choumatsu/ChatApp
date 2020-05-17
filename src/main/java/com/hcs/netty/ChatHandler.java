package com.hcs.netty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hcs.SpringUtil;
import com.hcs.enums.MsgActionEnum;
import com.hcs.service.UserService;
import com.hcs.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @Description: 处理消息的handler
 * TextWebSocketFrame： 在netty中，是用于为websocket专门处理文本的对象，frame是消息的载体
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	// 用于记录和管理所有客户端的channle
	private static ChannelGroup users =
			new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) 
			throws Exception {

		//获取消息
		String content = msg.text();
		Channel currentChannel = ctx.channel();

		//System.out.println(content);
		DataContent dataContent = JsonUtils.jsonToPojo(content,DataContent.class);

		Integer action = dataContent.getAction();
		//判断消息类型，处理不同业务
		if(action == MsgActionEnum.CONNECT.type){
			System.out.println("初始化");
			//1.ws第一次open时，初始化channel，把channel和userId关联
			String senderId = dataContent.getChatMsg().getSenderId();
			UserChannelRel.put(senderId, currentChannel);


			// 测试
//			users.stream().map(c -> c.id().asLongText()).forEach(System.out::println);
//			UserChannelRel.output();
		}else if (action == MsgActionEnum.CHAT.type){
			//2.聊天消息，将消息保存到数据库 标记状态
			ChatMsg chatMsg = dataContent.getChatMsg();
			String msgText = chatMsg.getMsg();
			String receiverId = chatMsg.getReceiverId();
			String senderId = chatMsg.getSenderId();

			// 保存消息到数据库，且标记为 未签收
			UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
			String msgId = userService.saveMsg(chatMsg);

			chatMsg.setMsgId(msgId);

			DataContent dataContentMsg = new DataContent();
			dataContentMsg.setChatMsg(chatMsg);

			// 发送消息
			// 从全局用户Channel关系中获取接受方的channel
			Channel receiverChannel = UserChannelRel.get(receiverId);

			if (receiverChannel == null) {
				// TODO channel为空代表用户离线，推送消息
			} else {
				// 当receiverChannel不为空的时候，从ChannelGroup去查找对应的channel是否存在
				Channel findChannel = users.find(receiverChannel.id());
				if (findChannel != null) {
					// 用户在线
					receiverChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContentMsg)));
				} else {
					// 用户离线 TODO 推送消息
				}
			}


		}else if (action == MsgActionEnum.SIGNED.type){
			//3.签收消息，更改消息状态
			UserService userService = (UserService)SpringUtil.getBean("userServiceImpl");

			// 扩展字段在signed类型的消息中，代表需要去签收的消息id，逗号间隔
			String msgIdsStr = dataContent.getExtand();
			String msgIds[] = msgIdsStr.split(",");

			List<String> msgIdList = new ArrayList<>();
			for (String mid : msgIds) {
				if (StringUtils.isNotBlank(mid)) {
					msgIdList.add(mid);
				}
			}

			//System.out.println(msgIdList.toString());

			if (msgIdList != null && !msgIdList.isEmpty() && msgIdList.size() > 0) {
				// 批量签收
				userService.updateMsgSigned(msgIdList);
			}
		}else if (action == MsgActionEnum.KEEPALIVE.type){
			//4.心跳监测

		}





	}

	/**
	 * 当客户端连接服务端之后（打开连接）
	 * 获取客户端的channel，并且放到ChannelGroup中去进行管理
	 */
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		users.add(ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();

		ctx.channel().close();
		users.remove(ctx.channel());
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		// 当触发handlerRemoved，ChannelGroup会自动移除对应客户端的channel
		users.remove(ctx.channel());

	}

	
	
}
