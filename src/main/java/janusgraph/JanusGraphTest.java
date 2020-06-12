package janusgraph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.*;
import org.janusgraph.core.schema.VertexLabelMaker;
import org.apache.tinkerpop.gremlin.structure.T;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JanusGraphTest {

    public static void main(String[] args){
        int GraphSize = 1;
        int tx1Total  = 100;
        int tx2Total  = 10000;
        int SleepMax  = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            clearGraph();
            buildInitialGraph(GraphSize);
            Runnable tx1Task = () -> {
                for(int i = 1;i<=tx1Total;i++) {
                    JanusGraph graph =JanusGraphFactory.open("conf/janusgraph-cassandra-es-server.properties");
                    int chosenID = (int) (Math.random() * GraphSize)+1;
                    ItemCutTx1(graph,chosenID);
                    graph.close();
                }
            };
            Runnable tx2Task = () -> {
                for(int i = 1;i<=tx2Total;i++) {
                    JanusGraph graph =JanusGraphFactory.open("conf/janusgraph-cassandra-es-server.properties");
                    int chosenID = (int) (Math.random() * GraphSize)+1;
                    int chosenSleep = (int) (Math.random() * SleepMax)+1;
                    try {
                        ItemCutTx2(graph,chosenID,chosenSleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    graph.close();
                }
            };
            executor.execute(tx1Task);
            executor.execute(tx1Task);
            executor.execute(tx2Task);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void clearGraph() {
        JanusGraph graph =JanusGraphFactory.open("conf/janusgraph-cassandra-es-server.properties");
        JanusGraphTransaction transaction = graph.newTransaction();
        GraphTraversalSource g = transaction.traversal();
        g.V().drop().iterate();
        transaction.commit();
        graph.close();
    }

    public static void buildInitialGraph(int Count) {
        JanusGraph graph =JanusGraphFactory.open("conf/janusgraph-cassandra-es-server.properties");
        JanusGraphTransaction transaction = graph.newTransaction();
        GraphTraversalSource gWrite = transaction.traversal();
        for(int i = 1;i<=Count;i++){
            Vertex v = gWrite.addV().next();
            v.property("id",i);
            v.property("version",1);
        }
        transaction.commit();
        graph.close();
    }

    public static void ItemCutTx1(JanusGraph graph,int ID) {
        JanusGraphTransaction transaction = graph.newTransaction();
        GraphTraversalSource gWrite = transaction.traversal();
        Vertex currentVertex = gWrite.V().has("id",ID).next();
        int currentVersion = currentVertex.value("version");
        currentVertex.property("version",currentVersion+1);
        transaction.commit();
    }

    public static void ItemCutTx2(JanusGraph graph,int ID,int sleepTime) throws InterruptedException {
        JanusGraphTransaction transaction = graph.newTransaction();
        GraphTraversalSource gWrite = transaction.traversal();
        int firstRead = gWrite.V().has("id",ID).next().value("version");
        Thread.sleep(sleepTime);
        int secondRead = gWrite.V().has("id",ID).next().value("version");
        if(firstRead!=secondRead){
            System.out.println("Anomaly Discovered");
        }
        else{
            System.out.println(secondRead+" "+firstRead);
        }
        transaction.commit();
    }

}


//            GraphTraversalSource gRead = graph.traversal();
//            JanusGraphTransaction transaction = graph.newTransaction();
//            GraphTraversalSource gWrite = transaction.traversal();
//            gWrite.addV().property("id",1L).next().property("name","test2");
//            gWrite.V().has("name","test2").next().property("name","test3");
//            transaction.commit();