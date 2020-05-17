package com.hcs.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 心跳监测handler
 */
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        //判断是否是IdleStateEvent 用于触发用户事件（读 写空闲）
        if (evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;

            if(event.state() == IdleState.READER_IDLE){
                System.out.println("READER_IDLE");
            }else if ((event.state() == IdleState.WRITER_IDLE)){
                System.out.println("WRITER_IDLE");

            }else if ((event.state() == IdleState.ALL_IDLE)){
                Channel channel = ctx.channel();
                channel.close();
            }


        }
    }


}
