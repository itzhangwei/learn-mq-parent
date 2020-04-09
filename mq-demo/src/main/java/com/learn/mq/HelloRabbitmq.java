package com.learn.mq;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * @author zhang
 * @projectName learn-mq-parent
 * @title HelloRabbitmq
 * @package com.learn.mq
 * @description
 * @date 2020/4/9 10:45 上午
 */
public class HelloRabbitmq {
	public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
		sendMessage();
//		getMessage();
	}
	
	private static void sendMessage() throws IOException, TimeoutException {
		// mq连接地址
		String host = "106.13.162.44";
		//定义一个rabbitmq 的队列名称
		String queueName = "warehouse_schedule_delivery";
		//connectionFactory 实现socket网络连接
		ConnectionFactory connectionFactory = new ConnectionFactory();
		//设置rabbitmq连接地址
		connectionFactory.setHost(host);
		connectionFactory.setUsername("admin");
		connectionFactory.setPassword("admin-rabbitmq");
		// try 后面的代码块中的代码如果出现异常，会自动关闭try后面括号里面的资源代码
		try(
				//创建连接，并且开通channel通道
				Connection connection = connectionFactory.newConnection();
				final Channel channel = connection.createChannel()
		) {
			/**
			 * 定义一个rabbitmq的队列queue，如果不存在就创建,
			 * @param queue the name of the queue
			 * @param durable true if we are declaring a durable queue (the queue will survive a server restart)
			 *                持久化队列，可以在mq重启持久化队列，却无法持久化队列中的消息
			 * @param exclusive true if we are declaring an exclusive queue (restricted to this connection)
			 *
			 * @param autoDelete true if we are declaring an autodelete queue (server will delete it when no longer in use)
			 * @param arguments other properties (construction arguments) for the queue
			 */
			channel.queueDeclare(queueName, true, false, false, null);
			
			//定义向mq发送的消息
			String message = "订单消息5";
			
			// 推送一条消息到mq的queueName队列里面去
			//MessageProperties.PERSISTENT_TEXT_PLAIN 将推送的消息开启消息持久化，重启之mq服务后也能看到队列中的消息
			//没有指定持久化消息会在mq服务重启后丢失
			channel.basicPublish("",queueName, MessageProperties.PERSISTENT_TEXT_PLAIN,message.getBytes(StandardCharsets.UTF_8));
			System.out.println("定点服务发送消息，message=" + message);
		}
	}
	
	private static void getMessage() throws IOException, TimeoutException, InterruptedException {
		// mq连接地址
		String host = "106.13.162.44";
		//定义一个rabbitmq 的队列名称
		String queueName = "warehouse_schedule_delivery";
		//connectionFactory 实现socket网络连接
		ConnectionFactory connectionFactory = new ConnectionFactory();
		//设置rabbitmq连接地址
		connectionFactory.setHost(host);
		connectionFactory.setUsername("admin");
		connectionFactory.setPassword("admin-rabbitmq");
		// try 后面的代码块中的代码如果出现异常，会自动关闭try后面括号里面的资源代码
		try(
				//创建连接，并且开通channel通道
				Connection connection = connectionFactory.newConnection();
				final Channel channel = connection.createChannel()
		) {
			/**
			 * 定义一个rabbitmq的队列queue，如果不存在就创建，避免消费的时候出现问题
			 */
			channel.queueDeclare(queueName, false, false, false, null);
			/**
			 * 接受到消息后的接口回调函数，接收到消息就执行方法体
			 */
			DeliverCallback deliverCallback = (consumerTag, delivery)->{
//				int i = 1 / 0;
				//获取到消息
				final String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
				System.out.println("仓库服务接收到消息，准备执行调度发货流程，message=" + message);
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("仓库完成调度，现在要手动进行ack");
				// 手动对rabbitmq中的数据进行ack操作，把mq中的消息标记为删除
				
				channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
			};
			/**
			 *  queueName： 监听的消息队列名称，
			 *  true：如果有消息就消费出来，只要消息被投递到服务里面了，立马删除mq中的消息，传递false就是关闭了autoAck行为，则需要手动ack
			 *  deliverCallback：消费出来的消息交给deliverCallback的回调函数处理
			 */
			channel.basicConsume(queueName,false,deliverCallback,consumerTag->{});
			Thread.sleep(5000);
		}
	}
}
