package com.nhb.dtu;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.nhb.dtu.config.ConfigBean;
import com.nhb.dtu.init.InitModbusGenericProMap;
import com.nhb.dtu.initializer.DtuServerInitializer;
import com.nhb.dtu.server.DtuServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan("com.nhb")
@Configuration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
public class CollectSoftApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(CollectSoftApplication.class, args);
		InitModbusGenericProMap.initModbusGenericMap();
		DtuServer dtuServer = context.getBean(DtuServer.class);
		dtuServer.start();
	}

	@Autowired
	private ConfigBean configBean;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Bean(name = "serverBootstrap")
	public ServerBootstrap bootstrap() {
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup(), workerGroup());
		b.channel(NioServerSocketChannel.class);
		b.handler(new LoggingHandler(LogLevel.INFO)).childHandler(dtuServerInitializer);
		Map<ChannelOption<?>, Object> tcpChannelOptions = tcpChannelOptions();
		Set<ChannelOption<?>> keySet = tcpChannelOptions.keySet();
		for (ChannelOption option : keySet) {
			b.option(option, tcpChannelOptions.get(option));
		}
		return b;
	}

	@Autowired
	@Qualifier("dtuServerInitializer")
	private DtuServerInitializer dtuServerInitializer;

	@SuppressWarnings("static-access")
	@Bean(name = "tcpChannelOptions")
	public Map<ChannelOption<?>, Object> tcpChannelOptions() {
		Map<ChannelOption<?>, Object> options = new HashMap<ChannelOption<?>, Object>();
		options.put(ChannelOption.SO_KEEPALIVE, configBean.keepalive);
		options.put(ChannelOption.SO_BACKLOG, configBean.backlog);
		return options;
	}

	@SuppressWarnings("static-access")
	@Bean(name = "bossGroup", destroyMethod = "shutdownGracefully")
	public NioEventLoopGroup bossGroup() {
		return new NioEventLoopGroup(configBean.bossCount);
	}

	@SuppressWarnings("static-access")
	@Bean(name = "workerGroup", destroyMethod = "shutdownGracefully")
	public NioEventLoopGroup workerGroup() {
		return new NioEventLoopGroup(configBean.workerCount);
	}

	@SuppressWarnings("static-access")
	@Bean(name = "serverPort")
	public int port() {
		return configBean.serverPort;
	}

}
