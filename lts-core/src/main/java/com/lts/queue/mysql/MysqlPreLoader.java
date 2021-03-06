package com.lts.queue.mysql;

import com.lts.core.AppContext;
import com.lts.core.cluster.Config;
import com.lts.core.logger.Logger;
import com.lts.core.logger.LoggerFactory;
import com.lts.core.support.JobQueueUtils;
import com.lts.core.support.SystemClock;
import com.lts.queue.AbstractPreLoader;
import com.lts.queue.domain.JobPo;
import com.lts.queue.mysql.support.RshHolder;
import com.lts.store.jdbc.SqlTemplate;
import com.lts.store.jdbc.SqlTemplateFactory;
import com.lts.store.jdbc.builder.OrderByType;
import com.lts.store.jdbc.builder.SelectSql;
import com.lts.store.jdbc.builder.UpdateSql;

import java.util.List;

/**
 * @author Robert HG (254963746@qq.com) on 8/14/15.
 */
public class MysqlPreLoader extends AbstractPreLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlPreLoader.class);
    private SqlTemplate sqlTemplate;

    public MysqlPreLoader(AppContext appContext) {
        super(appContext);
        this.sqlTemplate = SqlTemplateFactory.create(appContext.getConfig());
    }

    @Override
    protected boolean lockJob(String taskTrackerNodeGroup, String jobId,
                              String taskTrackerIdentity,
                              Long triggerTime,
                              Long gmtModified) {
        try {
            return new UpdateSql(sqlTemplate)
                    .update()
                    .table(getTableName(taskTrackerNodeGroup))
                    .set("is_running", true)
                    .set("task_tracker_identity", taskTrackerIdentity)
                    .set("gmt_modified", SystemClock.now())
                    .where("job_id = ?", jobId)
                    .and("is_running = ?", false)
                    .and("trigger_time = ?", triggerTime)
                    .and("gmt_modified = ?", gmtModified)
                    .doUpdate() == 1;
        } catch (Exception e) {
            LOGGER.error("Error when lock job:" + e.getMessage(), e);
            return false;
        }
    }


    @Override
    protected List<JobPo> load(String loadTaskTrackerNodeGroup, int loadSize) {
        try {
            return new SelectSql(sqlTemplate)
                    .select()
                    .all()
                    .from()
                    .table(getTableName(loadTaskTrackerNodeGroup))
                    .where("is_running = ?", false)
                    .and("trigger_time< ?", SystemClock.now())
                    .orderBy()
                    .column("trigger_time", OrderByType.ASC)
                    .column("priority", OrderByType.ASC)
                    .column("gmt_created", OrderByType.ASC)
                    .limit(0, loadSize)
                    .list(RshHolder.JOB_PO_LIST_RSH);
        } catch (Exception e) {
            LOGGER.error("Error when load job:" + e.getMessage(), e);
            return null;
        }
    }

    private String getTableName(String taskTrackerNodeGroup) {
        return JobQueueUtils.getExecutableQueueName(taskTrackerNodeGroup);
    }
}
