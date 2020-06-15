package janusgraph;

import com.google.common.collect.ImmutableMap;
import driver.TestDriver;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraphTransaction;

import java.util.*;

public class JanusGraphDriver extends TestDriver {

    private JanusGraph graph = JanusGraphFactory.open("conf/janusgraph-cassandra-es-server.properties");

    @Override
    public JanusGraphTransaction startTransaction()  {
        return graph.newTransaction();
    }

    @Override
    public void commitTransaction(Object tt) {
        JanusGraphTransaction transaction = (JanusGraphTransaction) tt;
        transaction.commit();
    }

    @Override
    public void abortTransaction(Object tt) {
        JanusGraphTransaction transaction = (JanusGraphTransaction) tt;
        transaction.rollback();
    }

    @Override
    public void close()  {
        graph.close();
    }


    @Override
    public void nukeDatabase() {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        g.V().drop().iterate(); //drop all vertices
        commitTransaction(transaction);

        close();//restart connection
        graph = JanusGraphFactory.open("conf/janusgraph-cassandra-es-server.properties");

    }

    //****** G0 BLOCK ******//

    @Override
    public void g0Init() {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long[] version = {0L};
        Vertex person1 = g.addV("Person").next();
        person1.property("id",1L);
        person1.property("versionHistory", Arrays.copyOf(version,1));
        Vertex person2 = g.addV("Person").next();
        person2.property("id",2L);
        person2.property("versionHistory", Arrays.copyOf(version,1));
        Edge kEdge = person1.addEdge("Knows",person2);
        kEdge.property("versionHistory", Arrays.copyOf(version,1));
        commitTransaction(transaction);
    }

    @Override
    public Map<String, Object> g0(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long person1ID       = (long) parameters.get("person1Id");
        long person2ID       = (long) parameters.get("person2Id");
        long trans           = (int)  parameters.get("transactionId");
        long[] transactionID = {trans};
        if(g.V().has("id",person1ID).hasNext()) {
            Vertex person1 = g.V().hasLabel("Person").has("id",person1ID).next();
            long[] versionHistoryP1 = person1.value("versionHistory");
            person1.property("versionHistory", ArrayUtils.addAll(versionHistoryP1, transactionID));
            Vertex person2 = g.V().hasLabel("Person").has("id",person2ID).next();
            long[] versionHistoryP2 = person2.value("versionHistory");
            person2.property("versionHistory", ArrayUtils.addAll(versionHistoryP2, transactionID));
            Edge   kEdge   = g.V().hasLabel("Person").has("id",person1ID).outE("Knows").next();
            long[] versionHistoryK = kEdge.value("versionHistory");
            kEdge.property("versionHistory", ArrayUtils.addAll(versionHistoryK, transactionID));
            transaction.commit();
            return ImmutableMap.of();
        }
        else
            throw new IllegalStateException("G0 Person Missing from Graph");
    }

    @Override
    public Map<String, Object> g0check(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long person1ID       = (long) parameters.get("person1Id");
        long person2ID       = (long) parameters.get("person2Id");
        if(g.V().has("id",person1ID).hasNext()) {
            Vertex person1 = g.V().hasLabel("Person").has("id",person1ID).next();
            long[] versionHistoryP1 = person1.value("versionHistory");
            Vertex person2 = g.V().hasLabel("Person").has("id",person2ID).next();
            long[] versionHistoryP2 = person2.value("versionHistory");
            Edge   kEdge   = g.V().hasLabel("Person").has("id",person1ID).outE("Knows").next();
            long[] versionHistoryK = kEdge.value("versionHistory");
            transaction.commit();

            final List<Long> p1VersionHistory = new ArrayList<>();
            for (long v : versionHistoryP1) p1VersionHistory.add(v);

            final List<Long> p2VersionHistory = new ArrayList<>();
            for (long v : versionHistoryP2) p2VersionHistory.add(v);

            final List<Long> kVersionHistory = new ArrayList<>();
            for (long v : versionHistoryK) kVersionHistory.add(v);

            return ImmutableMap.of("p1VersionHistory", p1VersionHistory, "kVersionHistory", kVersionHistory, "p2VersionHistory", p2VersionHistory);
        }
        else
            throw new IllegalStateException("G0 Person Missing from Graph");
    }




