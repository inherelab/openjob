package io.openjob.server.cluster.service;

import io.openjob.common.request.WorkerJobInstanceLogRequest;
import io.openjob.common.request.WorkerJobInstanceStatusRequest;
import io.openjob.server.repository.dao.JobInstanceLogDAO;
import io.openjob.server.repository.dao.JobInstanceTaskDAO;
import io.openjob.server.repository.entity.JobInstanceLog;
import io.openjob.server.repository.entity.JobInstanceTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author stelin <swoft@qq.com>
 * @since 1.0.0
 */
@Service
public class JobInstanceService {
    private final JobInstanceTaskDAO jobInstanceTaskDAO;
    private final JobInstanceLogDAO jobInstanceLogDAO;

    @Autowired
    public JobInstanceService(JobInstanceTaskDAO jobInstanceTaskDAO, JobInstanceLogDAO jobInstanceLogDAO) {
        this.jobInstanceTaskDAO = jobInstanceTaskDAO;
        this.jobInstanceLogDAO = jobInstanceLogDAO;
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleInstanceStatus(WorkerJobInstanceStatusRequest statusRequest) {
        // Update job instance status.

        // Save job instance task
        List<JobInstanceTask> taskList = new ArrayList<>();
        statusRequest.getTaskRequestList().forEach(t -> {
            JobInstanceTask jobInstanceTask = new JobInstanceTask();
            jobInstanceTask.setJobId(t.getJobId());
            jobInstanceTask.setJobInstanceId(t.getJobInstanceId());
            jobInstanceTask.setCircleId(t.getCircleId());
            jobInstanceTask.setTaskId(t.getTaskId());
            jobInstanceTask.setParentTaskId(t.getParentTaskId());
            jobInstanceTask.setTaskName(t.getTaskName());
            jobInstanceTask.setStatus(t.getStatus());
            jobInstanceTask.setResult(t.getResult());
            jobInstanceTask.setWorkerAddress(t.getWorkerAddress());
            jobInstanceTask.setCreateTime(t.getCreateTime());
            jobInstanceTask.setUpdateTime(t.getUpdateTime());
            taskList.add(jobInstanceTask);
        });
        this.jobInstanceTaskDAO.batchSave(taskList);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleInstanceLog(WorkerJobInstanceLogRequest logRequest) {
        // Update job instance status.

        JobInstanceLog jobInstanceLog = new JobInstanceLog();
        jobInstanceLog.setJobId(logRequest.getJobId());
        jobInstanceLog.setJobInstanceId(logRequest.getJobInstanceId());
        jobInstanceLog.setMessage(logRequest.getMessage());
        jobInstanceLog.setCreateTime(logRequest.getTime());
        jobInstanceLog.setUpdateTime(logRequest.getTime());
        this.jobInstanceLogDAO.save(jobInstanceLog);
    }
}
