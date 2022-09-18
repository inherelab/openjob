package io.openjob.server.cluster.actor;

import io.openjob.common.actor.BaseActor;
import io.openjob.common.request.WorkerJobInstanceTaskLogRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author stelin <swoft@qq.com>
 * @since 1.0.0
 */
@Component
@Log4j2
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WorkerJobInstanceTaskLogActor extends BaseActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WorkerJobInstanceTaskLogRequest.class, this::handleLog)
                .build();
    }

    public void handleLog(WorkerJobInstanceTaskLogRequest logRequest) {

    }
}
