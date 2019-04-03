# NettyWebCaht

webChat-使用netty实现了webSocek和文件传输（新版webSocket）

本项目（springboot）使用netty实现了webSocket聊天功能，后台通过配置redis以及自定义的通信报文，实现文本消息、表情、图片、文件的实时发送、离线消息提醒、未读消息标识、查看历史记录等等
，图片和文件的发送采用的是netty的文件传输，将图片和文件保存到文件服务器，通过nginx静态代理实现服务与图片文件分离

由于没有放配置文件，所以需要添加配置文件进行使用
