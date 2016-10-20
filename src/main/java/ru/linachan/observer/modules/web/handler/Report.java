package ru.linachan.observer.modules.web.handler;

import com.mongodb.client.MongoCollection;
import org.apache.velocity.VelocityContext;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import ru.linachan.common.GenericCore;
import ru.linachan.common.GenericMethod;
import ru.linachan.observer.ObserverWorker;
import ru.linachan.observer.modules.watch.data.build.BuildData;
import ru.linachan.observer.modules.web.WebUIPageHandler;

import java.util.ArrayList;
import java.util.List;

public class Report implements WebUIPageHandler {

    private MongoCollection<Document> builds;

    public Report() {
        builds = ((ObserverWorker) GenericCore.instance().worker()).db().collection("builds");
    }

    public static class ReportGroup {

        private Integer changeId;
        private Integer patchSet;
        private String changeTitle;

        private Long lastReport;

        private List<BuildData> reports = new ArrayList<>();

        public ReportGroup(Document group) {
            changeId = ((Document) group.get("_id")).getInteger("changeId");
            patchSet = ((Document) group.get("_id")).getInteger("patchSet");
            changeTitle = ((Document) group.get("_id")).getString("changeTitle");

            lastReport = group.getLong("lastReport");
        }

        public void addReport(BuildData report) {
            reports.add(report);
        }

        public String id() {
            if (changeTitle != null) {
                return String.format("%s_%s", changeId, patchSet);
            }

            return "null";
        }

        public Integer changeId() {
            return changeId;
        }

        public Integer patchSet() {
            return patchSet;
        }

        public String changeTitle() {
            return changeTitle;
        }

        public Long lastReport() {
            return lastReport;
        }

        public boolean ok() {
            for (BuildData report: reports) {
                if (report.comment().length() == 0) {
                    return false;
                }
            }

            return true;
        }

        public List<BuildData> reports() {
            return reports;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void prepareContext(VelocityContext ctx) {
        List<ReportGroup> reports = new ArrayList<>();

        List<Document> aggregator = new ArrayList<>();

        aggregator.add(new Document("$sort", new Document("timeStamp", -1)));
        aggregator.add(new Document("$limit", GenericCore.instance().config().getInt("web.report_limit", 30)));

        aggregator.add(new Document("$group", new Document()
            .append("_id", new Document("changeId", "$change.id").append("patchSet", "$change.patchSet").append("changeTitle", "$change.title"))
            .append("reports", new Document("$push", "$_id"))
            .append("lastReport", new Document("$max", "$timeStamp"))
        ));

        aggregator.add(new Document("$sort", new Document("lastReport", -1)));

        for (Document group: builds.aggregate(aggregator)) {
            ReportGroup reportGroup = new ReportGroup(group);
            for (ObjectId reportId: (List<ObjectId>) group.get("reports")) {
                BuildData report = BuildData.fromBSON(builds.find(new Document("_id", reportId)).first());
                reportGroup.addReport(report);
            }
            reports.add(reportGroup);
        }

        ctx.internalPut("REPORTS", reports);
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("comment")
    public void comment(JSONObject request, JSONObject response) {
        String comment = (String) request.get("comment");

        Document build = builds.find(new Document("_id", new ObjectId((String) request.get("id")))).first();

        if (builds.updateOne(build, new Document("$set", new Document("comment", comment))).getModifiedCount() > 0) {
            response.put("success", true);
            response.put("message", "Comment updated");
        } else {
            response.put("success", false);
            response.put("message", "Unable to update comment");
        }

        Document change = (Document) build.get("change");

        boolean groupOk = builds.count(
            new Document("change.id", change.getInteger("id"))
                .append("change.patchSet", change.getInteger("patchSet"))
                .append("comment", new Document("$regex", "^$"))
        ) == 0;

        response.put("ok", comment.length() > 0);
        response.put("id", request.get("id"));

        response.put("group_ok", groupOk);
        response.put("group", (change.getString("title") != null) ? String.format("%s_%s", change.getInteger("id"), change.getInteger("patchSet")) : "null");

        response.put("notify", true);
    }
}
