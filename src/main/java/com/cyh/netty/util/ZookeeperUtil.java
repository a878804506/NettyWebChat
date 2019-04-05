package com.cyh.netty.util;

import com.alibaba.fastjson.JSON;
import com.cyh.netty.nettyWebChat.BaseWebSocketServerHandler;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.security.NoSuchAlgorithmException;
import java.util.*;

@Component
public class ZookeeperUtil {

    @Autowired
    private RedisDB redisDB;

    private static final String CONNECTSTR = "148.70.60.231:2181";

    private static final int SESSIONTIMEOUT = 60000;

    private static final int CONNECTIONTIMEOUT = 60000;

    private static final String PATH = "/waterUserForOnline";

    //拥有所有的权限
    private static final String AUTHINFO_ALL = "admin:cyh19930807@!";
    //只拥有读和写的权限
    private static final String AUTHINFO_READ_WRITE = "test:cyh19930807";

    // 自定义权限列表
    private static final List<ACL> acls = new ArrayList<ACL>();
    static {
        try {
            Id user1 = new Id("digest", DigestAuthenticationProvider.generateDigest(AUTHINFO_ALL));
            Id user2 = new Id("digest", DigestAuthenticationProvider.generateDigest(AUTHINFO_READ_WRITE));
            acls.add(new ACL(ZooDefs.Perms.ALL, user1));
            acls.add(new ACL(ZooDefs.Perms.READ, user2));
//            acls.add(new ACL(ZooDefs.Perms.READ | ZooDefs.Perms.WRITE , user2)); // 多个权限的给予方式，使用 | 位运算符
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    //CuratorFramework工厂，从工厂中创建zk对象
    private static CuratorFramework client = CuratorFrameworkFactory.builder()
        .connectString(CONNECTSTR)
        .authorization("digest", AUTHINFO_READ_WRITE.getBytes()) //使用用户名/密码进行连接
        .sessionTimeoutMs(SESSIONTIMEOUT)  //session 超时时间
        .connectionTimeoutMs(CONNECTIONTIMEOUT)  // 客户端连接服务器超时时间
        .namespace("cyh") //命名空间
        .retryPolicy(new ExponentialBackoffRetry(1000, 3))
        .build();

     /*  zookeeper的会话状态有以下几种：:(是跟客户端实例相关的)
     *  Disconneced     //连接失败
     *  SyncConnected	//连接成功
     * 	AuthFailed      //认证失败
     * 	Expired         //会话过期
     */
    // 连接zookeeper.一般不用手动调用，直接用zoo对象就好
    public static void zkConnect() throws Exception {
        client.start();
    }

    /**
     * 创建节点
     * createMode：是否为临时节点,可取值为：PERSISTENT(持久无序)PERSISTENT_SEQUENTIAL(持久有序) EPHEMERAL(临时无序) EPHEMERAL_SEQUENTIAL(临时有序)
     * inTransaction() 创建事务并开始
     *  and().commit()  事务提交
     */
    public static void zkCreate(byte[] data) {
        try {
            client.inTransaction().create().withMode(CreateMode.PERSISTENT).withACL(acls).forPath(PATH,data).and().commit();
        } catch (Exception e) {
            CommonUtil.print("节点创建失败!");
            e.printStackTrace();
        }
    }

    // 读取数据节点数据
    public static String zkGetData() {
        String result = "";
        try {
            byte [] data = client.getData().forPath(PATH);
            result = new String(data,"UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // 修改节点中的数据
    public static Boolean zkUpdate(byte[] data) {
        //判断节点是否存在
        if (!zkExists())
            return false;
        try {
            Stat stat = client.setData().forPath(PATH);
            client.inTransaction().setData().withVersion(stat.getVersion()).forPath(PATH, data).and().commit();
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // 删除节点
    public static void zkDelete() {
        try {
            client.inTransaction().delete().forPath(PATH).and().commit();//只能删除叶子节点
            //client.delete().deletingChildrenIfNeeded().forPath("/Russia");//删除一个节点,并递归删除其所有子节点
            //client.delete().withVersion(5).forPath("/America");//强制指定版本进行删除
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 判断节点是否存在
    public static Boolean zkExists(){
        try {
            Stat stat = client.checkExists().forPath(PATH);
            return stat != null ? true : false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 判断节点是否是持久化节点
     * @param path 路径
     * @return 2-节点不存在  | 1-是持久化 | 0-临时节点
     */
    public int isPersistentNode(String path) {
        try {
            Stat stat = client.checkExists().forPath(path);
            if (stat == null)
                return 2;
            if (stat.getEphemeralOwner() > 0)
                return 1;
            return 0;
        }
        catch (Exception e) {
            e.printStackTrace();
            return 2;
        }
    }

    //获取子节点集合
    public static List<String> zkGetChildren(String path){
        try {
            return client.getChildren().forPath(PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<String>();
    }

    // 关闭连接
    public static void zkClose() throws InterruptedException {
        client.close();
    }

    /*  下边列举了ZooKeeper中最常见的几个通知状态和事件类型
    KeeperState             EventType                        触发条件                                       说明

                            None（-1）              客户端与服务端成功建立连接
    SyncConnected（0）      NodeCreated（1）        Watcher监听的对应数据节点被创建
                            NodeDeleted（2）        Watcher监听的对应数据节点被删除                 此时客户端和服务器处于连接状态
                            NodeDataChanged（3）    Watcher监听的对应数据节点的数据内容发生变更
                            NodeChildChanged（4）   Wather监听的对应数据节点的子节点列表发生变更
    Disconnected（0）       None（-1）              客户端与ZooKeeper服务器断开连接                 此时客户端和服务器处于断开连接状态
    Expired（-112）         Node（-1）              会话超时                                        此时客户端会话失效，通常同时也会受到SessionExpiredException异常
    AuthFailed（4）         None（-1）              通常有两种情况，1：使用错误的schema进行权限检查 2：SASL权限检查失败/通常同时也会收到AuthFailedException异常
    */
    //注册监听器，用于监听PATH上的变化
    @SuppressWarnings("resource")
	public static void zkNodeCache(){
        try {
            NodeCache nodeCache = new NodeCache(client, PATH, false);
            NodeCacheListener ncl = new NodeCacheListener() {
                @Override
                public void nodeChanged() throws Exception {
                    ChildData childData = nodeCache.getCurrentData();
                    CommonUtil.print("ZNode节点状态改变, path={}"+ childData.getPath());
                    CommonUtil.print("ZNode节点数据改变, data={}"+ new String(childData.getData(), "Utf-8"));
                    CommonUtil.print("ZNode节点状态改变, stat={}"+ childData.getStat());
                }
            };
            nodeCache.getListenable().addListener(ncl);
            nodeCache.start(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //注册监听器，用于监听PATH节点下所有子节点的变化
    @SuppressWarnings("resource")
	public void zkPathChildrenCache(){
        try {
			PathChildrenCache cache = new PathChildrenCache(client, PATH, true);
            PathChildrenCacheListener pccl = new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client,PathChildrenCacheEvent event) {
                    try {
                        ChildData data = event.getData();
                        Jedis jedis = null;
                        switch (event.getType()) {
                            case CHILD_ADDED:
                                CommonUtil.print("有用户上线, path={}, data={}"+data.getPath()+ ",该节点数据为："+ new String(data.getData(), "UTF-8"));
                                try {
                                	jedis = redisDB.getJedis();
                                    jedis.select(redisDB.dbSelectedForSystem);
                    				synchronized (CommonUtil.contactsList) {
                    					//获取redis中最新的联系人列表
                    					List<Map<String,Object>> redis_contactsList = SerializeUtil.unserializeForList(jedis.get(redisDB.systemUsers.getBytes()));
                    					String userId = new String(data.getData(), "UTF-8");
                                        //把当前登陆用户的上线消息 推送给其他用户
                              			Map<String,Object> contactsIsOnline = new HashMap<>();
                              			contactsIsOnline.put("id", userId);
                              			contactsIsOnline.put("type", 1);
                              			for (String key : CommonUtil.pushCtxMap.keySet()) {
                              				if(!key.equals(userId)) {
                              					contactsIsOnline.put("data", CommonUtil.getOneToOneUnReadMessageCount( redis_contactsList, Integer.valueOf(key)));
                              					//这里使用的是单个推送
                              					BaseWebSocketServerHandler.push(CommonUtil.pushCtxMap.get(key),JSON.toJSONString(contactsIsOnline));
                              				}
                              			}
                              			//最新的联系人列表 赋值给本地 contactsList
                                        CommonUtil.contactsList = redis_contactsList;
                    				}
								} catch (Exception e) {
									e.printStackTrace();
									RedisDB.returnBrokenResource(jedis);
								}finally {
									RedisDB.returnResource(jedis);
								}
                                break;
                            case CHILD_UPDATED:
                                CommonUtil.print("子节点更新, path={}, data={}"+data.getPath()+ ",该节点数据为："+ new String(data.getData(), "UTF-8"));
                                break;
                            case CHILD_REMOVED:
                                CommonUtil.print("有用户下线，子节点删除, path={}, data={}"+data.getPath()+ ",该节点数据为："+new String(data.getData(), "UTF-8"));
                                try {
                                	jedis = redisDB.getJedis();
                                    jedis.select(redisDB.dbSelectedForSystem);
                    				synchronized (CommonUtil.contactsList) {
                    					//获取redis中最新的联系人列表
                    					List<Map<String,Object>> redis_contactsList = SerializeUtil.unserializeForList(jedis.get(redisDB.systemUsers.getBytes()));
                    					String userId = new String(data.getData(), "UTF-8");
                                        //把当前登陆用户的下线消息 推送给其他用户
                              			Map<String,Object> contactsIsOnline = new HashMap<>();
                              			contactsIsOnline.put("id", userId);
                              			contactsIsOnline.put("type", 5);
                              			for (String key : CommonUtil.pushCtxMap.keySet()) {
                              				if(!key.equals(userId)) {
                              					contactsIsOnline.put("data", CommonUtil.getOneToOneUnReadMessageCount( redis_contactsList, Integer.valueOf(key)));
                              					//这里使用的是单个推送
                              					BaseWebSocketServerHandler.push(CommonUtil.pushCtxMap.get(key),JSON.toJSONString(contactsIsOnline));
                              				}
                              			}
                              			//最新的联系人列表 赋值给本地 contactsList
                                        CommonUtil.contactsList = redis_contactsList;
                    				}
								} catch (Exception e) {
									e.printStackTrace();
									RedisDB.returnBrokenResource(jedis);
								}finally {
									RedisDB.returnResource(jedis);
								}
                                break;
                            default:
                                break;
                        }
                        //打印最新联系人列表
                        for (Map<String, Object> ttt : CommonUtil.contactsList) {
                            CommonUtil.print("name:"+ttt.get("name")+"-----nickName:"+ttt.get("nickName")+"-----isOnline:"+ttt.get("isOnline"));
    					}
                        } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            cache.getListenable().addListener(pccl);
            cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
