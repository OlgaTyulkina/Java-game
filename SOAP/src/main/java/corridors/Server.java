package corridors;

import javax.xml.ws.Endpoint;

public class Server
{
    public static String url = "http://localhost:8080/Server";

    public static void main(String[] args) throws InterruptedException{
        RemoteGameServer service = new RemoteGameServer();
        Endpoint.publish(url, service);     // publish our webservice
        Thread.sleep(Integer.MAX_VALUE);
    }
}
