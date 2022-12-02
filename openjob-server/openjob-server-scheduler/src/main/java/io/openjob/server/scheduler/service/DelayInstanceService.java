package io.openjob.server.scheduler.service;

import io.openjob.common.request.WorkerDelayAddRequest;
import io.openjob.common.request.WorkerDelayPullItemRequest;
import io.openjob.common.request.WorkerDelayPullRequest;
import io.openjob.common.response.ServerDelayAddResponse;
import io.openjob.common.response.ServerDelayInstanceResponse;
import io.openjob.common.response.ServerDelayPullResponse;
import io.openjob.server.scheduler.dto.DelayInstanceAddRequestDTO;
import io.openjob.server.scheduler.dto.DelayInstanceAddResponseDTO;
import io.openjob.server.scheduler.util.RedisUtil;
import io.openjob.server.scheduler.dto.DelayDTO;
import io.openjob.server.scheduler.scheduler.DelayInstanceScheduler;
import io.openjob.server.scheduler.util.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author stelin <swoft@qq.com>
 * @since 1.0.0
 */
@Slf4j
@Service
public class DelayInstanceService {
    private final DelayInstanceScheduler delayInstanceScheduler;

    @Autowired
    public DelayInstanceService(DelayInstanceScheduler delayInstanceScheduler) {
        this.delayInstanceScheduler = delayInstanceScheduler;
    }

    /**
     * Pull instance.
     *
     * @param pullRequest pull request.
     * @return ServerDelayPullResponse
     */
    public ServerDelayPullResponse pullInstance(WorkerDelayPullRequest pullRequest) {
        List<ServerDelayInstanceResponse> responses = new ArrayList<>();

        // Pull delay instance.
        pullRequest.getPullItems().forEach(item -> responses.addAll(this.pullByTopic(item)));

        ServerDelayPullResponse delayPullResponse = new ServerDelayPullResponse();
        delayPullResponse.setDelayInstanceResponses(responses);
        return delayPullResponse;
    }

    /**
     * Add delay instance.
     *
     * @param addRequest add request.
     * @return ServerDelayAddResponse
     */
    public ServerDelayAddResponse addDelayInstance(WorkerDelayAddRequest addRequest) {
        ServerDelayAddResponse serverDelayAddResponse = new ServerDelayAddResponse(addRequest.getDeliveryId());
        DelayInstanceAddRequestDTO addRequestDTO = new DelayInstanceAddRequestDTO();
        addRequestDTO.setTaskId(addRequest.getTaskId());
        addRequestDTO.setTopic(addRequest.getTopic());
        addRequestDTO.setParams(addRequest.getParams());
        addRequestDTO.setExtra(addRequest.getExtra());
        addRequestDTO.setExecuteTime(addRequest.getExecuteTime());
        DelayInstanceAddResponseDTO addResponseDTO = this.delayInstanceScheduler.add(addRequestDTO);
        serverDelayAddResponse.setTaskId(addResponseDTO.getTaskId());
        return serverDelayAddResponse;
    }

    private List<ServerDelayInstanceResponse> pullByTopic(WorkerDelayPullItemRequest item) {
        List<ServerDelayInstanceResponse> responses = new ArrayList<>();

        // Pull from redis.
        String topicKey = CacheUtil.getTopicKey(item.getTopic());
        List<Object> delayList = RedisUtil.popAndRemoveFromList(topicKey, item.getSize());

        // Not empty
        delayList.forEach(d -> {
            if (d instanceof DelayDTO) {
                DelayDTO delayDTO = (DelayDTO) d;
                ServerDelayInstanceResponse delayInstanceResponse = new ServerDelayInstanceResponse();
                delayInstanceResponse.setDelayId(delayDTO.getDelayId());
                delayInstanceResponse.setDelayParams(delayDTO.getDelayParams());
                delayInstanceResponse.setDelayExtra(delayDTO.getDelayExtra());
                delayInstanceResponse.setTopic(delayDTO.getTopic());
                delayInstanceResponse.setProcessorInfo(delayDTO.getProcessorInfo());
                delayInstanceResponse.setFailRetryTimes(delayDTO.getFailRetryTimes());
                delayInstanceResponse.setFailRetryInterval(delayDTO.getFailRetryInterval());
                delayInstanceResponse.setExecuteTimeout(delayDTO.getExecuteTimeout());
                delayInstanceResponse.setBlockingSize(delayDTO.getBlockingSize());
                delayInstanceResponse.setConcurrency(delayDTO.getConcurrency());
                responses.add(delayInstanceResponse);
            }
        });
        return responses;
    }
}
