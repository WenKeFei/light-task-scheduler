package com.lts.admin.access.mysql;

import com.lts.admin.access.RshHandler;
import com.lts.admin.access.face.BackendTaskTrackerMAccess;
import com.lts.admin.request.MDataPaginationReq;
import com.lts.admin.web.vo.NodeInfo;
import com.lts.core.cluster.Config;
import com.lts.monitor.access.domain.TaskTrackerMDataPo;
import com.lts.monitor.access.mysql.MysqlTaskTrackerMAccess;
import com.lts.store.jdbc.builder.DeleteSql;
import com.lts.store.jdbc.builder.SelectSql;
import com.lts.store.jdbc.builder.WhereSql;

import java.util.List;

/**
 * @author Robert HG (254963746@qq.com) on 3/9/16.
 */
public class MysqlBackendTaskTrackerMAccess extends MysqlTaskTrackerMAccess implements BackendTaskTrackerMAccess {

    public MysqlBackendTaskTrackerMAccess(Config config) {
        super(config);
    }

    @Override
    public List<TaskTrackerMDataPo> querySum(MDataPaginationReq request) {

        return new SelectSql(getSqlTemplate())
                .select()
                .columns("timestamp",
                        "SUM(exe_success_num) AS exe_success_num",
                        "SUM(exe_failed_num) AS exe_failed_num",
                        "SUM(exe_later_num) AS exe_later_num",
                        "SUM(exe_exception_num) AS exe_exception_num",
                        "SUM(total_running_time) AS total_running_time")
                .from()
                .table(getTableName())
                .whereSql(buildWhereSql(request))
                .groupBy(" timestamp ASC ")
                .limit(request.getStart(), request.getLimit())
                .list(RshHandler.TASK_TRACKER_SUM_M_DATA_RSH);
    }

    @Override
    public void delete(MDataPaginationReq request) {

        new DeleteSql(getSqlTemplate())
                .delete()
                .from()
                .table(getTableName())
                .whereSql(buildWhereSql(request))
                .doDelete();
    }

    @Override
    public List<NodeInfo> getTaskTrackers() {
        return new SelectSql(getSqlTemplate())
                .select()
                .columns("DISTINCT identity", "node_group")
                .from()
                .table(getTableName())
                .list(RshHandler.NODE_INFO_LIST_RSH);
    }

    public WhereSql buildWhereSql(MDataPaginationReq request) {
        return new WhereSql()
                .andOnNotNull("id = ?", request.getId())
                .andOnNotEmpty("identity = ?", request.getIdentity())
                .andOnNotEmpty("node_group = ?", request.getNodeGroup())
                .andBetween("timestamp", request.getStartTime(), request.getEndTime());
    }
}