    @Override
    public void g1aInit() {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        Vertex v = g.addV().next();
        v.property("id",1L);
        v.property("version",1L);
        commitTransaction(transaction);
    }

    @Override
    public Map<String, Object> g1a1(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long personID = (long) parameters.get("personId");
        long sleepTime = (long) parameters.get("sleepTime");
        if(g.V().has("id",personID).hasNext()) {
            Vertex person = g.V().has("id", personID).next();
            long currentVersion = person.value("version");
            sleep(sleepTime);
            person.property("version", currentVersion + 1L);
            sleep(sleepTime);
            abortTransaction(transaction);
            return ImmutableMap.of();
        }
        else
            throw new IllegalStateException("G1A1 Person Missing from Graph");
    }

    @Override
    public Map<String, Object> g1a2(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long personID = (long) parameters.get("personId");
        if(g.V().has("id",personID).hasNext()) {
            Vertex person = g.V().has("id", personID).next();
            long pVersion = person.value("version");
            return ImmutableMap.of("pVersion", (long) pVersion);
        }
        else
            throw new IllegalStateException("G1A2 Person Missing from Graph");
    }


    //****** G1B BLOCK ******//

    @Override
    public void g1bInit() {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        Vertex v = g.addV().next();
        v.property("id",1L);
        v.property("version",99L);
        commitTransaction(transaction);
    }

    @Override
    public Map<String, Object> g1b1(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long personID  = (long) parameters.get("personId");
        long sleepTime = (long) parameters.get("sleepTime");
        long even      = (long) parameters.get("even");
        long odd       = (long) parameters.get("odd");

        if(g.V().has("id",personID).hasNext()) {
            Vertex person = g.V().has("id", personID).next();
            person.property("version", even);
            sleep(sleepTime);
            person.property("version", odd);
            abortTransaction(transaction);
            return ImmutableMap.of();
        }
        else
            throw new IllegalStateException("G1B1 Person Missing from Graph");
    }

    @Override
    public Map<String, Object> g1b2(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long personID = (long) parameters.get("personId");
        if(g.V().has("id",personID).hasNext()) {
            Vertex person = g.V().has("id", personID).next();
            long pVersion = person.value("version");
            return ImmutableMap.of("pVersion", (long) pVersion);
        }
        else
            throw new IllegalStateException("G2B2 Person Missing from Graph");
    }

    //****** G1C BLOCK ******//
    @Override
    public void g1cInit() {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        Vertex v1 = g.addV().next();
        v1.property("id",1L);
        v1.property("version",0L);
        Vertex v2 = g.addV().next();
        v2.property("id",2L);
        v2.property("version",0L);
        commitTransaction(transaction);
    }


    @Override
    public Map<String, Object> g1c(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long person1ID      = (long) parameters.get("person1Id");
        long person2ID      = (long) parameters.get("person2Id");
        long transactionId  = (long) parameters.get("transactionId");

        if(g.V().has("id",person1ID).hasNext() &&
           g.V().has("id",person2ID).hasNext()) {
            Vertex person1 = g.V().has("id", person1ID).next();
            person1.property("version", transactionId);
            Vertex person2 = g.V().has("id", person2ID).next();
            long person2Version = person2.value("version");
            commitTransaction(transaction);
            return ImmutableMap.of("person2Version", person2Version);
        }
        else
            throw new IllegalStateException("G1C Person Missing from Graph");
    }


    //****** IMP BLOCK ******//

    @Override
    public void impInit() {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        Vertex v = g.addV().next();
        v.property("id",1L);
        v.property("version",1L);
        commitTransaction(transaction);
    }

    @Override
    public Map<String, Object> imp1(Map parameters) {

        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long personID = (long) parameters.get("personId");

        if(g.V().has("id",personID).hasNext()) {
            Vertex person = g.V().has("id", personID).next();
            long currentVersion = person.value("version");
            person.property("version", currentVersion + 1L);
            commitTransaction(transaction);
            return ImmutableMap.of();
        }
        else
            throw new IllegalStateException("IMP1 Person Missing from Graph");

    }

    @Override
    public Map<String, Object> imp2(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long personID  = (long) parameters.get("personId");
        long sleepTime = (long) parameters.get("sleepTime");
        long firstRead = (long) g.V().has("id",personID).next().value("version");
        sleep(sleepTime);
        long secondRead = g.V().has("id",personID).next().value("version");

        return ImmutableMap.of("firstRead", firstRead, "secondRead", secondRead);
    }

