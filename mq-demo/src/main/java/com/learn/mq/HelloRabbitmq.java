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
//		sendMessage();
		getMessage();
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
		/**
		 *  try 后面的代码块中的代码如果出现异常，会自动关闭try后面括号里面的资源代码,
		 *  出现异常之后channel已经关闭，可能会导致 channel is already closed 异常
		 */
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
			String message = "订单消息6";
			
			// 推送一条消息到mq的queueName队列里面去
			//MessageProperties.PERSISTENT_TEXT_PLAIN 将推送的消息开启消息持久化，重启之mq服务后也能看到队列中的消息
			//没有指定持久化消息会在mq服务重启后丢失
			channel.basicPublish("",queueName, MessageProperties.PERSISTENT_TEXT_PLAIN,message.getBytes(StandardCharsets.UTF_8));
			System.out.println("定点服务发送消息，message=" + message);
		}
	}
	
	private static void getMessage() throws TimeoutException, InterruptedException, IOException {
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
		//创建连接，并且开通channel通道
		try (Connection connection = connectionFactory.newConnection()) {
			try (final Channel channel = connection.createChannel()) {
				/**
				 * 定义一个rabbitmq的队列queue，如果不存在就创建，避免消费的时候出现问题
				 * durable：持久化，如果消息队列已经存在，这里需要和存在的消息队列是否持久化对应。
				 * 上面发消息创建的true持久化的，这里也要持久化
				 */
				channel.queueDeclare(queueName, true, false, false, null);
				/**
				 * 接受到消息后的接口回调函数，接收到消息就执行方法体
				 */
				DeliverCallback deliverCallback = (consumerTag, delivery) -> {
					try {
						System.out.println("line "+getLineNumber()+" 线程名称：" + Thread.currentThread().getName());
						//获取到消息
						final String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
						System.out.println("仓库服务接收到消息，准备执行调度发货流程，message=" + message);
//						if ("订单消息5".equals(message)) {
//							System.out.println("我要进行 basicNack 操作了");
//							channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
//						}
						
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("出现异常，我需要通知mq将消息发给别的消费服务！");
						/**
						 * 消费mq消息的时候出现了异常，这个时候要告诉mq把消息传递给别的服务。
						 * ，如果调用了 basicNack 通知mq不要ack，在此后在调用 basicAck 清除消息是无法清除的。
						 * 如果同时调用了 basicNack 和 basicAck 会出现异常 AlreadyClosedException
						 * requeue：是否重新回到队列中
						 */
						channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
					} finally {
						System.out.println("仓库完成调度，现在要手动进行ack");
						// 手动对rabbitmq中的数据进行ack操作，把mq中的消息标记为删除
						// true批量发送给mq
						channel.basicAck(delivery.getEnvelope().getDeliveryTag(), true);
					}
				};
				/**
				 *  queueName： 监听的消息队列名称，
				 *  true：如果有消息就消费出来，只要消息被投递到服务里面了，立马删除mq中的消息，传递false就是关闭了autoAck行为，则需要手动ack
				 *  deliverCallback：消费出来的消息交给deliverCallback的回调函数处理
				 */
				channel.basicConsume(queueName, false, deliverCallback, System.out::println);
				Thread.sleep(10000);
				System.out.println("line " + getLineNumber() + "线程名称：" + Thread.currentThread().getName());
			}
		}
		
	}
	
	/**
	 * 获取行号
	 * @return int 执行此方法的行号
	 */
	public static int getLineNumber() {
		int level = 1;
		StackTraceElement[] stacks = new Throwable().getStackTrace();
		return stacks[level].getLineNumber();
	}
}
