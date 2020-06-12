package janusgraph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;

public class JanusGraphTest {

    public static void main(String[] args){
        JanusGraph graph = JanusGraphFactory.open("conf/janusgraph-cassandra-es-server.properties");
        try {
            GraphTraversalSource g = graph.traversal();
            g.addV().property("id",1).property("version",1);
            g.tx().commit();

            System.out.println(g.V().next());
//.has("id",1)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