    //****** PMP BLOCK ******//
    @Override
    public void pmpInit() {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        Vertex person = g.addV("Person").next();
        person.property("id",1L);
        Vertex post = g.addV("Post").next();
        post.property("id",1L);
        commitTransaction(transaction);
    }

    @Override
    public Map<String, Object> pmp1(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long personID  = (long) parameters.get("personId");
        long postID    = (long) parameters.get("postId");

        if(g.V().hasLabel("Person").has("id",personID).hasNext() &&
           g.V().hasLabel("Post").has("id",postID).hasNext()) {

            Vertex person = g.V().hasLabel("Person").has("id",personID).next();
            Vertex post =  g.V().hasLabel("Post").has("id",postID).next();
            person.addEdge("Likes",post);
            commitTransaction(transaction);
            return ImmutableMap.of();
        }
        else
            throw new IllegalStateException("PMP1 Person or Post  Missing from Graph");
    }

    @Override
    public Map<String, Object> pmp2(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long postID       = (long) parameters.get("postId");
        long sleepTime    = (long) parameters.get("sleepTime");
        if(g.V().hasLabel("Post").has("id",postID).hasNext()){
            long firstRead = g.V().hasLabel("Post").has("id", postID).inE("Likes").count().next();
            sleep(sleepTime);
            long secondRead = g.V().hasLabel("Post").has("id", postID).inE("Likes").count().next();
            commitTransaction(transaction);
            return ImmutableMap.of("firstRead", firstRead, "secondRead", secondRead);
        }
        else
            throw new IllegalStateException("PMP2 Person or Post Missing from Graph");
    }

    //****** LU BLOCK ******//

    @Override
    public void luInit() {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        Vertex person = g.addV("Person").next();
        person.property("id",1L);
        person.property("numFriends",0L);
        commitTransaction(transaction);
    }

    @Override
    public Map<String, Object>  lu1(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long person1Id    = (long) parameters.get("person1Id");
        long person2Id    = (long) parameters.get("person2Id");
        if(g.V().hasLabel("Person").has("id",person1Id).hasNext()){
            Vertex firstPerson  = g.V().hasLabel("Person").has("id", person1Id).next();
            Vertex secondPerson = g.addV("Person").next();
            secondPerson.property("id",person2Id);
            firstPerson.addEdge("Knows",secondPerson);
            long currentFriendCount = firstPerson.value("numFriends");
            firstPerson.property("numFriends",currentFriendCount+1);
            commitTransaction(transaction);
            return ImmutableMap.of();

        }
        else
            throw new IllegalStateException("LU1 Person Missing from Graph");

    }

    @Override
    public Map<String, Object> lu2(Map parameters) {
        JanusGraphTransaction transaction = startTransaction();
        GraphTraversalSource g = transaction.traversal();
        long personId    = (long) parameters.get("personId");

        if(g.V().hasLabel("Person").has("id",personId).hasNext()){
            System.out.println(g.V().count().value());
            long numKnowsEdges = g.V().hasLabel("Person").has("id", personId).outE("Knows").count().next();
            long numFriends = g.V().hasLabel("Person").has("id", personId).next().value("numFriends");
            commitTransaction(transaction);
            return ImmutableMap.of("numKnowsEdges", (long) numKnowsEdges, "numFriendsProp", (long) numFriends);
        }
        else
            throw new IllegalStateException("LU2 Person Missing from Graph");

    }

    //****** OTV BLOCK ******//
    @Override
    public void otvInit() {

    }

    @Override
    public Map<String, Object> otv1(Map parameters) {
        return null;
    }

    @Override
    public Map<String, Object> otv2(Map parameters) {
        return null;
    }


    //****** FR BLOCK ******//
    @Override
    public void frInit() {

    }

    @Override
    public Map<String, Object> fr1(Map parameters) {
        return null;
    }

    @Override
    public Map<String, Object> fr2(Map parameters) {
        return null;
    }

    //****** WS BLOCK ******//
    @Override
    public void wsInit() {

    }

    @Override
    public Map<String, Object> ws2(Map parameters) {
        return null;
    }

    @Override
    public Map<String, Object> ws1(Map parameters) {
        return null;
    }



    @Override
    public Object runQuery(Object tt, String querySpecification, Object o) throws Exception {
        return null;
    }

}