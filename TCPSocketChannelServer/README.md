# TCPServer Demo
## 思路如下：  
    * 假设收到本地的连接请求事件***ACCEPT***，根据请求的端口，从**NATManager**获取远程信息，建立连接远程，并分别为本地、远程Socket注册***READ***。  
    为了便于管理，新建**TwinsChannel**保存本地、远程Socket信息，并作为附件传递。  
    * 假设收到本地的可读事件***READ***，读取内容，往远程Socket写入； 
    * 假设收到远程的可读事件***READ***，读取内容，往本地Socket写入； 




