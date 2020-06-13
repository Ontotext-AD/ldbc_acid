package neo4j;

import com.google.common.collect.ImmutableMap;
import driver.TestDriver;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;

import java.util.Map;

public class Neo4jTestDriver extends TestDriver<Transaction, Map<String, Object>, Result> {

    protected Driver driver;

    public Neo4jTestDriver() {
        driver = GraphDatabase.driver("bolt://localhost:" + 7777, AuthTokens.basic("neo4j", "neo4j"));
    }

    @Override
    public void close() throws Exception {
        driver.close();
    }

    @Override
    public Transaction startTransaction() {
        return driver.session().beginTransaction();
    }

    @Override
    public void commitTransaction(Transaction tt) {
        tt.commit();
    }

    @Override
    public void abortTransaction(Transaction tt) {
        tt.rollback();
    }

    @Override
    public Result runQuery(Transaction tt, String querySpecification, Map<String, Object> queryParameters) {
        return tt.run(querySpecification, queryParameters);
    }

    @Override
    public void nukeDatabase() {
        final Transaction tt = startTransaction();
        tt.run("MATCH (n) DETACH DELETE n");
        tt.commit();
    }

    // LU

    @Override
    public void luInit() {
        final Transaction tt = startTransaction();
        tt.run("CREATE (:Person {id: 1, numFriends: 0})");
        tt.commit();
    }

    @Override
    public Void lu1() {
        final Transaction tt = startTransaction();
        tt.run("MATCH (p1:Person {id: 1})\n" +
                "SET p1.numFriends = p1.numFriends + 1\n" +
                "RETURN p1.numFriends\n");
        tt.commit();
        return null;
    }

    @Override
    public long lu2() {
        final Transaction tt = startTransaction();
        final Result result = tt.run("MATCH (p:Person {id: 1})\n" +
                "OPTIONAL MATCH (p)-[k:KNOWS]->()\n" +
                "WITH p, count(k) AS numKnowsEdges\n" +
                "RETURN numKnowsEdges,\n" +
                "       p.numFriends AS numFriendsProp\n");
        final Record record = result.next();
        long numKnowsEdges = record.get("numKnowsEdges").asLong();
        long numFriends = record.get("numFriendsProp").asLong();
        return numFriends;
    }

    // IMP

    @Override
    public void impInit() {
        final Transaction tt = startTransaction();
        tt.run("CREATE (:Person {id: 1, version: 1})");
        tt.commit();
    }

    @Override
    public Map<String, Long> imp1(long personId) {
        final Transaction tt = startTransaction();
        tt.run("MATCH (p:Person {id: $personId}) SET p.version = p.version + 1 RETURN p",
                ImmutableMap.of("personId", personId));
        tt.commit();
        return ImmutableMap.of();
    }

    @Override
    public Map<String, Long> imp2(long personId) {
        final Transaction tt = startTransaction();

        final Result result1 = tt.run("MATCH (p:Person {id: $personId}) RETURN p.version AS firstRead", ImmutableMap.of("personId", personId));
        if (!result1.hasNext()) throw new IllegalStateException("IMP result1 empty");
        final long firstRead = result1.next().get("firstRead").asLong();

        sleep(250);

        final Result result2 = tt.run("MATCH (p:Person {id: $personId}) RETURN p.version AS secondRead", ImmutableMap.of("personId", personId));

        if (!result2.hasNext()) throw new IllegalStateException("IMP result2 empty");
        final long secondRead = result2.next().get("secondRead").asLong();

        return ImmutableMap.of("firstRead", firstRead, "secondRead", secondRead);
    }

    // PMP

    @Override
    public void pmpInit() {
        final Transaction tt = startTransaction();
        tt.run("CREATE (:PersonY {id: 1, version: 1})");
        tt.commit();

    }

    @Override
    public Map<String, Object> pmp1(long personId) {
        return ImmutableMap.of();
    }

    @Override
    public Map<String, Object> pmp2(long personId) {
        return ImmutableMap.of();
    }

}
