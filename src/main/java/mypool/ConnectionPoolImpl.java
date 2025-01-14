package mypool;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.awaitility.*;

/**
 * Created by sudhirkumar on 4/10/17.
 */
 class ConnectionPoolImpl implements ConnectionPool {

    private static final int DEFAULT_POOL_SIZE = 5;

    private static BlockingQueue<MongoClient> blockingQueue ;

    public ConnectionPoolImpl(int size) {
        int MAX_SIZE = (size != 0) ? size : DEFAULT_POOL_SIZE;
        if(blockingQueue == null) {
            blockingQueue = new ArrayBlockingQueue<MongoClient>(MAX_SIZE);
        }

        for (int i = 0; i < MAX_SIZE; i++) {
            try {
                blockingQueue.put(createConnection());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (MongoClientException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized int getAvailableConnections() {
        return blockingQueue.size();
    }

    @Override
    public MongoClient getConnection() throws InterruptedException {
        System.out.println("size before getting connection" + blockingQueue.size());
        MongoClient mongoClient = null;
        if (getAvailableConnections() != 0) {
            mongoClient = blockingQueue.take();
        } else {
            System.out.println("All resources under utilization ,Release old one !!!");
        }

        System.out.println("size after getting connection" + getAvailableConnections());

        return mongoClient;
    }

    /** reuse released connection
     * */
    @Override
    public void releaseConnection(MongoClient client) throws InterruptedException{
        System.out.println("size before releasing connection" + getAvailableConnections());

        blockingQueue.put(client);
        System.out.println("size after releasing connection" + getAvailableConnections());
    }

    private MongoClient createConnection()
    {
        // this can be moved to dao package
        Config config = Config.getConfig();
        return new MongoClient(config.HOST);
        // ,new MongoClientOptions.Builder().connectTimeout(config.TIMEOUT).build());
    }

    @Override
    public void close() throws IOException {
        Iterator<MongoClient> iterator = blockingQueue.iterator();

        while (iterator.hasNext()) {
            MongoClient client = iterator.next();
            //System.out.println(client.hashCode());
            client.close();
        }

        System.out.println("Closeee");
    }
}

class Main {

    public static void main(String[] args) throws InterruptedException, IOException {
//        ConnectionPoolFactory factory = new ConnectionPoolFactory();
        ConnectionPool pool = new ConnectionPoolImpl(4);

        MongoClient client1 = null;
        MongoClient client2 = null;
        MongoClient client3 = null;
        MongoClient client4 = null;
        MongoClient client5 = null;


        client1 = pool.getConnection(); // code to interface always
        client2 = pool.getConnection();
//        Thread.sleep(5000L);
        Awaitility a = new Awaitility();
        a.await().atLeast(Duration.FIVE_SECONDS);

        client3 = pool.getConnection();
        client4 = pool.getConnection();
        System.out.println(pool.getAvailableConnections());
        //client5 = pool.getConnection();
        pool.releaseConnection(client1);
        pool.releaseConnection(client2);
        pool.releaseConnection(client3);
        pool.releaseConnection(client4);
        System.out.println(pool.getAvailableConnections());

        // pool.close();
/*

        Scanner s = new Scanner(System.in);
        int N1 = s.nextInt();
        int N2 = s.nextInt();
        int count = 1;

        int max = N1>N2 ? N1:N2;
        for(int i = 2; i< max;i++){
            if(N1%i ==0 && N2%i ==0)
            {

                count++;
            }

        }

        System.out.println(count);

               client1 = pool.getConnection();
        pool.close();



        } catch (Exception e) {
            System.out.println("Error occured >>> " + e);
        } finally {
            try {
                pool.close();
            }catch(IOException ix){
                System.out.println("Broken!!!");
            }

        }*/
    }
}